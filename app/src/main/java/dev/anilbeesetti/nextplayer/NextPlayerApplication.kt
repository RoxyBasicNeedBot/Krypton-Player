package dev.anilbeesetti.nextplayer

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import dev.anilbeesetti.nextplayer.core.common.di.ApplicationScope
import dev.anilbeesetti.nextplayer.core.data.repository.PreferencesRepository

import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

@HiltAndroidApp
class NextPlayerApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var tdlibManager: dev.anilbeesetti.nextplayer.core.data.tdlib.TdlibManager

    override fun onCreate() {
        super.onCreate()



        // Initialize Firebase App Check (Safe debug provider or permissive for sideloaded releases)
        try {
            val appCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG) {
                try {
                    val debugClass = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                    val getInstance = debugClass.getMethod("getInstance")
                    val factory = getInstance.invoke(null) as com.google.firebase.appcheck.AppCheckProviderFactory
                    appCheck.installAppCheckProviderFactory(factory)
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        // Initialize Firebase Remote Config (USB_DEBUG_POLICY switch)
        try {
            val remoteConfig = com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance()
            val defaults = mapOf("USB_DEBUG_POLICY" to "STRICT")
            remoteConfig.setDefaultsAsync(defaults)
            remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val policyStr = remoteConfig.getString("USB_DEBUG_POLICY")
                    dev.anilbeesetti.nextplayer.core.common.AppSecurityManager.currentUsbPolicy =
                        dev.anilbeesetti.nextplayer.core.common.UsbDebugPolicy.fromString(policyStr)
                }
            }
        } catch (_: Exception) {}



        // Initialize Firebase In-App Messaging (FIAM) & Performance Monitoring
        try {
            val fiam = com.google.firebase.inappmessaging.FirebaseInAppMessaging.getInstance()
            fiam.isAutomaticDataCollectionEnabled = true

            // Suppress FIAM in-app dialogs during full-screen video playback in PlayerActivity
            registerActivityLifecycleCallbacks(object : android.app.Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: android.os.Bundle?) {}
                override fun onActivityStarted(activity: android.app.Activity) {
                    if (activity.javaClass.simpleName == "PlayerActivity") {
                        try { fiam.setMessagesSuppressed(true) } catch (_: Exception) {}
                    }
                }
                override fun onActivityResumed(activity: android.app.Activity) {}
                override fun onActivityPaused(activity: android.app.Activity) {}
                override fun onActivityStopped(activity: android.app.Activity) {
                    if (activity.javaClass.simpleName == "PlayerActivity") {
                        try { fiam.setMessagesSuppressed(false) } catch (_: Exception) {}
                    }
                }
                override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: android.os.Bundle) {}
                override fun onActivityDestroyed(activity: android.app.Activity) {}
            })
        } catch (_: Exception) {}

        try {
            com.google.firebase.perf.FirebasePerformance.getInstance().isPerformanceCollectionEnabled = true
        } catch (_: Exception) {}

        // Initialize In-App Top Bar Announcement Manager (Remote Config & Firestore)
        try {
            dev.anilbeesetti.nextplayer.notification.FirebaseAnnouncementManager.initialize(this, applicationScope)
        } catch (_: Exception) {}

        // Initialize Firebase Cloud Messaging (Channel & Topic Subscription)
        try {
            dev.anilbeesetti.nextplayer.notification.FcmNotificationHelper.createNotificationChannel(this)
            val fcm = com.google.firebase.messaging.FirebaseMessaging.getInstance()
            fcm.subscribeToTopic("all")
            fcm.subscribeToTopic("announcements")
            fcm.token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    android.util.Log.d("NextPlayerApp", "FCM Device Token: $token")
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (currentUser != null && token != null) {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users").document(currentUser.uid)
                            .set(
                                mapOf("fcmToken" to token, "lastTokenUpdate" to System.currentTimeMillis()),
                                com.google.firebase.firestore.SetOptions.merge()
                            )
                    }
                }
            }
        } catch (_: Exception) {}

        applicationScope.launch {
            try {
                val prefs = preferencesRepository.playerPreferences.first()
                val botToken = prefs.telegramBotToken.ifBlank { "" }
                val apiId = if (prefs.telegramApiId != 0) prefs.telegramApiId else 0
                val apiHash = prefs.telegramApiHash.ifBlank { "" }

                tdlibManager.initialize(
                    botToken = botToken,
                    apiId = apiId,
                    apiHash = apiHash
                )
            } catch (e: Exception) {
                // Ignore cancellation or startup errors
            }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            imageLoader.memoryCache?.clear()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        imageLoader.memoryCache?.clear()
    }
}
