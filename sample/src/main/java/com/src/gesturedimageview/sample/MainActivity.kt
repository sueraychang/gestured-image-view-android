package com.src.gesturedimageview.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.bumptech.glide.Glide
import com.src.gesturedimageview.GesturedImageView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gesturedImageView = findViewById<GesturedImageView>(R.id.gestured_image_view).apply {
            maxScale = 4f
            minScale = 1f

            setScrollToEndListener(object: GesturedImageView.ScrollToEndListener{
                override fun onScrollToHorizontalEnd(direction: Int) {
                    Log.d(TAG, "onScrollToHorizontalEnd $direction")
                }

                override fun onScrollToVerticalEnd(direction: Int) {
                    Log.d(TAG, "onScrollToVerticalEnd $direction")
                }
            })
        }

        Glide.with(this)
            .load("https://upload.wikimedia.org/wikipedia/commons/thumb/1/11/Test-Logo.svg/1280px-Test-Logo.svg.png")
            .fitCenter()
            .into(gesturedImageView)
    }

    companion object {
        private const val TAG = "[GesturedImageView]"
    }
}