package dev.anilbeesetti.nextplayer.crash

import java.util.regex.Pattern

data class BytecodeFailureDetails(
    val isBytecodeIssue: Boolean = false,
    val failureType: String = "",
    val missingSymbol: String = "",
    val receiverType: String = "",
    val technicalCause: String = "",
    val suggestedKeepRule: String = "",
    val solutionSteps: List<String> = emptyList(),
)

data class ComposeFailureContext(
    val isComposeIssue: Boolean = false,
    val lifecyclePhase: String = "",
    val failingSubsystem: String = "",
    val probableScreen: String = "",
    val technicalExplanation: String = "",
    val contextAdvice: String = "",
)

data class CrashAnalysis(
    val category: String,
    val culpritClass: String,
    val culpritMethod: String,
    val culpritFile: String,
    val culpritLine: Int,
    val culpritOperation: String,
    val affectedSubsystem: String,
    val plainDiagnosis: String,
    val recommendation: String,
    val appFrames: List<String> = emptyList(),
    val isObfuscatedFrame: Boolean = false,
    val bytecodeDetails: BytecodeFailureDetails = BytecodeFailureDetails(),
    val composeContext: ComposeFailureContext = ComposeFailureContext(),
    val triggerAction: String = "",
    val failureOrigin: String = "",
    val reasonHindi: String = "",
    val fixHindi: String = "",
)

object CrashAnalyzer {

    // Regex to match stack trace frames: at class.method(File.kt:123)
    private val STACK_FRAME_REGEX = Pattern.compile(
        """at\s+([\w$.]+)\.([\w$<>]+)\(([^:)]+)(?::(\d+))?\)"""
    )

    // Regex for AbstractMethodError: abstract method "void foo.bar(baz)" on receiver java.lang.Class<qux>
    private val ABSTRACT_METHOD_REGEX = Pattern.compile(
        """abstract method "([^"]+)" on receiver (.+)"""
    )

    // Regex for NoSuchMethodError: No virtual method foo(Ljava/lang/String;)V in class Lbar;
    private val NO_SUCH_METHOD_REGEX = Pattern.compile(
        """No (?:virtual|direct|static|interface) method ([^(]+)\(([^)]*)\)(?:[A-Za-z0-9_/$;]+)? in class (.+)"""
    )

