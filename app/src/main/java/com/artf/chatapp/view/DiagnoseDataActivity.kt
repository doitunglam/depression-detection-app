package com.artf.chatapp.view

import android.app.Activity
import android.content.Intent
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
import com.artf.chatapp.api.ApiClient
import com.artf.chatapp.data.model.DiagnoseResponse
import com.artf.chatapp.data.model.EvidenceResponse
import com.artf.chatapp.data.model.PostEvidenceRequest
import com.artf.chatapp.utils.FileHelper
import com.artf.chatapp.utils.states.NetworkState
import com.artf.chatapp.view.chatRoom.AudioHelper
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint

class DiagnoseDataActivity : AppCompatActivity() {
    private val apiClient = ApiClient()

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

        findViewById<ImageButton>(R.id.diagnose_data_button_record).setOnTouchListener(
            onRecordButtonTouch()
        )
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
                        .addOnSuccessListener { taskSnapshot ->
                            val urlTask = taskSnapshot.storage.downloadUrl
                            urlTask.addOnSuccessListener {
                                setResult(Activity.RESULT_OK)

                                val postEvidenceRequest = PostEvidenceRequest(it.toString());
                                apiClient.getApiService(this).postEvidence(postEvidenceRequest)
                                    .enqueue(object :
                                        Callback<EvidenceResponse> {
                                        override fun onFailure(
                                            call: Call<EvidenceResponse>,
                                            t: Throwable
                                        ) {
                                            finish()
                                        }

                                        override fun onResponse(
                                            call: Call<EvidenceResponse>,
                                            response: Response<EvidenceResponse>
                                        ) {
                                            firebaseVm.addStatus("Waiting");
                                            finish()
                                        }
                                    })
                            }.addOnFailureListener {
                                setResult(Activity.RESULT_CANCELED)
                                finish()
                            }
                        }.addOnFailureListener {
                        setResult(Activity.RESULT_CANCELED)
                        finish()
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