package com.nuvoton.thermalviewergl.utility

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.TextView

/**
 * Created by cchsu20 on 11/01/2018.
 */

class StrokedTextView : TextView {
    private var strokedText: TextView? = null

    constructor(context: Context) : super(context) {
        strokedText = TextView(context)
        init()
    }

    constructor(context: Context, attr: AttributeSet) : super(context, attr) {
        strokedText = TextView(context, attr)
        init()
    }

    fun init() {
        val textPaint = strokedText!!.paint
        textPaint.strokeWidth = 10f
        textPaint.style = Paint.Style.STROKE
        strokedText!!.setTextColor(-0x1)
        strokedText!!.gravity = gravity
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams) {
        super.setLayoutParams(params)
        strokedText!!.layoutParams = params
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val text = strokedText!!.text

        if (text == null || text != this.text) {
            strokedText!!.text = getText()
            this.postInvalidate()
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        strokedText!!.measure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        strokedText!!.layout(left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas) {
        strokedText!!.draw(canvas)
        super.onDraw(canvas)
    }
}
