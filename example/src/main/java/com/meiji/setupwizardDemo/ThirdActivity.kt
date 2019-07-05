package com.meiji.setupwizardDemo

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity

import com.android.setupwizardlib.SetupWizardListLayout
import com.android.setupwizardlib.view.NavigationBar

import java.util.Arrays

class ThirdActivity : AppCompatActivity() {

    private lateinit var layout: SetupWizardListLayout
    internal var integers = arrayOf(0, 1, 2, 3, 4, 5, 6, 7)
    internal var items = Arrays.asList(*integers)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_third)
        layout = findViewById(R.id.setup)

        val itemsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        layout.setAdapter(itemsAdapter)
        layout.headerText = "SetupWizardListLayout"
        layout.setIllustration(resources.getDrawable(R.drawable.bg2, theme))
        layout.setIllustrationAspectRatio(4f)
        layout.navigationBar?.setNavigationBarListener(object :
            NavigationBar.NavigationBarListener {
            override fun onNavigateBack() {
                onBackPressed()
            }

            override fun onNavigateNext() {
                startActivity(Intent(this@ThirdActivity, SecondActivity::class.java))
            }
        })
    }
}
