package com.newbee.dynamic_watermark

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.IntRange
import androidx.core.view.children
import java.lang.ref.WeakReference
import java.util.Objects

/**
 * WaterMaker Manager
 */
object WaterMarkerManager {

    /**
     * Watermark all activities or not
     */
    var isMarkAllActivity: Boolean = false

    /**
     * Watermark Included activity classes
     */
    private val includeActivityClasses = mutableSetOf<Class<out Activity>>()

    /**
     * Watermark Excluded activity classes
     */
    private val excludeActivityClasses = mutableSetOf<Class<out Activity>>()

    /**
     * Activities instances that have been watermarked, draw watermark on its decorView
     */
    private val markedActivities: MutableList<WeakReference<Activity>> = mutableListOf()

    /**
     * ViewGroup instances that have been draw watermark
     */
    private val markedViewGroups: MutableList<WeakReference<ViewGroup>> = mutableListOf()

    /**
     * Register activity class to be watermarked
     */
    fun <T : Activity> registerActivity(clazz: Class<T>) = includeActivityClasses.add(clazz)

    fun isRegistered(clazz: Class<out Activity>): Boolean = includeActivityClasses.contains(clazz)

    fun <T : Activity> addExcludeActivity(clazz: Class<T>) = excludeActivityClasses.add(clazz)

    fun isExcluded(clazz: Class<out Activity>): Boolean = excludeActivityClasses.contains(clazz)

    fun isContainsWaterMark(activity: Activity): Boolean = markedActivities.any { it.get() == activity }

    fun isContainsWaterMark(view: ViewGroup): Boolean = markedViewGroups.any { it.get() == view }

    /**
     * Check if high text contrast is enabled in the system settings - accessibility settings
     */
    private var isHighTextContrast: Boolean? = null

