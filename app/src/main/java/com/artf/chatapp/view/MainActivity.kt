package com.artf.chatapp.view

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.icu.lang.UCharacter.GraphemeClusterBreak.V
import android.media.MediaScannerConnection
import android.net.Uri
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ProgressBar
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.doOnAttach
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.artf.chatapp.R
import com.artf.chatapp.api.ApiClient
import com.artf.chatapp.data.model.DiagnoseResponse
import com.artf.chatapp.data.model.User
import com.artf.chatapp.data.source.firebase.FirebaseDaoImpl
import com.artf.chatapp.databinding.ActivityMainBinding
import com.artf.chatapp.utils.FileHelper
import com.artf.chatapp.utils.convertFromString
import com.artf.chatapp.utils.states.AuthenticationState
import com.artf.chatapp.utils.states.FragmentState
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val navigationManager by lazy { NavigationManager(this, binding) }
    private val firebaseVm: FirebaseViewModel by viewModels()

    private val apiClient = ApiClient()
    private var waitForResultFromSignIn = false

    private lateinit var builder: AlertDialog.Builder


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.root.doOnAttach { navigationManager.run { } }

        builder = AlertDialog.Builder(this);

        observeAuthState()
        observeFragmentState()
        observeUser()

        checkNotificationIntent()
        supportActionBar?.hide()
    }

    private fun observeAuthState() {
        firebaseVm.authenticationState.observe(this) {
            when (it) {
                is AuthenticationState.Authenticated -> onAuthenticated()
                is AuthenticationState.Unauthenticated -> onUnauthenticated()
                is AuthenticationState.InvalidAuthentication -> TODO()
            }
        }
    }

    private val _startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {

        }
    }
    private fun observeUser() {
        firebaseVm.user.observe(this) {
            if (it.first != null) {
                val role = it.first?.role.toString();
                if (role == "null") {
                    startActivity(Intent(this, DiagnoseActivity::class.java))
                }
                if (role == "Regular User") {
                    val status = it.first?.status.toString();
                    Log.i("USER", it.first.toString())

                    if (status == "null") {
                        _startForResult.launch(Intent(this, DiagnoseDataActivity::class.java))
                    }

                    if (status == "Waiting") {
                        findViewById<ProgressBar>(R.id.activity_main_progress).visibility = View.VISIBLE;
                        apiClient.getApiService(this).getDiagnose().enqueue(object : Callback<DiagnoseResponse> {
                            override fun onFailure(call: Call<DiagnoseResponse>, t: Throwable) {
                                Log.i("GET DIAGNOSE", "Oh no")
                                t.printStackTrace();
                                findViewById<ProgressBar>(R.id.activity_main_progress).visibility = View.INVISIBLE;

                            }

                            override fun onResponse(call: Call<DiagnoseResponse>, response: Response<DiagnoseResponse>) {
                                Log.i("GET DIAGNOSE", response.body()?.diagnose?.result.toString())

                                val result = response.body()?.diagnose?.result.toString();
                                var message : CharSequence;
                                if (Regex("sad|angry|fearful", RegexOption.IGNORE_CASE).containsMatchIn(result)) {
                                    message = "Hệ thống phát hiện bạn có thể bị trầm cảm"
                                    builder.setMessage(message)
                                    builder.setNeutralButton("Nhận tư vấn ngay") { dialog, id -> }
                                }
                                 else {
                                     message = "Hệ thống chưa nhận thấy bạn bị trầm cảm"
                                    builder.setMessage(message)
                                    builder.setPositiveButton("Mình vẫn muốn được tư vấn") { dialog, id -> }
                                }
                                builder.setTitle("Thông báo")
                                val dialog: AlertDialog = builder.create()
                                dialog.show()
                                firebaseVm.addStatus(response.body()?.diagnose?.result.toString())
                                findViewById<ProgressBar>(R.id.activity_main_progress).visibility = View.INVISIBLE;

                            }
                        })
                    }
                }
            }
        }
    }

    private fun observeFragmentState() {
        firebaseVm.fragmentState.observe(this, Observer {
            it?.let {
                if (!it.second) return@Observer
                navigationManager.onFragmentStateChange(it.first)
            }
        })
    }

    private fun onAuthenticated() {
        firebaseVm.onSignIn()
        supportActionBar?.show()
    }

    private fun onUnauthenticated() {
        if (waitForResultFromSignIn.not()) {
            firebaseVm.onSignOut()
            startSignInActivity()
            supportActionBar?.hide()
            waitForResultFromSignIn = true
        }
    }

    private fun checkNotificationIntent() {
        if (intent == null) return
        if (intent.hasExtra("userString")) {
            val user: User = convertFromString(intent.getStringExtra("userString")!!)
            firebaseVm.setReceiver(user)
            firebaseVm.setFragmentState(FragmentState.CHAT)
        }
    }

    @Deprecated("Deprecated in Java")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FirebaseDaoImpl.RC_SIGN_IN -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        supportActionBar?.show()
                    }

                    Activity.RESULT_CANCELED -> finish()
                }
                waitForResultFromSignIn = false
            }

            FirebaseDaoImpl.RC_PHOTO_PICKER -> {
                if (resultCode == Activity.RESULT_OK) {
                    val picUri = if (data != null) data.data!! else galleryAddPic()
                    picUri?.let { firebaseVm.pushPicture(picUri) }
                }
            }



        }
    }

    private fun galleryAddPic(): Uri? {
        val photoFile = File(FileHelper.currentPhotoPath)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(photoFile.extension)
        MediaScannerConnection.scanFile(
            this,
            arrayOf(photoFile.absolutePath),
            arrayOf(mimeType),
            null
        )
        return Uri.fromFile(photoFile)
    }

    private fun startSignInActivity() {
        val providers = mutableListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
//            AuthUI.IdpConfig.FacebookBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
//            AuthUI.IdpConfig.PhoneBuilder().build()
        )

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setTheme(R.style.LoginTheme)
                .setIsSmartLockEnabled(false)
                .setAvailableProviders(providers)
                .setLogo(R.drawable.ic_launcher)
                .build(), FirebaseDaoImpl.RC_SIGN_IN
        )
        firebaseVm.setFragmentState(FragmentState.START)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)

        val searchItem = menu.findItem(R.id.search)
        val searchView = searchItem.actionView as SearchView
        setSearchViewListener(searchView)
        return true
    }

    private fun setSearchViewListener(searchView: SearchView) {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty().not()) firebaseVm.onSearchTextChange(newText)
                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out_menu -> {
                AuthUI.getInstance().signOut(this)
                true
            }

            R.id.change_username -> {
                true
            }

            R.id.search -> {
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
