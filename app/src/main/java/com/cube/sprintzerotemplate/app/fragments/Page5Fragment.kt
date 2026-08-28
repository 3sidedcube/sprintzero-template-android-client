package com.cube.sprintzerotemplate.app.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.cube.sprintzerotemplate.R
import com.cube.sprintzerotemplate.databinding.FragmentPage5Binding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment class for the page 5 tab
 */
@AndroidEntryPoint
class Page5Fragment : Fragment(R.layout.fragment_page5) {
	private var binding: FragmentPage5Binding? = null

	companion object {
		/**
		 * Get an instance of the [Page5Fragment]
		 */
		fun getInstance() = Page5Fragment()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		FragmentPage5Binding.bind(view).let {
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