package com.cube.sprintzerotemplate.lib.util

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Tests for [PermissionsHelper] using a MockK'd Fragment host: the launcher registration is
 * intercepted so the system dialog result can be simulated by invoking the captured callback.
 * Robolectric only supplies [android.os.Build.VERSION.SDK_INT] (for the POST_NOTIFICATIONS gate)
 */
@RunWith(AndroidJUnit4::class)
class PermissionsHelperTest {
	private val fragment = mockk<Fragment>()
	private val activity = mockk<FragmentActivity>()
	private val launcher = mockk<ActivityResultLauncher<Array<String>>>()
	private val resultCallback = slot<ActivityResultCallback<Map<String, Boolean>>>()
	private val launched = mutableListOf<Array<String>>()
	private val results = mutableListOf<PermissionsHelper.Result>()

	private lateinit var helper: PermissionsHelper

	@Before
	fun setUp() {
		mockkStatic(ContextCompat::class)
		mockkStatic(ActivityCompat::class)
		every { fragment.registerForActivityResult(any<ActivityResultContract<Array<String>, Map<String, Boolean>>>(), capture(resultCallback)) } returns launcher
		every { fragment.requireActivity() } returns activity
		every { launcher.launch(capture(launched)) } returns Unit

		helper = PermissionsHelper(fragment) { results.add(it) }
	}

	@After
	fun tearDown() {
		unmockkAll()
	}

	@Test
	fun checkPermissions_allAlreadyGranted_reportsImmediatelyWithoutLaunching() {
		grant(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

		helper.checkPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

		verify(exactly = 0) { launcher.launch(any()) }
		assertEquals(1, results.size)
		assertTrue(results.first().allGranted)
		assertEquals(listOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), results.first().granted)
	}

	@Test
	fun checkPermissions_someMissing_launchesOnlyTheMissingOnes() {
		grant(Manifest.permission.CAMERA)
		deny(Manifest.permission.RECORD_AUDIO)

		helper.checkPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

		assertEquals(listOf(Manifest.permission.RECORD_AUDIO), launched.single().toList())
		assertTrue(results.isEmpty())
	}

	@Test
	fun result_mixedGrant_mergesPreGrantedIntoGrantedAndReportsDenied() {
		grant(Manifest.permission.CAMERA)
		deny(Manifest.permission.RECORD_AUDIO)
		rationale(Manifest.permission.RECORD_AUDIO, show = true)
		helper.checkPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

		resultCallback.captured.onActivityResult(mapOf(Manifest.permission.RECORD_AUDIO to false))

		val result = results.single()
		assertFalse(result.allGranted)
		assertEquals(listOf(Manifest.permission.CAMERA), result.granted)
		assertEquals(listOf(Manifest.permission.RECORD_AUDIO), result.denied)
		assertTrue(result.permanentlyDenied.isEmpty())
	}

	@Test
	fun result_deniedWithoutRationale_flaggedAsPermanentlyDenied() {
		deny(Manifest.permission.CAMERA)
		rationale(Manifest.permission.CAMERA, show = false)
		helper.checkPermissions(Manifest.permission.CAMERA)

		resultCallback.captured.onActivityResult(mapOf(Manifest.permission.CAMERA to false))

		val result = results.single()
		assertEquals(listOf(Manifest.permission.CAMERA), result.denied)
		assertEquals(listOf(Manifest.permission.CAMERA), result.permanentlyDenied)
	}

	@Test
	fun result_grantedFromDialog_reportsAllGranted() {
		deny(Manifest.permission.CAMERA)
		helper.checkPermissions(Manifest.permission.CAMERA)

		resultCallback.captured.onActivityResult(mapOf(Manifest.permission.CAMERA to true))

		assertTrue(results.single().allGranted)
	}

	@Test
	@Config(sdk = [32])
	fun checkPermissions_postNotificationsBelowApi33_treatedAsGrantedWithoutRequesting() {
		helper.checkPermissions(Manifest.permission.POST_NOTIFICATIONS)

		verify(exactly = 0) { launcher.launch(any()) }
		assertTrue(results.single().allGranted)
	}

	@Test
	fun checkPermissions_postNotificationsOnApi33Plus_requestedNormally() {
		deny(Manifest.permission.POST_NOTIFICATIONS)

		helper.checkPermissions(Manifest.permission.POST_NOTIFICATIONS)

		assertEquals(listOf(Manifest.permission.POST_NOTIFICATIONS), launched.single().toList())
	}

	private fun grant(vararg permissions: String) = permissions.forEach {
		every { ContextCompat.checkSelfPermission(activity, it) } returns PackageManager.PERMISSION_GRANTED
	}

	private fun deny(vararg permissions: String) = permissions.forEach {
		every { ContextCompat.checkSelfPermission(activity, it) } returns PackageManager.PERMISSION_DENIED
	}

	private fun rationale(permission: String, show: Boolean) {
		every { ActivityCompat.shouldShowRequestPermissionRationale(activity, permission) } returns show
	}
}