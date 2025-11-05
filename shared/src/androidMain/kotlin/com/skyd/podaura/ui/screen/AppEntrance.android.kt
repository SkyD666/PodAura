package com.skyd.podaura.ui.screen

import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.skyd.podaura.ext.safeLaunch

@Composable
internal actual fun PermissionChecker(onMainContent: @Composable (() -> Unit)) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        var permissionGranted by remember {
            mutableStateOf(Environment.isExternalStorageManager())
        }
        val permissionRequester = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { permissionGranted = Environment.isExternalStorageManager() }

        if (permissionGranted) {
            onMainContent()
        } else {
            RequestStoragePermissionScreen(
                shouldShowRationale = false,
                onPermissionRequest = {
                    permissionGranted = Environment.isExternalStorageManager()
                    if (!permissionGranted) {
                        permissionRequester.safeLaunch(
                            Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        )
                    }
                },
            )
        }
    } else {
        val storagePermissionState = rememberMultiplePermissionsState(
            mutableListOf<String>().apply {
                add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        )
        if (storagePermissionState.allPermissionsGranted) {
            onMainContent()
        } else {
            RequestStoragePermissionScreen(
                shouldShowRationale = storagePermissionState.shouldShowRationale,
                onPermissionRequest = {
                    storagePermissionState.launchMultiplePermissionRequest()
                },
            )
        }
    }
}