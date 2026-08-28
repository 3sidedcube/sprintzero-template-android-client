package com.cube.sprintzerotemplate.lib.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Helper to check and request runtime ("dangerous") permissions, reporting a single [Result]
 * per request with the granted, denied and permanently denied permissions.
 *
 * Construct this as a property of the host activity/fragment (or in its onCreate) — it registers
 * an ActivityResultLauncher, which the framework only allows before the host reaches STARTED.
 * Creating one later (e.g. in a click listener) will throw.
 *
 * Limitations to be aware of:
 * - Runtime permissions only. Special permissions (e.g. SYSTEM_ALERT_WINDOW,
 *   MANAGE_EXTERNAL_STORAGE, SCHEDULE_EXACT_ALARM) never show a dialog and must be granted by
 *   the user via a Settings intent instead.
 * - ACCESS_BACKGROUND_LOCATION must be requested on its own in a separate request after
 *   foreground location has been granted — never bundle it with other permissions.
 * - Choosing version-specific permissions (e.g. READ_MEDIA_* on 33+ vs READ_EXTERNAL_STORAGE
 *   below) is the caller's responsibility; only POST_NOTIFICATIONS, which has no pre-33
 *   equivalent, is version-gated here (treated as granted below API 33).
 * - Run one request at a time; a second checkPermissions call before the first result arrives
 *   will mix up the already-granted bookkeeping.
 *
 * @param caller The activity/fragment to register the request launcher on
 * @param activityProvider Lazily provides the host [Activity] for permission state checks
 * @param onResult The callback receiving the [Result] of each request
 */
class PermissionsHelper private constructor(caller: ActivityResultCaller, private val activityProvider: () -> Activity, private val onResult: (Result) -> Unit) {
	/**
	 * @param activity The host activity, also used to register the request launcher
	 * @param onResult The callback receiving the [Result] of each request
	 */
	constructor(activity: AppCompatActivity, onResult: (Result) -> Unit) : this(activity, { activity }, onResult)

	/**
	 * @param fragment The host fragment to register the request launcher on
	 * @param onResult The callback receiving the [Result] of each request
	 */
	constructor(fragment: Fragment, onResult: (Result) -> Unit) : this(fragment, { fragment.requireActivity() }, onResult)

	/**
	 * The outcome of a [checkPermissions] call
	 *
	 * @param granted The requested permissions that are now granted (including ones that already were)
	 * @param denied The requested permissions that remain denied
	 * @param permanentlyDenied The subset of [denied] the system will no longer show a dialog for —
	 * the user must be sent to app settings to grant these. Detected via the standard heuristic
	 * (denied without a rationale prompt), so it is only meaningful directly after a request
	 */
	data class Result(val granted: List<String>, val denied: List<String>, val permanentlyDenied: List<String>) {
		val allGranted: Boolean
			get() = denied.isEmpty()
	}

	/**
	 * Permissions from the current request that were already granted before it was launched,
	 * merged back into the launcher result so [Result.granted] always covers the full request
	 */
	private var grantedBeforeRequest = emptyList<String>()

	/**
	 * Permission result launcher
	 */
	private val permissionsRequestLauncher = caller.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
		val granted = grantedBeforeRequest + results.filterValues { it }.keys
		val denied = results.filterValues { !it }.keys.toList()
		val permanentlyDenied = denied.filterNot { ActivityCompat.shouldShowRequestPermissionRationale(activityProvider(), it) }
		grantedBeforeRequest = emptyList()

		onResult(Result(granted, denied, permanentlyDenied))
	}

	/**
	 * Method to check permission status and request any that are missing. [onResult] is always
	 * invoked exactly once — immediately if nothing needs requesting, otherwise once the user
	 * has responded to the system dialog
	 *
	 * @param permissions The permissions to be checked and requested
	 */
	fun checkPermissions(vararg permissions: String) {
		val missing = permissions.filter { permission ->
			isApplicable(permission) && ContextCompat.checkSelfPermission(activityProvider(), permission) != PackageManager.PERMISSION_GRANTED
		}

		if (missing.isEmpty()) {
			onResult(Result(granted = permissions.toList(), denied = emptyList(), permanentlyDenied = emptyList()))
		} else {
			grantedBeforeRequest = permissions.toList() - missing.toSet()
			permissionsRequestLauncher.launch(missing.toTypedArray())
		}
	}

	/**
	 * Whether a permission exists on this OS version; inapplicable ones are treated as granted
	 * rather than requested (the system would auto-deny a permission it doesn't know)
	 */
	private fun isApplicable(permission: String) = permission != Manifest.permission.POST_NOTIFICATIONS || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}