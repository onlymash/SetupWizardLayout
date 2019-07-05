package com.meiji.setupwizardDemo

import android.content.Intent
import android.os.Bundle
import android.widget.ScrollView

import androidx.appcompat.app.AppCompatActivity

import com.android.setupwizardlib.SetupWizardLayout
import com.android.setupwizardlib.view.NavigationBar

class SecondActivity : AppCompatActivity() {

    private lateinit var layout: SetupWizardLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
        layout = findViewById(R.id.setup)

        layout.headerText = "Add your account"
        layout.setIllustrationAspectRatio(4f)

        val scrollView = layout.scrollView

        layout.navigationBar?.setNavigationBarListener(object :
            NavigationBar.NavigationBarListener {
            override fun onNavigateBack() {
                onBackPressed()
            }

            override fun onNavigateNext() {
                if (scrollView != null) {
                    if (scrollView.getChildAt(0).bottom <= scrollView.height + scrollView.scrollY) {
                        //scroll view is at bottom
                        startActivity(Intent(this@SecondActivity, ThirdActivity::class.java))
                    } else {
                        //scroll view is not at bottom
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN)

                    }
                }
            }
        })
    }
}
