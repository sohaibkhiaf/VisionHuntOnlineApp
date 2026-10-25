package com.example.visionhuntonline.util

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.visionhuntonline.R
import kotlinx.coroutines.launch

class NetworkDialogHelper(private val activity: Activity) {

    private var noInternetDialog: AlertDialog? = null

    fun monitorNetwork(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                NetworkUtils.observeNetworkStatus(activity).collect { isConnected ->
                    if (!isConnected) {
                        showNoInternetDialog()
                    } else {
                        dismissNoInternetDialog()
                    }
                }
            }
        }
    }

    fun checkAndShowDialog(): Boolean {
        val isConnected = NetworkUtils.isNetworkConnected(activity)
        if (!isConnected) {
            showNoInternetDialog()
        }
        return isConnected
    }

    fun showNoInternetDialog() {
        if (activity.isFinishing || activity.isDestroyed) return
        if (noInternetDialog?.isShowing == true) return

        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_no_internet, null)
        val btnRetry = dialogView.findViewById<View>(R.id.btn_retry_connection)
        val btnClose = dialogView.findViewById<View>(R.id.btn_close_connection)

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnRetry.setOnClickListener {
            if (NetworkUtils.isNetworkConnected(activity)) {
                dismissNoInternetDialog()
            } else {
                GameToast.show(activity, "ما زال الاتصال بالإنترنت غير متوفر!", Toast.LENGTH_SHORT)
            }
        }

        btnClose?.setOnClickListener {
            dismissNoInternetDialog()
        }

        noInternetDialog = dialog
        dialog.show()
    }

    fun dismissNoInternetDialog() {
        noInternetDialog?.dismiss()
        noInternetDialog = null
    }
}
