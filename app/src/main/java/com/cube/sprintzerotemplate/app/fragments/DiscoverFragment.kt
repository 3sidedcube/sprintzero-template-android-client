package com.cube.sprintzerotemplate.app.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.cube.sprintzerotemplate.R
import com.cube.sprintzerotemplate.databinding.FragmentDiscoverBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment class for the discover screen
 */
@AndroidEntryPoint
class DiscoverFragment : Fragment(R.layout.fragment_discover) {
	private var binding: FragmentDiscoverBinding? = null

	companion object {
		/**
		 * Get an instance of the [DiscoverFragment]
		 */
		fun getInstance() = DiscoverFragment()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		FragmentDiscoverBinding.bind(view).let {
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

	override fun onResume() {
		super.onResume()
	}

	override fun onDestroyView() {
		super.onDestroyView()
		binding = null
	}
}