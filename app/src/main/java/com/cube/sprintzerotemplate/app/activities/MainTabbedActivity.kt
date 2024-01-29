package com.cube.sprintzerotemplate.app.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.navigation.NavGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.cube.sprintzerotemplate.R
import com.cube.sprintzerotemplate.databinding.ActivityMainTabbedBinding
import com.cube.sprintzerotemplate.lib.generic.ViewBindingActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity for the main tab screen
 */
@AndroidEntryPoint
class MainTabbedActivity : ViewBindingActivity<ActivityMainTabbedBinding>() {
	companion object {
		/**
		 * Get an intent to direct the app to the [MainTabbedActivity]
		 *
		 * @param context A [Context] of the application package implementing this class
		 */
		fun getIntent(context: Context) = Intent(context, MainTabbedActivity::class.java)
	}

	override fun inflateLayout(layoutInflater: LayoutInflater): ActivityMainTabbedBinding = ActivityMainTabbedBinding.inflate(layoutInflater)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setUpNavigation()
	}

	/**
	 * Set up the navigation with the bottom navigation bar
	 */
	private fun setUpNavigation() {
		val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view_home) as NavHostFragment
		val navController = navHostFragment.navController

		// Setup the bottom navigation view with navController
		val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
		bottomNavigationView.setupWithNavController(navController)

		// When re-selecting the current tab, pop the tabs stack back to the root
		bottomNavigationView.setOnItemReselectedListener {
			val selectedMenuItemNavGraph = navHostFragment.navController.graph.findNode(it.itemId) as? NavGraph?
			selectedMenuItemNavGraph?.let { menuGraph ->
				navHostFragment.navController.popBackStack(menuGraph.startDestinationId, false)
			}
		}

		bottomNavigationView.setOnItemSelectedListener {
			NavigationUI.onNavDestinationSelected(it, navController)
			true
		}
	}
}