package com.mapzone.mapzonealertview

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mapzone.mapzonealertview.config.AppConfig
import com.mapzone.mapzonealertview.presentation.controllers.NavigationController
import com.mapzone.mapzonealertview.presentation.controllers.SpeedAlertController
import com.mapzone.mapzonealertview.presentation.feature.map.MapScreen
import com.mapzone.mapzonealertview.presentation.theme.AlertViewSDKTheme
import vn.vietmap.vietmapsdk.Vietmap

class MainActivity : ComponentActivity() {

    private lateinit var navController: NavigationController
    private lateinit var speedAlert: SpeedAlertController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Vietmap.getInstance(applicationContext)
        } catch (t: Throwable) {
            Log.w("MainActivity", "Vietmap init: ${t.message}")
        }
        try {
            AppConfig.assertReady()
        } catch (t: Throwable) {
            Log.e("MainActivity", t.message, t)
        }

        speedAlert = SpeedAlertController(applicationContext)
        navController = NavigationController(applicationContext, speedAlert)

        enableEdgeToEdge()
        setContent {
            AlertViewSDKTheme {
                MapScreen(
                    navController = navController,
                    speedAlert = speedAlert,
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        navController.destroy()
        speedAlert.release()
    }
}
