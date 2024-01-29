package com.cube.sprintzerotemplate.lib.util

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Class to handle permission requests
 *
 * @param activity The calling activity
 * @param onPermissionsGranted The callback containing requested permissions that were granted
 * @param onAllPermissionsDenied The callback for when all permissions where denied
 */
class PermissionsHelper(
	private val activity: AppCompatActivity,
	private val onPermissionsGranted: (List<String>) -> Unit,
	private val onAllPermissionsDenied: () -> Unit
) {
	/**
	 * Permission result launcher
	 */
	private val permissionsRequestLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
		val grantedPermissions = permissions.entries
			.filter { it.value }
			.map { it.key }

		if (grantedPermissions.isNotEmpty()) {
			onPermissionsGranted.invoke(grantedPermissions)
		} else {
			onAllPermissionsDenied.invoke()
		}
	}

	/**
	 * Method to check permission status and request them if required
	 *
	 * @param permissions The permission to be checked and requested
	 */
	fun checkPermissions(vararg permissions: String) {
		val permissionsToRequest = permissions.filter {
			if (it == Manifest.permission.POST_NOTIFICATIONS && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
				false
			} else {
				ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
			}
		}

		if (permissionsToRequest.isNotEmpty()) {
			permissionsRequestLauncher.launch(permissions.toList().toTypedArray())
		} else {
			onPermissionsGranted.invoke(permissions.toList())
		}
	}
}