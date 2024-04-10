package com.artf.chatapp.view

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.viewModels
import com.artf.chatapp.R
import com.artf.chatapp.utils.FileHelper
import com.artf.chatapp.view.chatRoom.AudioHelper
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
@AndroidEntryPoint

class DiagnoseDataActivity : AppCompatActivity() {


    @Inject
    lateinit var fileHelper: FileHelper
    lateinit var audioHelper: AudioHelper
    private val firebaseVm: FirebaseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_diagnose_data)

        audioHelper = AudioHelper(fileHelper, this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.diagnose_data_button_record).setOnTouchListener(onRecordButtonTouch())
    }

    private fun onRecordButtonTouch() = object : View.OnTouchListener {
        override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
            if (onMicButtonClick(motionEvent)) return true
            return true
        }
    }


    private fun onMicButtonClick(motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> audioHelper.startRecording()
            MotionEvent.ACTION_UP -> {
                audioHelper.stopRecording()
                val recordFileName = audioHelper.recordFileName ?: return false
                val recorderDuration = audioHelper.recorderDuration ?: 0
                if (recorderDuration > 1000) {
                    val storageRef = FirebaseStorage.getInstance().getReference()
                    val ref = storageRef.child("diagnose_records")

                    val file = Uri.fromFile(audioHelper.recordFileName?.let { File(it) })

                    val uploadTask = ref.child(file.lastPathSegment!!).putFile(file)

                    val urlTask = uploadTask.continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let {
                                throw it
                            }
                        }
                        ref.downloadUrl
                    }.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val downloadUri = task.result
                            Log.i("Upload URL", downloadUri.toString())
                        } else {
                        }
                    }
                }
            }
        }
        return true
    }

    private fun pushAudio(recordFileName: String, recorderDuration: Long) {

        firebaseVm.pushAudio(recordFileName, recorderDuration)
    }

}