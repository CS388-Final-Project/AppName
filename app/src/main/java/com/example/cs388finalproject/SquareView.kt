package com.example.cs388finalproject

import android.content.Context
import android.util.AttributeSet
import android.view.View

class SquareView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = measuredWidth

        setMeasuredDimension(width, width)
    }
}