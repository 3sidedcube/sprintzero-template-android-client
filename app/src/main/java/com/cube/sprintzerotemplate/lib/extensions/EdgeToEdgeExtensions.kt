package com.cube.sprintzerotemplate.lib.extensions

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.cube.sprintzerotemplate.R

// Extensions for building edge-to-edge screens (enforced from targetSdk 35). ViewBindingActivity
// enables edge-to-edge for every screen; use these to keep content and touch targets clear of the
// system bars, display cutouts and (opt-in) the keyboard. Hardened with the lessons from
// arc-firstaid-android-client's EdgeToEdgeUtils (PR #267 and the fix chain that led to it):
//
// - Initial padding/margins are captured once in a view tag, so rotation, IME show/hide and
//   re-registration never compound the insets.
// - Each bar's inset is gated on its visibility — some devices (older Samsungs on Android 13)
//   keep reporting an inset after a bar is hidden, leaving a phantom strip.
// - Only the requested edges are touched; padding set on other edges by external mechanisms
//   (e.g. a scroll-hide toolbar) is never clobbered.
//
// Gotchas: call at most ONE of these per view — a second call replaces the first listener (one
// call handles all edges, so combine flags instead). For scrolling views, pair bottom padding
// with android:clipToPadding="false" so content can scroll into the inset region.

/**
 * Method to pad this view by the system bar / display cutout insets on the chosen edges,
 * preserving any padding already set in the layout. Insets are not consumed, so sibling views
 * still receive them
 *
 * @param left Whether to apply the left inset (landscape nav bar / cutout)
 * @param top Whether to apply the top inset (status bar / cutout)
 * @param right Whether to apply the right inset (landscape nav bar / cutout)
 * @param bottom Whether to apply the bottom inset (nav bar)
 * @param includeIme Whether the keyboard inset is added to the bottom edge so the view stays
 * above an open keyboard — opt in on screens with text input only
 * @param balancedHorizontal When both [left] and [right] are applied, pad both sides by
 * max(left, right) so centred content stays visually centred when only one edge has an inset
 */
fun View.applySystemBarInsetsAsPadding(left: Boolean = false, top: Boolean = false, right: Boolean = false, bottom: Boolean = false, includeIme: Boolean = false, balancedHorizontal: Boolean = false) {
	ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
		val initial = view.initialRect(R.id.tag_initial_padding) { Rect(paddingLeft, paddingTop, paddingRight, paddingBottom) }
		val insets = windowInsets.effectiveInsets(includeIme)
		val (appliedLeft, appliedRight) = insets.horizontal(left, right, balancedHorizontal)

		view.setPadding(
			if (left) initial.left + appliedLeft else view.paddingLeft,
			if (top) initial.top + insets.top else view.paddingTop,
			if (right) initial.right + appliedRight else view.paddingRight,
			if (bottom) initial.bottom + insets.bottom else view.paddingBottom
		)
		windowInsets
	}
	// Dispatch immediately in case the view is already attached and won't fire onAttachedToWindow
	ViewCompat.requestApplyInsets(this)
}

/**
 * Method to offset this view by the system bar / display cutout insets on the chosen edges via
 * layout margins, preserving any margins already set in the layout. Prefer the padding variant;
 * use this for views where padding changes the look (e.g. FABs, cards, bottom sheets)
 *
 * @param left Whether to apply the left inset (landscape nav bar / cutout)
 * @param top Whether to apply the top inset (status bar / cutout)
 * @param right Whether to apply the right inset (landscape nav bar / cutout)
 * @param bottom Whether to apply the bottom inset (nav bar)
 * @param includeIme Whether the keyboard inset is added to the bottom edge — opt in on screens
 * with text input only
 * @param balancedHorizontal When both [left] and [right] are applied, offset both sides by
 * max(left, right) so centred views (e.g. bottom sheets) stay centred when only one edge has
 * an inset
 */
fun View.applySystemBarInsetsAsMargin(left: Boolean = false, top: Boolean = false, right: Boolean = false, bottom: Boolean = false, includeIme: Boolean = false, balancedHorizontal: Boolean = false) {
	ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
		val params = view.layoutParams as ViewGroup.MarginLayoutParams
		val initial = view.initialRect(R.id.tag_initial_margins) { Rect(params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin) }
		val insets = windowInsets.effectiveInsets(includeIme)
		val (appliedLeft, appliedRight) = insets.horizontal(left, right, balancedHorizontal)

		view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			if (left) leftMargin = initial.left + appliedLeft
			if (top) topMargin = initial.top + insets.top
			if (right) rightMargin = initial.right + appliedRight
			if (bottom) bottomMargin = initial.bottom + insets.bottom
		}
		windowInsets
	}
	ViewCompat.requestApplyInsets(this)
}

/**
 * The per-edge insets an edge-to-edge screen must respect. Each bar is read individually and
 * gated on its visibility (some devices report phantom insets for hidden bars), then merged
 * with the display cutout per edge. The keyboard inset joins the bottom edge when requested
 */
private fun WindowInsetsCompat.effectiveInsets(includeIme: Boolean): Insets {
	val statusBars = getInsets(WindowInsetsCompat.Type.statusBars())
	val navBars = getInsets(WindowInsetsCompat.Type.navigationBars())
	val cutout = getInsets(WindowInsetsCompat.Type.displayCutout())
	val ime = if (includeIme) getInsets(WindowInsetsCompat.Type.ime()) else Insets.NONE
	val statusBarsVisible = isVisible(WindowInsetsCompat.Type.statusBars())
	val navBarsVisible = isVisible(WindowInsetsCompat.Type.navigationBars())

	return Insets.of(
		maxOf(if (navBarsVisible) navBars.left else 0, cutout.left),
		maxOf(if (statusBarsVisible) statusBars.top else 0, cutout.top),
		maxOf(if (navBarsVisible) navBars.right else 0, cutout.right),
		maxOf(if (navBarsVisible) navBars.bottom else 0, cutout.bottom) + ime.bottom
	)
}

/**
 * The left/right insets to apply, optionally balanced to max(left, right) on both sides
 */
private fun Insets.horizontal(left: Boolean, right: Boolean, balanced: Boolean): Pair<Int, Int> {
	val appliedLeft = if (left) this.left else 0
	val appliedRight = if (right) this.right else 0

	return if (balanced && left && right) {
		val max = maxOf(appliedLeft, appliedRight)
		max to max
	} else {
		appliedLeft to appliedRight
	}
}

/**
 * The view's original padding/margins, captured once in a tag so repeated listener firings
 * (rotation, keyboard, re-registration) never compound the insets
 */
private fun View.initialRect(tagId: Int, capture: View.() -> Rect): Rect {
	(getTag(tagId) as? Rect)?.let { return it }
	return capture().also { setTag(tagId, it) }
}