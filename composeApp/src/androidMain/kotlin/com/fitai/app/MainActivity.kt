package com.fitai.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.fitai.app.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

import com.fitai.app.firebase.FirebaseBootstrap
import com.fitai.app.firebase.setFirebaseAndroidContext
import com.fitai.app.notifications.ReminderNotifications
import com.fitai.app.platform.initAndroidPreferencesContext
import com.fitai.app.ui.notifications.NotificationDeepLink
import com.fitai.app.ui.notifications.PendingNotificationLinks

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initAndroidPreferencesContext(this)
        setFirebaseAndroidContext(this)
        FirebaseBootstrap.ensureInitialized()
        ReminderNotifications.ensureChannels(this)
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@MainActivity)
                modules(appModules())
            }
        }
        // A recreated activity (rotation) still holds the launch intent; only a fresh start opens its link.
        if (savedInstanceState == null) openNotificationLink(intent)
        setContent {
            FitAIApp()
        }
    }

    /** Notification tap while FitAI is running (launchMode singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openNotificationLink(intent)
    }

    /** Local reminders and server pushes (also when the system showed them) carry `link` in the extras. */
    private fun openNotificationLink(intent: Intent?) {
        PendingNotificationLinks.offer(intent?.getStringExtra(NotificationDeepLink.EXTRA_KEY))
    }
}

@Preview
@Composable
private fun FitAIAppAndroidPreview() {
    FitAIApp()
}
