package com.meiji.setupwizardDemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.android.setupwizardlib.SetupWizardLayout
import com.android.setupwizardlib.view.NavigationBar

class FirstActivity : AppCompatActivity() {

    private lateinit var layout: SetupWizardLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)
        layout = findViewById(R.id.setup)

        layout.headerText = "Tap & Go"
        layout.setIllustration(resources.getDrawable(R.drawable.bg1, theme))
        layout.setIllustrationAspectRatio(4f)
        layout.navigationBar?.setNavigationBarListener(object :
            NavigationBar.NavigationBarListener {
            override fun onNavigateBack() {
                onBackPressed()
            }

            override fun onNavigateNext() {
                startActivity(Intent(this@FirstActivity, SecondActivity::class.java))
            }
        })
    }
}
