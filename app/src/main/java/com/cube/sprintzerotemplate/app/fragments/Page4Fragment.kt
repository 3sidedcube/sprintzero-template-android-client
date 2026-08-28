package com.cube.sprintzerotemplate.app.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.cube.sprintzerotemplate.R
import com.cube.sprintzerotemplate.databinding.FragmentPage4Binding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment class for the page 4 tab
 */
@AndroidEntryPoint
class Page4Fragment : Fragment(R.layout.fragment_page4) {
	private var binding: FragmentPage4Binding? = null

	companion object {
		/**
		 * Get an instance of the [Page4Fragment]
		 */
		fun getInstance() = Page4Fragment()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		FragmentPage4Binding.bind(view).let {
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