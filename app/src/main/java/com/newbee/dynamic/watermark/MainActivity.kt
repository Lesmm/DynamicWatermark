package com.newbee.dynamic.watermark

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.newbee.dynamic.watermark.databinding.ActivityMainBinding
import com.newbee.dynamic_watermark.WaterMarkerConfig
import com.newbee.dynamic_watermark.WaterMarkerConfigBuilder
import com.newbee.dynamic_watermark.WaterMarkerManager
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }

        binding.clickMeButton.setOnClickListener {
            refreshWatermark()
        }

        binding.clickHeButton.setOnClickListener {
            clearWatermark()
        }
    }

    companion object {
        fun clearWatermark() {
            WaterMarkerManager.clearWaterMarks()
        }

        fun refreshWatermark() {
            val configs: MutableList<WaterMarkerConfig> = mutableListOf()

            val visibleLabels = listOf("李白", SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date()))
            val invisibleLabels = listOf("ID: 80888232")

            val conf1 = WaterMarkerConfigBuilder.build(visibleLabels, -25, 14, Color.parseColor("#0D31456A"), 8, 4)
            configs.add(conf1)

            val conf2 = WaterMarkerConfigBuilder.build(invisibleLabels, 30, 25, Color.parseColor("#03AAAAAA"), 7, 4)
            configs.add(conf2)

            WaterMarkerManager.refreshWaterMarks(configs)
        }
    }

}
