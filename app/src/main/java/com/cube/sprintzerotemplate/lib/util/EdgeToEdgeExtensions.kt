package com.cube.sprintzerotemplate.lib.util

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

// Extensions for building edge-to-edge screens (enforced from targetSdk 35).
// ViewBindingActivity enables edge-to-edge for every screen; use these to keep content and
// touch targets clear of the system bars and display cutouts. Insets are not consumed, so
// sibling views still receive them.

/**
 * The inset types every edge-to-edge screen must respect
 */
private val insetTypes = WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()

/**
 * Method to pad this view by the system bar / display cutout insets on the chosen edges,
 * preserving any padding already set in the layout
 *
 * @param left Whether to apply the left inset
 * @param top Whether to apply the top inset
 * @param right Whether to apply the right inset
 * @param bottom Whether to apply the bottom inset
 */
fun View.applySystemBarInsetsAsPadding(left: Boolean = false, top: Boolean = false, right: Boolean = false, bottom: Boolean = false) {
	val initialLeft = paddingLeft
	val initialTop = paddingTop
	val initialRight = paddingRight
	val initialBottom = paddingBottom

	ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
		val insets = windowInsets.getInsets(insetTypes)
		view.updatePadding(
			left = initialLeft + if (left) insets.left else 0,
			top = initialTop + if (top) insets.top else 0,
			right = initialRight + if (right) insets.right else 0,
			bottom = initialBottom + if (bottom) insets.bottom else 0
		)
		windowInsets
	}
}

/**
 * Method to offset this view by the system bar / display cutout insets on the chosen edges via
 * layout margins, preserving any margins already set in the layout. Prefer the padding variant;
 * use this for views where padding changes the look (e.g. FABs, cards)
 *
 * @param left Whether to apply the left inset
 * @param top Whether to apply the top inset
 * @param right Whether to apply the right inset
 * @param bottom Whether to apply the bottom inset
 */
fun View.applySystemBarInsetsAsMargin(left: Boolean = false, top: Boolean = false, right: Boolean = false, bottom: Boolean = false) {
	val params = layoutParams as ViewGroup.MarginLayoutParams
	val initialLeft = params.leftMargin
	val initialTop = params.topMargin
	val initialRight = params.rightMargin
	val initialBottom = params.bottomMargin

	ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
		val insets = windowInsets.getInsets(insetTypes)
		view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
			leftMargin = initialLeft + if (left) insets.left else 0
			topMargin = initialTop + if (top) insets.top else 0
			rightMargin = initialRight + if (right) insets.right else 0
			bottomMargin = initialBottom + if (bottom) insets.bottom else 0
		}
		windowInsets
	}
}