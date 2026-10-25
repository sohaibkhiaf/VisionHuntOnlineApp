package com.example.visionhuntonline.util

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.visionhuntonline.R
import com.example.visionhuntonline.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServerHealthHelper(private val activity: Activity) {

    private var serverErrorDialog: AlertDialog? = null

    fun checkHealthAndShowDialog(scope: CoroutineScope, onHealthy: (() -> Unit)? = null) {
        if (!NetworkUtils.isNetworkConnected(activity)) return

        scope.launch {
            val result = ApiClient.checkHealth()
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    dismissServerErrorDialog()
                    onHealthy?.invoke()
                } else {
                    showServerErrorDialog(scope, onHealthy)
                }
            }
        }
    }

    fun showServerErrorDialog(scope: CoroutineScope, onHealthy: (() -> Unit)? = null) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (serverErrorDialog?.isShowing == true) return

        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_server_error, null)
        val btnRetry = dialogView.findViewById<View>(R.id.btn_retry_server)
        val btnClose = dialogView.findViewById<View>(R.id.btn_close_server)

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnRetry.setOnClickListener {
            btnRetry.isEnabled = false
            scope.launch {
                val result = ApiClient.checkHealth()
                withContext(Dispatchers.Main) {
                    btnRetry.isEnabled = true
                    if (result.isSuccess) {
                        dismissServerErrorDialog()
                        GameToast.show(activity, "تم الاتصال بالسيرفر بنجاح", Toast.LENGTH_SHORT)
                        onHealthy?.invoke()
                    } else {
                        GameToast.show(activity, "تعذر الاتصال بالسيرفر، يرجى المحاولة لاحقاً", Toast.LENGTH_SHORT)
                    }
                }
            }
        }

        btnClose?.setOnClickListener {
            dismissServerErrorDialog()
        }

        serverErrorDialog = dialog
        dialog.show()
    }

    fun dismissServerErrorDialog() {
        serverErrorDialog?.dismiss()
        serverErrorDialog = null
    }
}
