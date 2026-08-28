package com.cube.sprintzerotemplate.app.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.cube.sprintzerotemplate.R
import com.cube.sprintzerotemplate.databinding.FragmentPage1Binding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment class for the page 1 tab
 */
@AndroidEntryPoint
class Page1Fragment : Fragment(R.layout.fragment_page1) {
	private var binding: FragmentPage1Binding? = null

	companion object {
		/**
		 * Get an instance of the [Page1Fragment]
		 */
		fun getInstance() = Page1Fragment()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		FragmentPage1Binding.bind(view).let {
			this.binding = it
			setUpUi()
		}
	}

	/**
	 * Method to set up and render the UI for this screen
	 */
	private fun setUpUi() {
		binding?.apply {
			// TODO Implement later
		}
	}

	override fun onDestroyView() {
		super.onDestroyView()
		binding = null
	}
}