package com.example.szemelyes_penzugyi_menedzser

import android.content.Context
import android.util.AttributeSet
import android.widget.GridView

class HozzaadasGorgetesGridView(context: Context, attrs: AttributeSet) : GridView(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Korlátlan magasság meghatározása az összes elem megjelenítéséhez
        val expandSpec = MeasureSpec.makeMeasureSpec(Int.MAX_VALUE shr 2, MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, expandSpec)
    }
}
