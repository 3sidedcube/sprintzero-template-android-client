package com.cube.sprintzerotemplate.lib.extensions

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Robolectric tests for the edge-to-edge extensions, pinning the hardening rules ported from
 * arc-firstaid: no compounding on repeated dispatch, phantom insets gated on bar visibility,
 * per-edge cutout merging, balanced horizontal padding and opt-in keyboard insets
 */
@RunWith(AndroidJUnit4::class)
class EdgeToEdgeExtensionsTest {
	private val context: Context = ApplicationProvider.getApplicationContext()
	private lateinit var view: View

	@Before
	fun setUp() {
		view = View(context).apply {
			layoutParams = FrameLayout.LayoutParams(100, 100).apply { setMargins(5, 5, 5, 5) }
			setPadding(10, 10, 10, 10)
		}
	}

	@Test
	fun padding_appliedToRequestedEdgesOnly_preservingInitialPadding() {
		view.applySystemBarInsetsAsPadding(bottom = true)

		dispatch(insets(navBottom = 100, statusTop = 50))

		assertEquals(110, view.paddingBottom)
		assertEquals(10, view.paddingTop)
		assertEquals(10, view.paddingLeft)
		assertEquals(10, view.paddingRight)
	}

	@Test
	fun padding_repeatedDispatch_neverCompounds() {
		view.applySystemBarInsetsAsPadding(bottom = true)

		dispatch(insets(navBottom = 100))
		dispatch(insets(navBottom = 100))
		dispatch(insets(navBottom = 100))

		assertEquals(110, view.paddingBottom)
	}

	@Test
	fun padding_hiddenBars_contributeNothing() {
		view.applySystemBarInsetsAsPadding(top = true, bottom = true)

		dispatch(insets(navBottom = 100, statusTop = 50, navVisible = false, statusVisible = false))

		assertEquals(10, view.paddingTop)
		assertEquals(10, view.paddingBottom)
	}

	@Test
	fun padding_displayCutout_mergedEvenWhenBarsHidden() {
		view.applySystemBarInsetsAsPadding(left = true)

		dispatch(insets(cutoutLeft = 80, navVisible = false))

		assertEquals(90, view.paddingLeft)
	}

	@Test
	fun padding_balancedHorizontal_padsBothSidesByTheLargerInset() {
		view.applySystemBarInsetsAsPadding(left = true, right = true, balancedHorizontal = true)

		dispatch(insets(navRight = 100))

		assertEquals(110, view.paddingLeft)
		assertEquals(110, view.paddingRight)
	}

	@Test
	fun padding_includeIme_addsKeyboardInsetToBottom() {
		view.applySystemBarInsetsAsPadding(bottom = true, includeIme = true)

		dispatch(insets(navBottom = 100, ime = 300))

		assertEquals(410, view.paddingBottom)
	}

	@Test
	fun padding_imeIgnoredWhenNotOptedIn() {
		view.applySystemBarInsetsAsPadding(bottom = true)

		dispatch(insets(navBottom = 100, ime = 300))

		assertEquals(110, view.paddingBottom)
	}

	@Test
	fun margins_appliedToRequestedEdgesOnly_preservingInitialMargins() {
		view.applySystemBarInsetsAsMargin(bottom = true)

		dispatch(insets(navBottom = 100))
		dispatch(insets(navBottom = 100))

		val params = view.layoutParams as FrameLayout.LayoutParams
		assertEquals(105, params.bottomMargin)
		assertEquals(5, params.topMargin)
		assertEquals(5, params.leftMargin)
		assertEquals(5, params.rightMargin)
	}

	private fun dispatch(insets: WindowInsetsCompat) {
		ViewCompat.dispatchApplyWindowInsets(view, insets)
	}

	private fun insets(navBottom: Int = 0, navRight: Int = 0, statusTop: Int = 0, cutoutLeft: Int = 0, ime: Int = 0, navVisible: Boolean = true, statusVisible: Boolean = true) =
		WindowInsetsCompat.Builder()
			.setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, navRight, navBottom))
			.setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, statusTop, 0, 0))
			.setInsets(WindowInsetsCompat.Type.displayCutout(), Insets.of(cutoutLeft, 0, 0, 0))
			.setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, ime))
			.setVisible(WindowInsetsCompat.Type.navigationBars(), navVisible)
			.setVisible(WindowInsetsCompat.Type.statusBars(), statusVisible)
			.setVisible(WindowInsetsCompat.Type.ime(), ime > 0)
			.build()
}