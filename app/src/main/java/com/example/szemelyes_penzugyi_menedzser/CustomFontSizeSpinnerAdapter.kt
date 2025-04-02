package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.view.LayoutInflater
import android.util.TypedValue

class CustomFontSizeSpinnerAdapter(
    context: Context,
    private val resource: Int,
    private val items: List<String>
) : ArrayAdapter<String>(context, resource, items) {

    private val fontSize: Float

    init {
        val prefs = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)
        val font = prefs.getString("betumeret", "Közepes") ?: "Közepes"
        fontSize = when (font) {
            "Kicsi" -> 12f
            "Nagy" -> 20f
            else -> 16f
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = LayoutInflater.from(context).inflate(resource, parent, false)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = items[position]
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = LayoutInflater.from(context).inflate(resource, parent, false)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        textView.text = items[position]
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
        return view
    }
}
