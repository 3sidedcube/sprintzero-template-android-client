package com.cube.sprintzerotemplate.app.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.cube.sprintzerotemplate.R
import com.cube.sprintzerotemplate.databinding.FragmentPage2Binding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment class for the page 2 tab
 */
@AndroidEntryPoint
class Page2Fragment : Fragment(R.layout.fragment_page2) {
	private var binding: FragmentPage2Binding? = null

	companion object {
		/**
		 * Get an instance of the [Page2Fragment]
		 */
		fun getInstance() = Page2Fragment()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		FragmentPage2Binding.bind(view).let {
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