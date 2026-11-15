package com.fitai.app

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
        setContent {
            FitAIApp()
        }
    }
}

@Preview
@Composable
private fun FitAIAppAndroidPreview() {
    FitAIApp()
}
