package dev.anilbeesetti.nextplayer.crash

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

class ApplicationNotRespondingException(message: String) : Error(message)

class AnrWatchdog private constructor(
    private val context: Context,
    private val activityClass: Class<*>,
    private val timeoutMs: Long = 4000L,
) : Thread("KryptonPlayer-ANR-Watchdog") {

    companion object {
        private const val TAG = "ANRWatchdog"
        @Volatile
        private var instance: AnrWatchdog? = null

        fun start(context: Context, activityClass: Class<*>, timeoutMs: Long = 4000L) {
            synchronized(this) {
                if (instance == null) {
                    val watchdog = AnrWatchdog(context.applicationContext, activityClass, timeoutMs)
                    watchdog.isDaemon = true
                    watchdog.start()
                    instance = watchdog
                    Log.i(TAG, "ANR Watchdog started with timeout = ${timeoutMs}ms")
                }
            }
        }
    }

    @Volatile
    private var tick = 0L

    @Volatile
    private var reported = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val tickRunnable = Runnable { tick++ }

    override fun run() {
        while (!isInterrupted) {
            val lastTick = tick
            mainHandler.post(tickRunnable)

            try {
                sleep(timeoutMs)
            } catch (e: InterruptedException) {
                break
            }

            if (tick == lastTick) {
                // The Main thread failed to process the message within timeoutMs!
                if (!reported) {
                    reported = true
                    val mainThread = Looper.getMainLooper().thread
                    val mainStack = mainThread.stackTrace
                    val mainStackTraceFormatted = buildString {
                        appendLine("ANR: Main UI thread blocked for > ${timeoutMs}ms")
                        appendLine("Thread: ${mainThread.name} (Priority: ${mainThread.priority}, State: ${mainThread.state})")
                        appendLine()
                        appendLine("--- Main Thread Stack Trace ---")
                        for (element in mainStack) {
                            appendLine("    at $element")
                        }
                    }

                    // Collect other active threads
                    val allThreadsDump = buildString {
                        val allThreads = getAllStackTraces()
                        for ((thread, stack) in allThreads) {
                            if (thread.id == mainThread.id) continue
                            appendLine()
                            appendLine("--- Thread: ${thread.name} (id: ${thread.id}, state: ${thread.state}) ---")
                            for (elem in stack.take(15)) {
                                appendLine("    at $elem")
                            }
                        }
                    }

                    Log.e(TAG, "ANR DETECTED!\n$mainStackTraceFormatted")

                    val anrError = ApplicationNotRespondingException(
                        "Main UI thread was frozen for > ${timeoutMs}ms. Android was about to display 'Close app / Wait'."
                    )

                    // Persist ANR report to disk
                    CrashStorage.saveCrash(
                        context = context,
                        throwable = anrError,
                        threadName = "main",
                        isAnr = true,
                        extraInfo = "$mainStackTraceFormatted\n\n=== ALL THREADS ===\n$allThreadsDump",
                    )

                    // Launch CrashActivity in isolated :crash process
                    try {
                        val intent = Intent(context, activityClass).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra("exception", "$mainStackTraceFormatted\n\n=== ALL THREADS ===\n$allThreadsDump")
                            putExtra("thread_name", "main (UI Thread)")
                            putExtra("exception_type", "ApplicationNotResponding (ANR)")
                            putExtra("exception_full_type", ApplicationNotRespondingException::class.java.name)
                            putExtra("exception_message", "Main UI thread was frozen for > ${timeoutMs}ms ('Close app / Wait')")
                            putExtra("exception_cause", "Main Thread Blockage / UI Thread Starvation")
                            putExtra("is_anr", true)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to launch CrashActivity for ANR", e)
                    }
                }
            } else {
                reported = false
            }
        }
    }
}
