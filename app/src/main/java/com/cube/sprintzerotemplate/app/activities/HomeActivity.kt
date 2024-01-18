package com.cube.sprintzerotemplate.app.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import com.cube.sprintzerotemplate.databinding.ActivityHomeBinding
import com.cube.sprintzerotemplate.lib.generic.ViewBindingActivity

/**
 * Activity for the main home screen
 * TODO Update as required
 */
class HomeActivity : ViewBindingActivity<ActivityHomeBinding>() {
	companion object {
		/**
		 * Get an intent to direct the app to the [HomeActivity]
		 *
		 * @param context A [Context] of the application package implementing this class
		 */
		fun getIntent(context: Context) = Intent(context, HomeActivity::class.java)
	}

	override fun inflateLayout(layoutInflater: LayoutInflater): ActivityHomeBinding = ActivityHomeBinding.inflate(layoutInflater)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding.textSample.text = "Welcome"
	}
}