package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import androidx.core.content.ContextCompat

class IkonAdapter(private val context: Context, private val ikonLista: List<Int>) : BaseAdapter() {
    var kivalasztottPozicio = -1

    override fun getCount(): Int = ikonLista.size
    override fun getItem(position: Int): Any = ikonLista[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val kepNezet: ImageView = convertView as? ImageView ?: ImageView(context)
        kepNezet.layoutParams = ViewGroup.LayoutParams(100, 100)
        kepNezet.scaleType = ImageView.ScaleType.CENTER_CROP
        kepNezet.setImageResource(ikonLista[position])
        if (position == kivalasztottPozicio) {
            kepNezet.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
        } else {
            kepNezet.setBackgroundColor(0)
        }
        return kepNezet
    }

    fun setSelectedPosition(position: Int) {
        kivalasztottPozicio = position
    }
}
