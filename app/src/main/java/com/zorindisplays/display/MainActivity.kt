package com.zorindisplays.display

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat


class MainActivity : ComponentActivity() {

/*
alias hilo='curl -s "http://localhost:18080/status" | head -c 200; echo'
alias arm='curl -s "http://localhost:18080/cmd/arm?table=2&box=3" >/dev/null'
alias buy='curl -s "http://localhost:18080/cmd/buyin?amount=100" >/dev/null'
alias hi='curl -s "http://localhost:18080/cmd/choose?side=HI" >/dev/null'
alias lo='curl -s "http://localhost:18080/cmd/choose?side=LO" >/dev/null'
alias ok='curl -s "http://localhost:18080/cmd/confirm" >/dev/null'
alias rst='curl -s "http://localhost:18080/cmd/reset" >/dev/null'
*/

    private lateinit var model: AppModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        model = AppModel(this)
        model.start()

        setContent {
            App(model)
        }

        hideSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(
                    WindowInsets.Type.statusBars() or
                            WindowInsets.Type.navigationBars()
                )
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 7–10 (API 24–29)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    override fun onDestroy() {
        model.stop()
        super.onDestroy()
    }
}
