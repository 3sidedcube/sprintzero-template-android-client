package com.cube.sprintzerotemplate.lib.generic

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

/**
 * Base activity class for creating activities which use view binding
 *
 * @author Kieran Hawkins
 */
abstract class ViewBindingActivity<T : ViewBinding> : AppCompatActivity() {
	protected lateinit var binding: T

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		binding = inflateLayout(layoutInflater)
		setContentView(binding.root)
	}

	abstract fun inflateLayout(layoutInflater: LayoutInflater): T
}