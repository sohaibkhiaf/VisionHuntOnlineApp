package com.example.visionhuntonline.util

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.example.visionhuntonline.R

object GameToast {
    fun show(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        val inflater = LayoutInflater.from(context)
        val layout = inflater.inflate(R.layout.layout_3d_toast, null)
        
        val textView = layout.findViewById<TextView>(R.id.tv_toast_message)
        textView.text = message
        
        val toast = Toast(context.applicationContext)
        toast.duration = duration
        toast.view = layout
        toast.show()
    }
}