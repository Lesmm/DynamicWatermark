package com.newbee.dynamic.watermark.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout

class TestCaseFrameLayout : FrameLayout {

    constructor(context: Context) : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.i("TestCaseFrameLayout", "onDraw")
        TestCaseFrameLayout.painTexts2Canvas(canvas, listOf("3350-08-08 13:13:59", "杜甫", ""), -16, 160f)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        Log.i("TestCaseFrameLayout", "dispatchDraw")
        TestCaseFrameLayout.painTexts2Canvas(canvas, listOf("2024-12-24 08:12:22", "李白", ""), 16, 30f)
    }

    companion object {

        fun painTexts2Canvas(canvas: Canvas, texts: List<String>, degree: Int, offset: Float) {
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 50f
                isAntiAlias = true
            }

            canvas.save()
            canvas.translate(offset, offset)
            canvas.rotate(degree.toFloat(), canvas.width / 2f, canvas.height / 2f)
            for ((index, label) in texts.withIndex()) {
                canvas.drawText(label, 100f, 100f + index * 60f, paint)
            }
            canvas.restore()
        }
    }


}