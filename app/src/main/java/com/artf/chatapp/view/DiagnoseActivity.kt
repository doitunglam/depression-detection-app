package com.artf.chatapp.view

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.artf.chatapp.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DiagnoseActivity : AppCompatActivity() {

    private val firebaseVm: FirebaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_diagnose)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.diagnose_button_submit).setOnClickListener {
            val selected = findViewById<RadioGroup>(R.id.diagnose_radio_group).checkedRadioButtonId
            when (selected) {
                R.id.diagnose_radio_regular_user -> {
                    firebaseVm.addRole("Regular User");
                }
                R.id.diagnose_radio_expert -> {
                    firebaseVm.addRole("Expert");
                }
                R.id.diagnose_radio_volunteer -> {
                    firebaseVm.addRole("Volunteer");
                }
            }

            firebaseVm.roleUpdateState.observe(this, Observer {
                it?.let {
                    finish()
                }
            })
        }
    }
}