package com.cube.sprintzerotemplate.app.activities

import android.os.Looper
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cube.sprintzerotemplate.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * Robolectric tests for [MainTabbedActivity] — the example for testing a Hilt
 * (@AndroidEntryPoint) activity on the JVM: @HiltAndroidTest + HiltAndroidRule, with
 * HiltTestApplication supplied globally by robolectric.properties
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainTabbedActivityTest {
	@get:Rule
	val hiltRule = HiltAndroidRule(this)

	@Test
	fun launch_showsFiveTabsWithPage1Selected() {
		ActivityScenario.launch(MainTabbedActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				val bottomNav = activity.findViewById<BottomNavigationView>(R.id.bottom_nav)

				assertEquals(5, bottomNav.menu.size())
				assertEquals(R.id.page1_nav, bottomNav.selectedItemId)
			}
		}
	}

	@Test
	fun launch_startDestinationIsPage1() {
		ActivityScenario.launch(MainTabbedActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				assertEquals(R.id.page1_tab, activity.currentDestinationId())
			}
		}
	}

	@Test
	fun selectingATab_navigatesToItsPage() {
		ActivityScenario.launch(MainTabbedActivity::class.java).use { scenario ->
			scenario.onActivity { activity ->
				activity.findViewById<BottomNavigationView>(R.id.bottom_nav).selectedItemId = R.id.page3_nav
				shadowOf(Looper.getMainLooper()).idle()

				assertEquals(R.id.page3_tab, activity.currentDestinationId())
			}
		}
	}

	private fun MainTabbedActivity.currentDestinationId(): Int? {
		val navHost = supportFragmentManager.findFragmentById(R.id.fragment_container_view) as NavHostFragment
		return navHost.navController.currentDestination?.id
	}
}