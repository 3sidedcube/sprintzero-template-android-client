package com.cube.sprintzerotemplate.app.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.cube.sprintzerotemplate.R
import com.cube.sprintzerotemplate.databinding.FragmentMapBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment class for the map screen
 */
@AndroidEntryPoint
class MapFragment : Fragment(R.layout.fragment_map) {
	private var binding: FragmentMapBinding? = null

	companion object {
		/**
		 * Get an instance of the [LineupFragment]
		 */
		fun getInstance() = LineupFragment()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		FragmentMapBinding.bind(view).let {
			this.binding = it

			setUpUi()
		}
	}

	/**
	 * Method to set up and render the UI for this screen
	 */
	private fun setUpUi() {
		binding?.apply {
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