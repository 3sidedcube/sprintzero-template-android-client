package com.cube.sprintzerotemplate.app.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.cube.sprintzerotemplate.lib.util.AnalyticsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * BootActivity is the main activity that gets launched when the device boots up.
 * It serves as the entry point for the application and performs necessary initialization
 * or setup tasks while displaying the splash screen.
 *
 * TODO Add any project specific setup
 */
class BootActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val splashScreen = installSplashScreen()
		splashScreen.setKeepOnScreenCondition { true }

		lifecycleScope.launch {
			delay(3000L)

			AnalyticsHelper.setupAnalytics(true)

			startActivity(HomeActivity.getIntent(this@BootActivity))
		}
	}
}