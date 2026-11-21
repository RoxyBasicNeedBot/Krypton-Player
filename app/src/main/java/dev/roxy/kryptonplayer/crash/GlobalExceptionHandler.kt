package dev.roxy.kryptonplayer.crash

import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

class GlobalExceptionHandler(
    private val context: Context,
    private val activity: Class<*>,
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val isCrashing = AtomicBoolean(false)

    override fun uncaughtException(t: Thread, e: Throwable) {
        // Prevent recursive crashes
        if (!isCrashing.compareAndSet(false, true)) {
            defaultHandler?.uncaughtException(t, e) ?: exitProcess(1)
            return
        }

        try {
            Log.e("GlobalExceptionHandler", "FATAL EXCEPTION on thread ${t.name}", e)

            // 1. Immediately persist crash details to disk & SharedPreferences
            CrashStorage.saveCrash(
                context = context,
                throwable = e,
                threadName = t.name,
                isAnr = false,
            )

            // 2. Launch CrashActivity in isolated :crash process
            val intent = Intent(context, activity).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("exception", e.stackTraceToString())
                putExtra("thread_name", t.name)
                putExtra("exception_type", e.javaClass.simpleName)
                putExtra("exception_full_type", e.javaClass.name)
                putExtra("exception_message", e.localizedMessage ?: e.message ?: "No error message provided")
                putExtra("exception_cause", e.cause?.let { "${it.javaClass.simpleName}: ${it.localizedMessage}" } ?: "None")
                putExtra("is_anr", false)
            }
            context.startActivity(intent)

            // 3. Give Android OS ActivityManager 300ms to dispatch the Intent to :crash process
            Thread.sleep(300)
        } catch (fatal: Throwable) {
            Log.e("GlobalExceptionHandler", "Failed to dispatch CrashActivity", fatal)
        } finally {
            // 4. Cleanly terminate the crashed process so the user immediately sees CrashActivity
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }
}