    fun isHighTextContrastEnabled(context: Context): Boolean {
        if (isHighTextContrast == null) {
            try {
                val retVal: Int = Settings.Secure.getInt(context.contentResolver, "high_text_contrast_enabled", 0)
                isHighTextContrast = Objects.equals(retVal, 1)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        return isHighTextContrast ?: false
    }

    /**
     * Current global paint water mark properties
     */
    val configs: List<WaterMarkerConfig> =
        listOf(WaterMarkerConfig(mutableListOf()), WaterMarkerConfig(mutableListOf()))

    /**
     * Clear all watermarks on all activities and all view groups
     */
    fun clearWaterMarks() {
        refreshWaterMarks(listOf(WaterMarkerConfig(mutableListOf()), WaterMarkerConfig(mutableListOf())))
    }

    /**
     * Refresh watermarks on all activities and all view groups, update the configs then repaint by called `invalidateSelf`
     */
    fun refreshWaterMarks(list: List<WaterMarkerConfig>) {
        for (i in list.indices) {
            val e = list.getOrNull(i) ?: break
            val config = configs.getOrNull(i) ?: break
            config.labels = e.labels
            config.degree = e.degree
            config.fontSize = e.fontSize
            config.paintColor = e.paintColor
            config.rowCount = e.rowCount
            config.columnCount = e.columnCount
        }

        fun refresh4View(viewGroup: ViewGroup?) {
            if (viewGroup == null) return
            // Because WaterMarker they shared the same configs instance
            val frameLayout: FrameLayout? = viewGroup.findViewById(R.id.water_mark_frame_layout)
            // val drawingView: View? = frameLayout?.findViewById(R.id.water_mark_drawing_view)
            frameLayout?.children?.forEach { drawingView ->
                val waterMarker: WaterMarker? = (drawingView as? TextView)?.background as? WaterMarker
                waterMarker?.isLastPaintWaterMarkerSuccess = null
                waterMarker?.invalidateSelf()
            }
        }

        // Refresh all marked activities
        val acts = markedActivities.toList()
        for (ref in acts) {
            val activity = ref.get()
            if (activity != null && !(activity.isFinishing) && !(activity.isDestroyed)) {
                refresh4View(activity.window.decorView as? ViewGroup)
            } else {
                markedActivities.remove(ref)
            }
        }
        // Refresh all marked view groups
        val groups = markedViewGroups.toList()
        for (ref in groups) {
            val viewGroup = ref.get()
            if (viewGroup != null) {
                refresh4View(viewGroup)
            } else {
                markedViewGroups.remove(ref)
            }
        }
    }

    /**
     * Apply watermarks to activity instance, always call this method after `onCreate` of activity or activity lifecycle callback
     */
    fun applyToActivity(activity: Activity) {
        if (!(isMarkAllActivity || isRegistered(activity::class.java))) {
            return
        }
        if (isExcluded(activity::class.java)) {
            return
        }
        createWaterMarkFrameLayout(activity, configs, activity.window.decorView as? ViewGroup)
        markedActivities.add(WeakReference(activity))
    }

    /**
     * Apply watermarks to all view group instance
     */
    fun applyToViewGroup(viewGroup: ViewGroup) {
        val context: Context = viewGroup.context
        createWaterMarkFrameLayout(context, configs, viewGroup)
        markedViewGroups.add(WeakReference(viewGroup))
    }

    /**
     * Create a water mark bitmap from a source bitmap
     */
    fun createWaterMarkBitmap(bitmap: Bitmap, rowGap: Float = 120f, columnGap: Float = 200f): Bitmap {
        val bitmapConfigs = configs.map {
            WaterMarkerConfig(
                it.labels,
                it.degree,
                it.fontSize,
                it.paintColor,
                it.rowCount,
                it.columnCount,
                rowGap,
                columnGap
            )
        }
        return WaterMarker.doPaint2Bitmap(bitmap, bitmapConfigs)
    }

    /**
     * create a frame layout, create a text view as its child to draw the water mark
     */
    private fun createWaterMarkFrameLayout(context: Context, configs: List<WaterMarkerConfig>, parent: ViewGroup?) {
        if (parent == null) return;
        if (parent.findViewById<View>(R.id.water_mark_frame_layout) != null) {
            return
        }

        // 1. Create a FrameLayout as the root layout
        val MATCH_PARENT: Int = FrameLayout.LayoutParams.MATCH_PARENT
        val frameLayout = FrameLayout(context)
        frameLayout.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        frameLayout.id = R.id.water_mark_frame_layout
        parent.addView(frameLayout)

        // 2. Create TextView(s) the add to the FrameLayout
        if (isHighTextContrastEnabled(context)) {
            // If high text contrast enabled by user, the text color (including alpha) will be not working
            for (i in configs.indices) {
                val config = configs[i]
                val drawingView = createWaterMarkTextView(context, listOf(config))
                drawingView.id = R.id.water_mark_drawing_view + i
                drawingView.alpha = 0.03f / (i + 1)
                frameLayout.addView(drawingView)
            }
        } else {
            val drawingView = createWaterMarkTextView(context, configs)
            drawingView.id = R.id.water_mark_drawing_view
            frameLayout.addView(drawingView)
        }
    }

    /**
     * Create a text view to draw the water mark
     */
    private fun createWaterMarkTextView(context: Context, configs: List<WaterMarkerConfig>): TextView {
        val MATCH_PARENT: Int = FrameLayout.LayoutParams.MATCH_PARENT
        val drawingView = TextView(context)
        drawingView.gravity = Gravity.CENTER
        drawingView.layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        drawingView.background = WaterMarker(context, configs)
        return drawingView
    }
}


/**
 * 水印配置构造
 *
 * @param labels   水印文字列表 多行显示支持
 * @param degree   水印角度
 * @param fontSize 水印文字大小
 * @param paintColor 水印文字颜色
 * @param rowCount 水印行数
 * @param columnCount 水印列数
 * @param rowGap 行间距
 * @param columnGap 列间距
 *
 * @constructor Create empty Water marker config
 *
 */

data class WaterMarkerConfig(
    var labels: List<String> = mutableListOf(),
    var degree: Int = -30,
    var fontSize: Int = 14,
    var paintColor: Int = 221332842, // "#0D31456A"
    var rowCount: Int = 8,
    var columnCount: Int = 3,
    var rowGap: Float? = null,
    var columnGap: Float? = null,
)

object WaterMarkerConfigBuilder {
    fun build(
        labels: List<String> = mutableListOf(),
        degree: Int = -30,
        fontSize: Int = 14,
        paintColor: Int = 221332842, // "#0D31456A"
        rowCount: Int = 8,
        columnCount: Int = 3,
        rowGap: Float? = null,
        columnGap: Float? = null,
    ): WaterMarkerConfig {
        return WaterMarkerConfig(
            labels,
            degree,
            fontSize,
            paintColor,
            rowCount,
            columnCount,
            rowGap,
            columnGap,
        )
    }
}

/**
 * A drawable to draw water mark on canvas
 */
open class WaterMarker(private val context: Context, open var configs: List<WaterMarkerConfig>) : Drawable() {

    var isLastPaintWaterMarkerSuccess: Boolean? = null
    var lastPaintCallback: ((Boolean) -> Unit)? = null

    private val paint = Paint()

    // Reference:
    // 0. https://stackoverflow.com/a/16752155
    // 1. https://stackoverflow.com/a/47711848
    override fun draw(canvas: Canvas) {
        val displayMetrics = context.resources.displayMetrics
        // val screenWidth = displayMetrics.widthPixels
        // val screenHeight = displayMetrics.heightPixels
        val canvasWidth = bounds.right
        val canvasHeight = bounds.bottom

        scaledDensity = displayMetrics.scaledDensity
        var isPaintSuccess = true
        for (config in configs) {
            val result = doPaint2Canvas(
                canvas,
                canvasWidth,
                canvasHeight,
                paint,
                config.labels,
                config.degree,
                config.fontSize,
                config.paintColor,
                config.rowCount,
                config.columnCount,
            )
            isPaintSuccess = isPaintSuccess && result
        }
        if (isLastPaintWaterMarkerSuccess == null) {
            // Post the result 'isPaintSuccess' to Biz ...
            lastPaintCallback?.invoke(isPaintSuccess)
        }
        isLastPaintWaterMarkerSuccess = isPaintSuccess
    }

    override fun setAlpha(@IntRange(from = 0, to = 255) alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.UNKNOWN", "android.graphics.PixelFormat"))
    override fun getOpacity(): Int = PixelFormat.UNKNOWN


    companion object {
        var scaledDensity: Float? = null

        private fun sp2px(context: Context?, spValue: Float): Int {
            if (scaledDensity == null && context != null) {
                scaledDensity = context.resources?.displayMetrics?.scaledDensity
            }
            val density = scaledDensity ?: 1f
            return (density * spValue + 0.5f).toInt()
        }

        private fun maxWithOfTexts(paint: Paint, texts: List<String?>): Float {
            var max = 0f
            for (text in texts) {
                val width = paint.measureText(text)
                if (width > max) {
                    max = width
                }
            }
            return kotlin.math.max(max, 1f)
        }

        private fun maxHeightOfTexts(paint: Paint, texts: List<String?>, lineGap: Int): Float {
            val fontMetrics = paint.fontMetrics
            val textHeight = fontMetrics.bottom - fontMetrics.top
            val textLineGap = texts.size * paint.fontSpacing + (texts.size - 1) * lineGap
            val max = textHeight + textLineGap
            return kotlin.math.max(max, 1f)
        }

        fun doPaint2Bitmap(src: Bitmap, configs: List<WaterMarkerConfig>): Bitmap {
            val canvasWidth = src.width
            val canvasHeight = src.height
            val result = Bitmap.createBitmap(canvasWidth, canvasHeight, src.config)

            val canvas = Canvas(result)
            canvas.drawBitmap(src, 0f, 0f, null)

            val paint = Paint()

            for (config in configs) {
                doPaint2Canvas(
                    canvas,
                    canvasWidth,
                    canvasHeight,
                    paint,
                    config.labels,
                    config.degree,
                    config.fontSize,
                    config.paintColor,
                    config.rowCount,
                    config.columnCount,
                    config.rowGap,
                    config.columnGap,
                )
            }
            return result
        }

        fun doPaint2Canvas(
            canvas: Canvas,
            canvasWidth: Int,
            canvasHeight: Int,
            paint: Paint,
            labels: List<String>,
            degree: Int,
            fontSize: Int,
            paintColor: Int,

            // extra
            rowCount: Int = 8,
            columnCount: Int = 3,

            rowGap: Float? = null,
            columnGap: Float? = null,
        ): Boolean {
            try {
                // if labels.size = 0, we clear the text on the canvas
                if (labels.isEmpty()) {
                    canvas.drawColor(Color.TRANSPARENT)
                    return true
                }

                var mRowCount = rowCount
                if (mRowCount <= 0) mRowCount = 8
                var mColumnCount = columnCount
                if (mColumnCount <= 0) mColumnCount = 3

                paint.color = paintColor
                paint.isAntiAlias = true
                paint.textSize = sp2px(null, fontSize.toFloat()).toFloat()

                val lineGap = 64

                val textWidth = maxWithOfTexts(paint, labels)
                val textHeight = maxHeightOfTexts(paint, labels, lineGap)

                val rowGapCount = mRowCount + 1
                var rowGapHeight = rowGap ?: ((canvasHeight - textHeight * mRowCount) / rowGapCount)
                rowGapHeight = kotlin.math.max(rowGapHeight, 1f)

                val columnGapCount = mColumnCount + 1
                var columnGapWidth = columnGap ?: ((canvasWidth - textWidth * mColumnCount) / columnGapCount)
                columnGapWidth = kotlin.math.max(columnGapWidth, 1f)

                canvas.drawColor(Color.TRANSPARENT)

                var positionY = rowGapHeight * 3
                while (positionY <= canvasHeight) {
                    var positionX = columnGapWidth
                    while (positionX < canvasWidth) {
                        canvas.save()
                        var y = positionY
                        canvas.rotate(degree.toFloat(), positionX, y)
                        for (label in labels) {
                            canvas.drawText(label, positionX, y, paint)
                            y += lineGap
                        }
                        canvas.restore()
                        positionX += textWidth + columnGapWidth
                    }
                    positionY += textHeight + rowGapHeight
                }
                return true
            } catch (e: Throwable) {
                e.printStackTrace()
                return false
            }
        }
    }
}