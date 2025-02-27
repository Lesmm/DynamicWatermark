package com.newbee.dynamic.watermark

import com.newbee.dynamic_watermark.WaterMarkerManager
import com.newbee.dynamic_watermark.WatermarkAppLifeCycle


class MainApplication : android.app.Application() {
    override fun onCreate() {
        super.onCreate()

        // register app lifecycle and check the 'High contrast text' setting changed or not
        WatermarkAppLifeCycle.instance.application = this
        WatermarkAppLifeCycle.instance.addOnAppEnterForegroundListener {
            // 是否需要针对 高对比度文字 刷新水印
            WaterMarkerManager.recreateWaterMarksIfNeeded(this)
        }

        // register watermark to activity
        WaterMarkerManager.registerActivity(MainActivity::class.java)
        // show the watermark
        MainActivity.refreshWatermark()
    }
}