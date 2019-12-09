package com.wagnod.avatarimageview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import com.wagnod.avatarimageview.ui.AvatarImageView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val curID = 1

    private fun createViewDynamicly() {
        // Создадим нашу View программно
        val view = AvatarImageView(this).apply {
            // Но сейчас View опять не сохраняет состояние. Вспомним, что ей надо указать ID
//            id = View.generateViewId() // Но при пересоздании назначается новый ID (autoincrement)
            id = curID
            layoutParams = LinearLayout.LayoutParams(120, 120)
            setImageResource(R.drawable.ic_launcher_background)
        }
        container.addView(view)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        please, delete XML
//        createViewDynamicly()

        btn_border.setOnClickListener {
            aiv.setBorderWidth((2..10).random())
        }

        btn_color.setOnClickListener {
            aiv.setBorderColor((AvatarImageView.bgColor).random())
        }
    }
}