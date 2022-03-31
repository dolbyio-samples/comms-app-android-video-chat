package com.dolbyio.android_audio_conference_call

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.dolbyio.android_audio_conference_call.databinding.ActivityMainBinding
import com.voxeet.VoxeetSDK
import com.voxeet.promise.Promise
import com.voxeet.promise.PromiseInOut
import com.voxeet.promise.solve.ErrorPromise
import com.voxeet.promise.solve.Solver
import com.voxeet.promise.solve.ThenPromise
import com.voxeet.promise.solve.ThenVoid
import com.voxeet.sdk.json.ParticipantInfo
import com.voxeet.sdk.json.internal.ParamsHolder
import com.voxeet.sdk.models.Conference
import com.voxeet.sdk.services.builders.ConferenceCreateOptions
import com.voxeet.sdk.services.builders.ConferenceJoinOptions

const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        throw IllegalStateException("<---- Remove this line and set your keys below to use this sample !!")
        VoxeetSDK.initialize("", "")

        val permissionGranted = ActivityCompat.checkSelfPermission(
            this@MainActivity,
            permissions[0]
        ) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) {
            ActivityCompat.requestPermissions(this@MainActivity, permissions, REQUEST_CODE)
        }

        initializeBtnCreateCall()
        initializeBtnEndCall()
    }

    private fun openSession(
        name: String,
        externalID: String = "",
        avatarURL: String = ""
    ): Promise<Boolean> {
        return VoxeetSDK.session().open(ParticipantInfo(name, externalID, avatarURL))
    }

    private fun createConferencePromise(
        conferenceName: String,
    ): PromiseInOut<Conference, Conference> {
        val paramsHolder = ParamsHolder()
        paramsHolder.setVideoCodec("VP8")
        paramsHolder.setAudioOnly(true)

        val conferenceCreateOptions = ConferenceCreateOptions.Builder()
            .setConferenceAlias(conferenceName)
            .setParamsHolder(paramsHolder)
            .build()

        val createPromise = VoxeetSDK.conference().create(conferenceCreateOptions)

        return joinCall(createPromise)
    }

    private fun joinCall(conferencePromise: Promise<Conference>): PromiseInOut<Conference, Conference> {
        val joinPromise = conferencePromise.then(ThenPromise<Conference, Conference> { conference ->
            val conferenceJoinOptions: ConferenceJoinOptions =
                ConferenceJoinOptions.Builder(conference).build()
            return@ThenPromise VoxeetSDK.conference().join(conferenceJoinOptions)
        })
        return joinPromise
    }

    private fun closeSession() {
        VoxeetSDK.session().close()
            .then { result: Boolean?, solver: Solver<Any?>? ->
                Toast.makeText(this@MainActivity, "closed session", Toast.LENGTH_SHORT).show()
                updateViews(true)
            }.error {
                Log.e("MainActivity", "Error with closing session")
            }
    }

    private fun error(): ErrorPromise {
        return ErrorPromise { error: Throwable ->
            Log.e("MainActivity", error.printStackTrace().toString())
            Toast.makeText(this@MainActivity, "Error with conference", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeBtnCreateCall() {
        binding.btnCreate.setOnClickListener {
            val podcastName = binding.etPodcastName.text.toString()
            if (podcastName.isNotEmpty()) {
                val session = openSession("Person 1")
                session.then { result: Boolean?, solver: Solver<Any?>? ->
                    Toast.makeText(this@MainActivity, "Opened session", Toast.LENGTH_SHORT).show()
                    createConferencePromise(
                        podcastName
                    ).then<Any>(ThenVoid { conference: Conference? ->
                        Toast.makeText(
                            this@MainActivity,
                            "${conference?.alias} conference started...",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateViews(false)
                    })
                        .error {
                            Log.e("MainActivity", "error creating a conference")
                            error()
                        }
                }.error(error())
            } else {
                Toast.makeText(this@MainActivity, "Podcast name can't be empty", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun initializeBtnEndCall() {
        binding.btnLeaveCall.setOnClickListener {
            VoxeetSDK.conference().leave()
                .then { result: Boolean?, solver: Solver<Any?>? ->
                    closeSession()
                    Toast.makeText(this@MainActivity, "left...", Toast.LENGTH_SHORT).show()
                }.error(error())
        }
    }

    private fun updateViews(enabled: Boolean) {
        binding.btnCreate.isEnabled = enabled
        binding.etPodcastName.isEnabled = enabled
        binding.btnLeaveCall.isEnabled = !enabled
        if (!enabled) {
            binding.btnLeaveCall.visibility = View.VISIBLE
        } else {
            binding.btnLeaveCall.visibility = View.INVISIBLE
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            permissionGranted = (grantResults[0] == PackageManager.PERMISSION_GRANTED)
        }
    }
}