package com.cube.sprintzerotemplate.app.activities

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.cube.sprintzerotemplate.R
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

/**
 * Espresso tests for [MainTabbedActivity] — the example for instrumented tests of a Hilt
 * (@AndroidEntryPoint) activity: HiltAndroidRule runs first (order 0), then
 * ActivityScenarioRule launches the screen. Runs through HiltTestRunner
 */
@HiltAndroidTest
class MainTabbedActivityInstrumentedTest {
	@get:Rule(order = 0)
	val hiltRule = HiltAndroidRule(this)

	@get:Rule(order = 1)
	val activityRule = ActivityScenarioRule(MainTabbedActivity::class.java)

	@Test
	fun launch_showsPage1WithTheTabBar() {
		onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()))
		onView(withId(R.id.text_sample)).check(matches(withText("Page 1")))
	}

	@Test
	fun selectingEachTab_showsThatPage() {
		listOf(
			R.id.page2_nav to "Page 2",
			R.id.page3_nav to "Page 3",
			R.id.page4_nav to "Page 4",
			R.id.page5_nav to "Page 5",
			R.id.page1_nav to "Page 1"
		).forEach { (tabId, pageTitle) ->
			onView(withId(tabId)).perform(click())

			onView(withId(R.id.text_sample)).check(matches(withText(pageTitle)))
		}
	}
}