    fun analyze(
        throwable: Throwable?,
        stackTraceText: String,
        isAnr: Boolean,
        lastNavBreadcrumb: String? = null,
    ): CrashAnalysis {
        val fullStack = if (throwable != null) {
            "${throwable.stackTraceToString()}\n$stackTraceText"
        } else {
            stackTraceText
        }

        // Correlate last screen from argument, breadcrumbs dump, or CrashBreadcrumbs tracker
        val effectiveLastNav = lastNavBreadcrumb
            ?: extractLastNavFromText(fullStack)
            ?: CrashBreadcrumbs.lastNav()
            ?: "Unknown Screen"

        // 1. Detect deep bytecode / linkage / R8 issues
        val bytecodeDetails = detectBytecodeFailure(fullStack, throwable)

        // 2. Detect Jetpack Compose lifecycle failure context
        val composeContext = detectComposeContext(fullStack, effectiveLastNav)

        // 3. Detect category, operation, subsystem, and recommendation
        val (category, operation, defaultSubsystem, recommendation) = when {
            isAnr && (fullStack.contains("File.exists") || fullStack.contains("BlockGuardOs.access") || fullStack.contains("UnixFileSystem") || fullStack.contains("FileInputStream") || fullStack.contains("FileOutputStream") || fullStack.contains("listFiles")) -> {
                Quadruple(
                    "Main UI Thread Disk I/O (ANR)",
                    "Synchronous Disk / File System Access (File.exists / listFiles / I/O)",
                    "Local Storage & Downloads",
                    "Offload all File / Disk I/O operations to CoroutineScope(Dispatchers.IO) or use in-memory caches.",
                )
            }
            isAnr && (fullStack.contains("SocketInputStream") || fullStack.contains("SocketOutputStream") || fullStack.contains("ConscryptEngineSocket") || fullStack.contains("Okio") || fullStack.contains("HttpURLConnection") || fullStack.contains("OkHttpClient")) -> {
                Quadruple(
                    "Main UI Thread Network I/O (ANR)",
                    "Network Socket / TLS Handshake on Main UI Thread",
                    "Network & Streaming Client",
                    "Perform network queries on Dispatchers.IO. Never make HTTP or socket requests on the Main Looper.",
                )
            }
            isAnr && (fullStack.contains("LockSupport.park") || fullStack.contains("AbstractQueuedSynchronizer") || fullStack.contains("synchronized") || fullStack.contains("Mutex.lock") || fullStack.contains("runBlocking")) -> {
                Quadruple(
                    "Thread Lock / Deadlock Contention (ANR)",
                    "Main Thread waiting on Lock / Mutex held by background thread or runBlocking",
                    "Concurrency & Synchronization",
                    "Avoid calling runBlocking or holding synchronized locks / Mutexes across thread boundaries where the Main Thread awaits.",
                )
            }
            isAnr -> {
                Quadruple(
                    "UI Thread Starvation / Long Loop (ANR)",
                    "Main Looper blocked for > 4000ms",
                    "UI Rendering & Compose",
                    "Inspect the Main Thread stack trace to find long loops, heavy layout passes, or excessive Compose recompositions.",
                )
            }
            bytecodeDetails.isBytecodeIssue -> {
                Quadruple(
                    bytecodeDetails.failureType,
                    "Bytecode Linkage Failure: ${bytecodeDetails.missingSymbol} on ${bytecodeDetails.receiverType}",
                    if (composeContext.isComposeIssue) "Jetpack Compose Modifier.Node Tree" else "DEX Bytecode & R8 Minification",
                    bytecodeDetails.solutionSteps.firstOrNull() ?: "Add required -keep ProGuard rules to protect interface implementations.",
                )
            }
            fullStack.contains("OutOfMemoryError") || fullStack.contains("Failed to allocate") -> {
                Quadruple(
                    "Heap Memory Exhausted (OOM)",
                    "Bitmap decode or large payload exceeding available Java heap",
                    "Memory & Image Caching",
                    "Enable android:largeHeap, downscale bitmaps on decode, and ensure caches are bounded.",
                )
            }
            fullStack.contains("PlaybackException") || fullStack.contains("MediaCodec") || fullStack.contains("ExoPlayer") -> {
                Quadruple(
                    "Media Playback / Codec Exception",
                    "Audio/Video decoder failure or expired stream source",
                    "Media Player & Codecs",
                    "Check network stream URL validity and ensure hardware decoder handles video resolution.",
                )
            }
            fullStack.contains("NullPointerException") -> {
                Quadruple(
                    "Null Pointer Dereference (NPE)",
                    "Attempted to access property or method on null object reference",
                    "Core Logic",
                    "Add safe call (?.) or null checks before accessing object properties.",
                )
            }
            fullStack.contains("IndexOutOfBoundsException") || fullStack.contains("ArrayIndexOutOfBoundsException") -> {
                Quadruple(
                    "Index Out of Bounds",
                    "Attempted to access collection index outside valid range",
                    "Data Structures & Collections",
                    "Use safe collection access (e.g. .getOrNull(index)) or check size before access.",
                )
            }
            fullStack.contains("ConcurrentModificationException") -> {
                Quadruple(
                    "Concurrent Collection Mutation",
                    "Collection modified while being iterated across threads",
                    "Thread Safety",
                    "Use thread-safe collections like ConcurrentHashMap or create immutable snapshots (.toList()).",
                )
            }
            fullStack.contains("org.drinkless.tdlib") || fullStack.contains("TdlibException") -> {
                Quadruple(
                    "Telegram TDLib MTProto Failure",
                    "Native MTProto TDLib client error or session timeout",
                    "Telegram MTProto Engine",
                    "Verify TDLib initialization parameters, authentication token, or network connectivity.",
                )
            }
            fullStack.contains("FLAG_IMMUTABLE") || fullStack.contains("Targeting S+ (version 31 and above)") -> {
                Quadruple(
                    "PendingIntent Mutability Missing (Android 12+)",
                    "PendingIntent created without PendingIntent.FLAG_IMMUTABLE or FLAG_MUTABLE",
                    "Android Platform & Notifications",
                    "Add PendingIntent.FLAG_IMMUTABLE (or FLAG_MUTABLE if needed) to all PendingIntent.get* calls.",
                )
            }
            fullStack.contains("FOREGROUND_SERVICE_") -> {
                Quadruple(
                    "Foreground Service Type Missing (Android 14+)",
                    "Service started without declaring required foregroundServiceType in AndroidManifest.xml",
                    "Background Services & Notifications",
                    "Add android:foregroundServiceType='mediaPlayback' or 'dataSync' to the <service> tag in AndroidManifest.xml.",
                )
            }
            fullStack.contains("TransactionTooLargeException") -> {
                Quadruple(
                    "IPC Binder Buffer Overflow (TransactionTooLargeException)",
                    "Intent extras or Binder IPC payload exceeded 1MB transaction buffer limit",
                    "Android IPC & Navigation",
                    "Avoid passing large Bitmaps, Lists, or ByteArrays via Intent or SavedStateHandle; use memory caches or disk URIs instead.",
                )
            }
            fullStack.contains("Room cannot verify the data integrity") || fullStack.contains("SQLiteDatabaseCorruptException") -> {
                Quadruple(
                    "Database Migration / Schema Verification Failure",
                    "Room database schema changed without supplying an appropriate Migration path",
                    "Local Database & Room",
                    "Add an explicit Room Migration or call fallbackToDestructiveMigration() in Room.databaseBuilder.",
                )
            }
            fullStack.contains("BadTokenException") -> {
                Quadruple(
                    "Window Token Invalid (BadTokenException)",
                    "Attempted to show a Dialog or PopupWindow on a destroyed Activity or invalid Window token",
                    "UI Window Management",
                    "Ensure Activity is not finishing or destroyed (!activity.isFinishing && !activity.isDestroyed) before showing dialogs.",
                )
            }
            composeContext.isComposeIssue && (fullStack.contains("SubcomposeLayout") || fullStack.contains("subcompose")) -> {
                Quadruple(
                    "Compose SubcomposeLayout Error",
                    "Dynamic subcomposition measurement pass failed",
                    "Jetpack Compose UI Tree",
                    "Ensure subcompose slots are not measured with infinite height/width constraints.",
                )
            }
            fullStack.contains("IllegalStateException") -> {
                Quadruple(
                    "Illegal State Exception",
                    "Method invoked at an inappropriate time or in invalid lifecycle state",
                    "Lifecycle & State Machine",
                    "Ensure component or coroutine is in the expected state before executing this action.",
                )
            }
            else -> {
                val errorType = throwable?.javaClass?.simpleName ?: "Exception"
                Quadruple(
                    errorType,
                    throwable?.localizedMessage ?: "Unexpected Runtime Failure",
                    "General App Module",
                    "Examine the stack trace below to identify the failing statement.",
                )
            }
        }

        // 4. Find the culprit frame and app call stack
        var culpritClass = "UnknownClass"
        var culpritMethod = "unknownMethod"
        var culpritFile = "UnknownSource.kt"
        var culpritLine = -1
        val appFrames = mutableListOf<String>()

        val lines = fullStack.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (!trimmed.startsWith("at ")) continue

            val isAppFrame = trimmed.contains("dev.anilbeesetti.nextplayer")
            if (isAppFrame) {
                val matcher = STACK_FRAME_REGEX.matcher(trimmed)
                if (matcher.find()) {
                    val fullClass = matcher.group(1) ?: ""
                    val rawMethod = matcher.group(2) ?: ""
                    val file = matcher.group(3) ?: ""
                    val lineNum = matcher.group(4)?.toIntOrNull() ?: -1

                    val (resolvedCls, resolvedMethod) = resolveKotlinMethod(fullClass, rawMethod)

                    if (culpritClass == "UnknownClass") {
                        culpritClass = resolvedCls
                        culpritMethod = resolvedMethod
                        culpritFile = file
                        culpritLine = lineNum
                    }

                    val lineStr = if (lineNum > 0) ":$lineNum" else ""
                    appFrames.add("$file$lineStr ($resolvedCls.$resolvedMethod)")
                }
            }
            if (appFrames.size >= 8) break
        }

