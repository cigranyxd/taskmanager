package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyFontSizeToCurrentActivity()
    }

    private fun applyFontSizeToCurrentActivity() {
        val prefs = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val fontSize = prefs.getString("betumeret", "Közepes") ?: "Közepes"
        val size = when (fontSize) {
            "Kicsi" -> 12f
            "Nagy" -> 20f
            else -> 18f
        }
        updateTextViewsFontSize(findViewById(android.R.id.content), size)
    }

    private fun updateTextViewsFontSize(view: View, fontSize: Float) {
        when (view) {
            is TextView -> view.textSize = fontSize
            is ViewGroup -> for (i in 0 until view.childCount) {
                updateTextViewsFontSize(view.getChildAt(i), fontSize)
            }
        }
    }
    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        applyFontSizeToCurrentActivity()
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        applyFontSizeToCurrentActivity()
    }
}