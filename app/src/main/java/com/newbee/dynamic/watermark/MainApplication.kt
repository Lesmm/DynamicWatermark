package com.newbee.dynamic.watermark

import com.newbee.dynamic_watermark.WaterMarkerManager
import com.newbee.dynamic_watermark.WatermarkAppLifeCycle


class MainApplication : android.app.Application() {
    override fun onCreate() {
        super.onCreate()

        WatermarkAppLifeCycle.instance.application = this

        // register watermark to activity
        WaterMarkerManager.registerActivity(MainActivity::class.java)

        // show the watermark
        MainActivity.refreshWatermark()
    }
}