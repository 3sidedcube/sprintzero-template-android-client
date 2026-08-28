package com.cube.sprintzerotemplate.lib.generic

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 * Base activity class for creating activities which use view binding.
 * Screens are edge-to-edge (enforced from targetSdk 35) — keep content clear of the system
 * bars with the extensions in `lib/extensions/EdgeToEdgeExtensions.kt`
 *
 * @author Kieran Hawkins
 */
abstract class ViewBindingActivity<T : ViewBinding> : AppCompatActivity() {
	protected lateinit var binding: T

	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)

		binding = inflateLayout(layoutInflater)
		setContentView(binding.root)
	}

	abstract fun inflateLayout(layoutInflater: LayoutInflater): T
}