package com.example.szemelyes_penzugyi_menedzser

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Betűméret alkalmazása, amikor az alkalmazás elindul
        val prefs = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val fontSize = prefs.getString("betumeret", "Közepes") ?: "Közepes"

        // Betűméret beállítása
        val size = when (fontSize) {
            "Kicsi" -> 12f
            "Nagy" -> 20f
            else -> 16f
        }

        // Alkalmazza az alkalmazás összes aktivitására
        applyFontSizeToAllViews(size)
    }

    // Betűméret beállítása minden TextView-ra
    fun applyFontSizeToAllViews(fontSize: Float) {
        val activity = (applicationContext as Context)
        val rootView = (activity as? ViewGroup) ?: return
        updateTextViewsFontSize(rootView, fontSize)
    }

    private fun updateTextViewsFontSize(view: View, fontSize: Float) {
        if (view is TextView) {
            view.textSize = fontSize
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                updateTextViewsFontSize(view.getChildAt(i), fontSize)
            }
        }
    }
}
