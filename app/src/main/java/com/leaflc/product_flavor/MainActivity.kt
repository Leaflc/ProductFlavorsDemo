package com.leaflc.product_flavor

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val pm = packageManager
        val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val url = appInfo.metaData.getString("URL")
        var number = appInfo.metaData.getString("NUMBER")
        number = number!!.substring(0, number.length - 1)
        tv_url.text = "Url:$url"
        tv_number.text = "Number:$number"
    }
}