        // If no dev.anilbeesetti.nextplayer frame matched, inspect top stack frames
        if (culpritClass == "UnknownClass" && lines.isNotEmpty()) {
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("at ")) {
                    val matcher = STACK_FRAME_REGEX.matcher(trimmed)
                    if (matcher.find()) {
                        val fullClass = matcher.group(1) ?: ""
                        val rawMethod = matcher.group(2) ?: ""
                        val file = matcher.group(3) ?: ""
                        val lineNum = matcher.group(4)?.toIntOrNull() ?: -1
                        val (resolvedCls, resolvedMethod) = resolveKotlinMethod(fullClass, rawMethod)

                        culpritClass = resolvedCls
                        culpritMethod = resolvedMethod
                        culpritFile = file
                        culpritLine = lineNum
                        break
                    }
                }
            }
        }

        // Check if top frame is an R8 minified/obfuscated frame e.g. yz7.invoke(SourceFile:46)
        val isObfuscated = culpritFile == "SourceFile" || (culpritClass.length <= 4 && !culpritClass.contains("."))

        // Auto-classify subsystem based on culprit class, stack trace, and Compose context
        val subsystem = when {
            composeContext.isComposeIssue -> composeContext.failingSubsystem
            culpritClass.contains("Download", ignoreCase = true) || fullStack.contains("TelegramDownloadManager") ->
                "Telegram Downloads & Cache"
            culpritClass.contains("Tdlib", ignoreCase = true) || fullStack.contains("org.drinkless.tdlib") ->
                "Telegram TDLib MTProto Engine"
            culpritClass.contains("Player", ignoreCase = true) || culpritClass.contains("Media", ignoreCase = true) || fullStack.contains("androidx.media3") ->
                "Media Player & ExoPlayer"
            culpritClass.contains("Channel", ignoreCase = true) || culpritClass.contains("TelegramScreen", ignoreCase = true) || culpritClass.contains("TelegramLibrary", ignoreCase = true) ->
                "Telegram Channels & UI"
            culpritClass.contains("Navigation", ignoreCase = true) || culpritClass.contains("NavHost", ignoreCase = true) ->
                "Navigation & Tab Controller"
            culpritClass.contains("Network", ignoreCase = true) || culpritClass.contains("Retrofit", ignoreCase = true) ->
                "Network & HTTP Client"
            culpritClass.contains("Database", ignoreCase = true) || culpritClass.contains("Room", ignoreCase = true) || culpritClass.contains("Dao", ignoreCase = true) ->
                "Local Database & Room"
            bytecodeDetails.isBytecodeIssue -> "DEX Bytecode & R8 Minification"
            else -> defaultSubsystem
        }

        val plainDiagnosis = buildString {
            append("Detected: $category")
            if (culpritClass != "UnknownClass") {
                append(" in $culpritClass.$culpritMethod()")
                if (culpritLine > 0) {
                    append(" at line $culpritLine of $culpritFile")
                }
                if (isObfuscated) {
                    append(" [R8 Obfuscated Frame]")
                }
            }
            append(".\n")

            if (composeContext.isComposeIssue) {
                append("Compose Phase: ${composeContext.lifecyclePhase} on screen '${composeContext.probableScreen}'.\n")
            }

            append("Culprit Operation: $operation.\n")
            if (isAnr) {
                append("Main UI Thread was frozen for > 4000ms ('Close app / Wait').")
            } else {
                append("Fatal unhandled exception terminated the app process.")
            }
        }

        val triggerAction = extractTriggerAction(fullStack, effectiveLastNav)
        val failureOrigin = determineFailureOrigin(isAnr, bytecodeDetails, composeContext, isObfuscated, fullStack)
        val reasonHindi = generateHindiReason(
            triggerAction = triggerAction,
            failureOrigin = failureOrigin,
            category = category,
            culpritClass = culpritClass,
            culpritMethod = culpritMethod,
            culpritFile = culpritFile,
            culpritLine = culpritLine,
            bytecodeDetails = bytecodeDetails,
            composeContext = composeContext,
            isAnr = isAnr,
            fullStack = fullStack,
        )
        val fixHindi = generateHindiFix(
            failureOrigin = failureOrigin,
            bytecodeDetails = bytecodeDetails,
            composeContext = composeContext,
            recommendation = recommendation,
            isAnr = isAnr,
        )

        return CrashAnalysis(
            category = category,
            culpritClass = culpritClass,
            culpritMethod = culpritMethod,
            culpritFile = culpritFile,
            culpritLine = culpritLine,
            culpritOperation = operation,
            affectedSubsystem = subsystem,
            plainDiagnosis = plainDiagnosis,
            recommendation = recommendation,
            appFrames = appFrames,
            isObfuscatedFrame = isObfuscated,
            bytecodeDetails = bytecodeDetails,
            composeContext = composeContext,
            triggerAction = triggerAction,
            failureOrigin = failureOrigin,
            reasonHindi = reasonHindi,
            fixHindi = fixHindi,
        )
    }

    fun extractTriggerAction(fullStack: String, effectiveLastNav: String): String {
        // First check last recorded click from CrashBreadcrumbs
        val lastClick = CrashBreadcrumbs.lastClick()
        if (!lastClick.isNullOrBlank()) {
            return lastClick.removePrefix("Clicked: ").trim()
        }

        // Second check text for [CLICK] entries
        val lines = fullStack.lines()
        for (i in lines.indices.reversed()) {
            val line = lines[i].trim()
            if (line.contains("[CLICK]") && line.contains("Clicked: ")) {
                return line.substringAfter("Clicked: ").trim()
            }
        }

        // Correlate with current screen or dialog state
        return when {
            fullStack.contains("UrlDialog") || fullStack.contains("NetworkStream") ->
                "Link / Network Stream Icon (URL Input Dialog)"
            fullStack.contains("Telegram") || effectiveLastNav.contains("Telegram", ignoreCase = true) ->
                "Telegram Cloud Icon / Navigation"
            fullStack.contains("Search") || effectiveLastNav.contains("Search", ignoreCase = true) ->
                "Search Icon / Search Screen"
            fullStack.contains("Reel") || effectiveLastNav.contains("Reel", ignoreCase = true) ->
                "Reels FAB / Short Video Player"
            effectiveLastNav.isNotBlank() && effectiveLastNav != "Unknown Screen" ->
                "Navigation to $effectiveLastNav"
            else -> "System / Lifecycle Action"
        }
    }

    fun determineFailureOrigin(
        isAnr: Boolean,
        bytecodeDetails: BytecodeFailureDetails,
        composeContext: ComposeFailureContext,
        isObfuscated: Boolean,
        fullStack: String,
    ): String {
        return when {
            bytecodeDetails.isBytecodeIssue || isObfuscated || fullStack.contains("SourceFile") || fullStack.contains("AbstractMethodError") || fullStack.contains("NoSuchMethodError") ->
                "⚙️ ProGuard / R8 Minification (Stripped Method / Keep Rule Missing)"
            composeContext.isComposeIssue || fullStack.contains("androidx.compose") ->
                "🎨 Jetpack Compose Engine (Modifier.Node / Styles / Recomposition)"
            isAnr ->
                "⏳ Main UI Thread Starvation (ANR / Heavy Loop / Disk I/O)"
            fullStack.contains("android.graphics") || fullStack.contains("FLAG_IMMUTABLE") || fullStack.contains("BadTokenException") || fullStack.contains("RenderEffect") ->
                "📱 Android Framework / OS API Mismatch"
            fullStack.contains("OutOfMemoryError") ->
                "🧠 Heap Memory Exhaustion (OOM)"
            else ->
                "🧩 Application Business Logic / State"
        }
    }

    fun generateDetailedReason(
        triggerAction: String,
        failureOrigin: String,
        category: String,
        culpritClass: String,
        culpritMethod: String,
        culpritFile: String,
        culpritLine: Int,
        bytecodeDetails: BytecodeFailureDetails,
        composeContext: ComposeFailureContext,
        isAnr: Boolean,
        fullStack: String,
    ): String {
        val triggerMention = if (triggerAction.isNotBlank()) "When '$triggerAction' was tapped." else "During screen transition."
        val locationMention = if (culpritLine > 0) "line $culpritLine ($culpritFile)" else culpritFile

        return when {
            bytecodeDetails.isBytecodeIssue && bytecodeDetails.failureType.contains("CustomStyle") ->
                "⚡ **Root Cause:**\n" +
                "Jetpack Compose Foundation Styles / Liquid Glass effect uses `CustomStyle.applyStyle()`. " +
                "During release build minification, R8/ProGuard stripped or devirtualized this interface method on receiver ${bytecodeDetails.receiverType}. " +
                "Consequently, when Dalvik/ART runtime attempted to resolve this style in $locationMention, an `AbstractMethodError` was thrown.\n" +
                "• **Trigger Event:** $triggerMention"

            bytecodeDetails.isBytecodeIssue ->
                "⚡ **Root Cause:**\n" +
                "Symbol `${bytecodeDetails.missingSymbol}` was missing in DEX bytecode. The method was stripped from the runtime class due to R8 minification or library binary mismatch.\n" +
                "• **Trigger Event:** $triggerMention"

            triggerAction.contains("Link", ignoreCase = true) ->
                "⚡ **Root Cause:**\n" +
                "Error occurred while opening the Network Stream URL Dialog, during Compose layout attachment or window token binding ($locationMention).\n" +
                "• **Operation:** $category"

            triggerAction.contains("Telegram", ignoreCase = true) ->
                "⚡ **Root Cause:**\n" +
                "Error occurred while opening Telegram Cloud, during Telegram Screen or TDLib MTProto engine initialization ($locationMention).\n" +
                "• **Operation:** $category"

            triggerAction.contains("Search", ignoreCase = true) ->
                "⚡ **Root Cause:**\n" +
                "Error occurred while opening Search, during Search TextField or FocusRequester layout rendering ($locationMention).\n" +
                "• **Operation:** $category"

            composeContext.isComposeIssue ->
                "⚡ **Root Cause:**\n" +
                "Component failed during Jetpack Compose UI Tree `${composeContext.lifecyclePhase}` ($locationMention).\n" +
                "• **Trigger Event:** $triggerMention"

            isAnr ->
                "⚡ **Freeze Cause:**\n" +
                "The Main UI Thread was blocked for more than 4 seconds in disk I/O or heavy computation, causing an ANR (Application Not Responding).\n" +
                "• **Trigger Event:** $triggerMention"

            else ->
                "⚡ **Root Cause:**\n" +
                "$category error occurred in $locationMention.\n" +
                "• **Trigger Event:** $triggerMention"
        }
    }

    fun generateHindiReason(
        triggerAction: String,
        failureOrigin: String,
        category: String,
        culpritClass: String,
        culpritMethod: String,
        culpritFile: String,
        culpritLine: Int,
        bytecodeDetails: BytecodeFailureDetails,
        composeContext: ComposeFailureContext,
        isAnr: Boolean,
        fullStack: String,
    ): String = generateDetailedReason(
        triggerAction, failureOrigin, category, culpritClass, culpritMethod,
        culpritFile, culpritLine, bytecodeDetails, composeContext, isAnr, fullStack,
    )

    fun generateActionableFix(
        failureOrigin: String,
        bytecodeDetails: BytecodeFailureDetails,
        composeContext: ComposeFailureContext,
        recommendation: String,
        isAnr: Boolean,
    ): String {
        return when {
            bytecodeDetails.isBytecodeIssue && bytecodeDetails.suggestedKeepRule.isNotBlank() ->
                "🛠️ **Recommended Fix:**\n" +
                "1. Add the following rules to `app/proguard-rules.pro` and module `consumer-rules.pro`:\n" +
                "```proguard\n${bytecodeDetails.suggestedKeepRule}\n```\n" +
                "2. Ensure `-keepattributes DefaultMethod,InterfaceDefaultMethod,Synthetic,Bridge` is included.\n" +
                "3. Set `android.enableR8.fullMode=false` in `gradle.properties`."

            composeContext.isComposeIssue ->
                "🛠️ **Recommended Fix:**\n" +
                "1. $recommendation\n" +
                "2. Ensure all Liquid Glass / Backdrop modifiers and custom Modifier.Nodes properly attach and detach in Compose."

            isAnr ->
                "🛠️ **Recommended Fix:**\n" +
                "1. $recommendation\n" +
                "2. Offload disk I/O, file checks, and database queries from Main thread to `withContext(Dispatchers.IO)`."

            else ->
                "🛠️ **Recommended Fix:**\n" +
                "1. $recommendation"
        }
    }

    fun generateHindiFix(
        failureOrigin: String,
        bytecodeDetails: BytecodeFailureDetails,
        composeContext: ComposeFailureContext,
        recommendation: String,
        isAnr: Boolean,
    ): String = generateActionableFix(failureOrigin, bytecodeDetails, composeContext, recommendation, isAnr)

    private fun detectBytecodeFailure(
        fullStack: String,
        throwable: Throwable?,
    ): BytecodeFailureDetails {
        val message = throwable?.message ?: throwable?.localizedMessage ?: fullStack.lines().firstOrNull() ?: ""

        // 1. AbstractMethodError
        if (fullStack.contains("AbstractMethodError") || throwable is AbstractMethodError) {
            val matcher = ABSTRACT_METHOD_REGEX.matcher(message)
            val symbol: String
            val receiver: String
            if (matcher.find()) {
                symbol = matcher.group(1) ?: "unknownMethod"
                receiver = matcher.group(2) ?: "unknownReceiver"
            } else {
                symbol = message.substringAfter("abstract method \"").substringBefore("\"").ifBlank { "interface method" }
                receiver = message.substringAfter("on receiver ").ifBlank { "Class instance" }
            }

            val isCustomStyle = symbol.contains("CustomStyle") || fullStack.contains("foundation.style") || fullStack.contains("StyleOuterNode") || fullStack.contains("StyleInnerNode")
            val isR8Devirtualization = receiver.contains("java.lang.Class") || receiver.contains("Class<") || receiver.length <= 8
            val isComposeRelated = isCustomStyle || fullStack.contains("androidx.compose.ui.node") || fullStack.contains("runAttachLifecycle") || fullStack.contains("onAttach")

            val suggestedKeepRule = when {
                isCustomStyle -> buildString {
                    appendLine("# Protect Jetpack Compose Foundation Styles & CustomStyle implementors from R8 devirtualization")
                    appendLine("-keep class * implements androidx.compose.foundation.style.CustomStyle { *; }")
                    appendLine("-keep class * implements androidx.compose.foundation.style.** { *; }")
                    appendLine("-keep interface androidx.compose.foundation.style.** { *; }")
                    appendLine("-keep class androidx.compose.foundation.style.** { *; }")
                    appendLine("-keepclassmembers class * implements androidx.compose.foundation.style.CustomStyle {")
                    appendLine("    public void applyStyle(androidx.compose.foundation.style.CustomStyleScope);")
                    appendLine("    public *;")
                    appendLine("}")
                    appendLine("-keepclassmembers class * implements androidx.compose.foundation.style.** { *; }")
                    appendLine("-keep class * extends androidx.compose.foundation.style.** { *; }")
                    appendLine("-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,DefaultMethod,InterfaceDefaultMethod")
                    appendLine("-keep class * extends androidx.compose.ui.Modifier\$Node { *; }")
                    appendLine("-dontwarn androidx.compose.**")
                }.trimEnd()
                else -> buildString {
                    appendLine("# Protect Compose Modifier.Node implementations & interfaces from R8 devirtualization")
                    appendLine("-keep class * extends androidx.compose.ui.Modifier\$Node { *; }")
                    appendLine("-keep interface androidx.compose.ui.node.** { *; }")
                    appendLine("-keep interface androidx.compose.ui.modifier.** { *; }")
                    appendLine("-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,DefaultMethod,InterfaceDefaultMethod")
                    if (isComposeRelated) {
                        appendLine("-keep class ** extends androidx.compose.ui.node.DelegatableNode { *; }")
                        appendLine("-dontwarn androidx.compose.ui.**")
                    }
                }.trimEnd()
            }

            val steps = when {
                isCustomStyle -> listOf(
                    "Add '-keep class * implements androidx.compose.foundation.style.CustomStyle { *; }' to app/proguard-rules.pro and consumer-rules.pro.",
                    "Add '-keepattributes DefaultMethod,InterfaceDefaultMethod' so Java 8 default interface methods are preserved in DEX bytecode.",
                    "Ensure custom glass or backdrop modifiers retain their CustomStyleScope overrides.",
                    "Verify that 'android.enableR8.fullMode=false' is active in gradle.properties to disable aggressive devirtualization."
                )
                else -> listOf(
                    "Add the suggested ProGuard rules to app/proguard-rules.pro to protect Compose Modifier.Node subclasses and interface implementations.",
                    "Ensure any custom Modifier.Node implementing interfaces (e.g. ObserverModifierNode, DrawModifierNode) explicitly implements all required interface methods.",
                    "If using aggressive R8 full mode, test with 'android.enableR8.fullMode=false' in gradle.properties to disable interface devirtualization.",
                    "Verify all Jetpack Compose BOM and compiler versions are aligned without conflicting transitive dependencies."
                )
            }

            val failureType = when {
                isCustomStyle -> "Compose Foundation Style R8 Devirtualization (CustomStyle.applyStyle)"
                isR8Devirtualization -> "R8 Full-Mode Devirtualization (AbstractMethodError)"
                else -> "Abstract Method Not Implemented (AbstractMethodError)"
            }

            val technicalCause = when {
                isCustomStyle -> "Dalvik/ART failed to dispatch 'CustomStyle.applyStyle' on receiver '$receiver'. In Jetpack Compose Foundation 1.8+ (used by Backdrop / Liquid Glass), StyleOuterNode resolves custom styles during node attachment. In release builds, R8 minification stripped the interface implementation from implementor classes (like $receiver) because '-keep class * implements androidx.compose.foundation.style.CustomStyle' was missing."
                isR8Devirtualization -> "Dalvik/ART attempted to dispatch the interface call '$symbol' on receiver '$receiver'. When receiver is a java.lang.Class, R8 Full Mode devirtualized or stripped the default interface method implementation during DEX optimization."
                else -> "Dalvik/ART attempted to invoke the abstract method '$symbol' on receiver '$receiver', which failed to implement it."
            }

            return BytecodeFailureDetails(
                isBytecodeIssue = true,
                failureType = failureType,
                missingSymbol = symbol,
                receiverType = receiver,
                technicalCause = technicalCause,
                suggestedKeepRule = suggestedKeepRule,
                solutionSteps = steps,
            )
        }

        // 2. NoSuchMethodError
        if (fullStack.contains("NoSuchMethodError") || throwable is NoSuchMethodError) {
            val matcher = NO_SUCH_METHOD_REGEX.matcher(message)
            val (methodName, args, targetClass) = if (matcher.find()) {
                Triple(matcher.group(1) ?: "unknownMethod", matcher.group(2) ?: "", matcher.group(3) ?: "UnknownClass")
            } else {
                Triple(message.substringAfterLast('.').substringBefore('('), "", message.substringBeforeLast('.'))
            }

            val cleanClass = targetClass.removePrefix("L").removeSuffix(";").replace('/', '.')
            val suggestedKeepRule = "-keepclassmembers class $cleanClass {\n    public * $methodName(...);\n}"

            val steps = listOf(
                "Add '-keepclassmembers class $cleanClass { public * $methodName(...); }' to app/proguard-rules.pro.",
                "Check for library version mismatches where a method was deprecated or moved to a different package.",
                "Clean the build and verify that transitive dependencies have matching compile-time and runtime versions."
            )

            return BytecodeFailureDetails(
                isBytecodeIssue = true,
                failureType = "Binary Incompatibility / Missing Method (NoSuchMethodError)",
                missingSymbol = "$methodName($args)",
                receiverType = cleanClass,
                technicalCause = "Calling code expected method '$methodName' in '$cleanClass', but the symbol does not exist in the runtime DEX bytecode. This indicates an ABI library conflict or R8 stripping a dynamically invoked member.",
                suggestedKeepRule = suggestedKeepRule,
                solutionSteps = steps,
            )
        }

        // 3. NoSuchFieldError
        if (fullStack.contains("NoSuchFieldError") || throwable is NoSuchFieldError) {
            val field = message.substringAfterLast(' ').ifBlank { "unknownField" }
            val cleanClass = message.substringBeforeLast('.').ifBlank { "UnknownClass" }

            return BytecodeFailureDetails(
                isBytecodeIssue = true,
                failureType = "Binary Incompatibility / Missing Field (NoSuchFieldError)",
                missingSymbol = field,
                receiverType = cleanClass,
                technicalCause = "Field '$field' in '$cleanClass' was accessed at runtime but was not present in DEX bytecode.",
                suggestedKeepRule = "-keepclassmembers class $cleanClass { *; }",
                solutionSteps = listOf(
                    "Add '-keepclassmembers class $cleanClass { *; }' to ProGuard rules.",
                    "Check for library version incompatibilities."
                ),
            )
        }

        // 4. ClassNotFoundException or NoClassDefFoundError
        if (fullStack.contains("NoClassDefFoundError") || fullStack.contains("ClassNotFoundException") || throwable is NoClassDefFoundError || throwable is ClassNotFoundException) {
            val missingClass = message.substringAfter("Didn't find class \"").substringBefore("\"")
                .ifBlank { message.substringAfter("NoClassDefFoundError: ").trim() }

            if (missingClass.contains("RenderEffect") || missingClass.contains("RuntimeShader") || fullStack.contains("RenderEffect")) {
                return BytecodeFailureDetails(
                    isBytecodeIssue = true,
                    failureType = "Android Hardware AGSL / RenderEffect API Mismatch",
                    missingSymbol = "android.graphics.RenderEffect",
                    receiverType = "Android Framework (Requires API 31+)",
                    technicalCause = "android.graphics.RenderEffect was introduced in Android 12 (API 31). Invoking this API on Android 11 or lower without an SDK version check causes runtime NoClassDefFoundError.",
                    suggestedKeepRule = "# RenderEffect requires an SDK runtime check, not ProGuard rules",
                    solutionSteps = listOf(
                        "Wrap RenderEffect usage in: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { ... } else { fallback }",
                        "On API < 31 devices, fallback to frosted background box or semi-transparent overlay instead of hardware RenderEffect."
                    ),
                )
            }

            return BytecodeFailureDetails(
                isBytecodeIssue = true,
                failureType = "Missing Runtime Class Definition (NoClassDefFoundError)",
                missingSymbol = missingClass,
                receiverType = missingClass,
                technicalCause = "Class '$missingClass' was referenced but could not be loaded by Dalvik/ART ClassLoader.",
                suggestedKeepRule = "-keep class $missingClass { *; }",
                solutionSteps = listOf(
                    "Add '-keep class $missingClass { *; }' to app/proguard-rules.pro.",
                    "Ensure the required dependency is included in build.gradle.kts implementation configuration."
                ),
            )
        }

        // 5. IncompatibleClassChangeError or VerifyError
        if (fullStack.contains("IncompatibleClassChangeError") || fullStack.contains("VerifyError") || throwable is IncompatibleClassChangeError || throwable is VerifyError) {
            return BytecodeFailureDetails(
                isBytecodeIssue = true,
                failureType = "DEX Bytecode Verification Failure (LinkageError)",
                missingSymbol = message,
                receiverType = "Runtime ClassLoader",
                technicalCause = "Dalvik/ART bytecode verification failed. A class definition changed incompatibly between compile-time and runtime (e.g. interface changed to class).",
                suggestedKeepRule = "-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod",
                solutionSteps = listOf(
                    "Check for library version conflicts across modules.",
                    "Verify annotation processors and bytecode transform plugins are compatible with Android Gradle Plugin."
                ),
            )
        }

        return BytecodeFailureDetails(isBytecodeIssue = false)
    }

    private fun detectComposeContext(
        fullStack: String,
        lastNav: String,
    ): ComposeFailureContext {
        val isCompose = fullStack.contains("androidx.compose") ||
            fullStack.contains("runAttachLifecycle") ||
            fullStack.contains("onAttach") ||
            fullStack.contains("SubcomposeLayout") ||
            fullStack.contains("SnapshotMutableState") ||
            fullStack.contains("DelegatableNode")

        if (!isCompose) return ComposeFailureContext(isComposeIssue = false)

        val lifecyclePhase = when {
            fullStack.contains("foundation.style") || fullStack.contains("StyleOuterNode") || fullStack.contains("StyleInnerNode") || fullStack.contains("CustomStyle") ->
                "Compose Foundation Style Resolution (StyleOuterNode / CustomStyle.applyStyle)"
            fullStack.contains("runAttachLifecycle") || fullStack.contains(".onAttach(") ->
                "Modifier.Node Attachment (runAttachLifecycle / onAttach)"
            fullStack.contains("runDetachLifecycle") || fullStack.contains(".onDetach(") ->
                "Modifier.Node Detachment (runDetachLifecycle / onDetach)"
            fullStack.contains("onReset") ->
                "Modifier.Node Reset / Recycled Pool"
            fullStack.contains("measureAndLayout") || fullStack.contains("LayoutNode.measure") || fullStack.contains("LayoutModifierNode") ->
                "Compose Layout & Measurement Phase"
            fullStack.contains("ContentDrawScope") || fullStack.contains("DrawModifierNode") || fullStack.contains("drawBehind") || fullStack.contains("drawWithContent") ->
                "Compose Drawing & Graphics Canvas"
            fullStack.contains("pointerInput") || fullStack.contains("PointerInputScope") || fullStack.contains("detectTapGestures") ->
                "Compose Touch & Gesture Dispatch"
            fullStack.contains("SubcomposeLayout") || fullStack.contains("subcompose") ->
                "SubcomposeLayout Dynamic Subcomposition"
            fullStack.contains("SnapshotState") || fullStack.contains("StateObject") || fullStack.contains("recordWrite") ->
                "Compose State Snapshot Conflict"
            fullStack.contains("recompose") || fullStack.contains("ComposerImpl") || fullStack.contains("Recomposer") ->
                "Compose Recomposition Phase"
            else -> "Compose UI Node Tree Traversal"
        }

        val failingSubsystem = when {
            lifecyclePhase.startsWith("Compose Foundation Style") && lastNav.contains("Telegram", ignoreCase = true) ->
                "Telegram Cloud Liquid Glass & Styles"
            lifecyclePhase.startsWith("Compose Foundation Style") && lastNav.contains("MediaPicker", ignoreCase = true) ->
                "MediaPicker / Folder Hero Liquid Glass & Styles"
            lifecyclePhase.startsWith("Compose Foundation Style") && lastNav.contains("Settings", ignoreCase = true) ->
                "Settings Screen Liquid Glass & Styles"
            lastNav.contains("MediaPicker", ignoreCase = true) -> "MediaPicker / Folder Hero & Liquid Glass UI"
            lastNav.contains("Telegram", ignoreCase = true) -> "Telegram Cloud & Video Streams UI"
            lastNav.contains("Player", ignoreCase = true) -> "ExoPlayer & Controls UI"
            lastNav.contains("Settings", ignoreCase = true) -> "Settings & Appearance UI"
            else -> "Jetpack Compose Modifier.Node Tree"
        }

        val explanation = when {
            lifecyclePhase.startsWith("Compose Foundation Style") ->
                "Compose Foundation StyleOuterNode was resolving component styles during node attachment on screen '$lastNav', but the CustomStyle implementation was stripped by R8. In release builds, all CustomStyle implementors must be kept by ProGuard rules."
            lifecyclePhase.startsWith("Modifier.Node Attachment") ->
                "A Modifier.Node attached to a UI component on screen '$lastNav' failed during its attach lifecycle. This occurs when custom modifier nodes (e.g. Liquid Glass, backdrop sampling, bouncy click, or draw modifiers) fail to attach or have their interface methods stripped by R8."
            lifecyclePhase.startsWith("Compose Layout") ->
                "A measurement or layout pass failed on screen '$lastNav'. Commonly caused by infinite height/width constraints or NaN dimensions."
            lifecyclePhase.startsWith("Compose State Snapshot") ->
                "A mutable state was modified concurrently or read after disposal during composition on screen '$lastNav'."
            else ->
                "An unhandled Compose runtime failure occurred during $lifecyclePhase on screen '$lastNav'."
        }

        val advice = when {
            lifecyclePhase.startsWith("Compose Foundation Style") ->
                "Add '-keep class * implements androidx.compose.foundation.style.CustomStyle { *; }' to app/proguard-rules.pro and consumer-rules.pro. This ensures R8 preserves the style implementation method on '$lastNav'."
            lifecyclePhase.startsWith("Modifier.Node Attachment") ->
                "Inspect custom Modifier.Node implementations attached in '$lastNav' (e.g. layerBackdrop, bouncyPress, clickable, graphicsLayer). Ensure all interface methods are implemented and protected by ProGuard rules."
            else ->
                "Check recent UI changes in '$lastNav' for invalid layout constraints or state mutations during composition."
        }

        return ComposeFailureContext(
            isComposeIssue = true,
            lifecyclePhase = lifecyclePhase,
            failingSubsystem = failingSubsystem,
            probableScreen = lastNav,
            technicalExplanation = explanation,
            contextAdvice = advice,
        )
    }

    private fun extractLastNavFromText(text: String): String? {
        val lines = text.lines()
        for (i in lines.indices.reversed()) {
            val line = lines[i].trim()
            if (line.contains("[NAV]") && line.contains("Opened Screen / Tab: ")) {
                return line.substringAfter("Opened Screen / Tab: ").trim()
            }
        }
        return null
    }

    private fun resolveKotlinMethod(rawClass: String, rawMethod: String): Pair<String, String> {
        val simpleClass = rawClass.substringAfterLast('.')
        // Handle Kotlin coroutine state machines / lambdas e.g. Class$method$1
        if (rawMethod in listOf("invoke", "invokeSuspend", "apply", "run", "call") && simpleClass.contains('$')) {
            val parts = simpleClass.split('$')
            val outerClass = parts[0]
            val candidateMethod = parts.getOrNull(1)?.takeIf { it.isNotEmpty() && !it[0].isDigit() }
            if (candidateMethod != null) {
                return Pair(outerClass, "$candidateMethod (coroutine/lambda)")
            }
            return Pair(outerClass, rawMethod)
        }
        return Pair(simpleClass, rawMethod)
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
