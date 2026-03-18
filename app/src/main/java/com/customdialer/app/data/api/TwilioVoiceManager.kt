package com.customdialer.app.data.api

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.twilio.voice.*

class TwilioVoiceManager(private val context: Context) {

    companion object {
        private const val TAG = "TwilioVoice"
    }

    private var accessToken: String? = null
    private var activeCall: Call? = null
    private var callListener: CallEventListener? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    interface CallEventListener {
        fun onCallConnecting(callSid: String?)
        fun onCallRinging(callSid: String?)
        fun onCallConnected(callSid: String?)
        fun onCallDisconnected(callSid: String?, error: String?)
        fun onCallFailed(error: String?)
    }

    fun setCallEventListener(listener: CallEventListener?) {
        callListener = listener
    }

    fun updateToken(token: String) {
        this.accessToken = token
        Log.d(TAG, "Twilio access token updated")
    }

    fun hasMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun makeCall(toNumber: String): Boolean {
        val token = accessToken
        if (token == null) {
            Log.e(TAG, "No access token available")
            callListener?.onCallFailed("Not connected to voice service. Please wait...")
            return false
        }

        if (!hasMicPermission()) {
            Log.e(TAG, "Microphone permission not granted")
            callListener?.onCallFailed("Microphone permission required")
            return false
        }

        // Ensure we run on main thread - Twilio SDK requires it
        mainHandler.post {
            try {
                // Set audio mode for voice call
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false

                val params = HashMap<String, String>()
                params["To"] = toNumber

                val connectOptions = ConnectOptions.Builder(token)
                    .params(params)
                    .build()

                activeCall = Voice.connect(context, connectOptions, object : Call.Listener {
                    override fun onConnectFailure(call: Call, callException: CallException) {
                        Log.e(TAG, "Call connect failure: ${callException.errorCode} - ${callException.message}")
                        activeCall = null
                        resetAudio()
                        callListener?.onCallFailed("${callException.message} (${callException.errorCode})")
                    }

                    override fun onRinging(call: Call) {
                        Log.d(TAG, "Call ringing: ${call.sid}")
                        callListener?.onCallRinging(call.sid)
                    }

                    override fun onConnected(call: Call) {
                        Log.d(TAG, "Call connected: ${call.sid}")
                        callListener?.onCallConnected(call.sid)
                    }

                    override fun onReconnecting(call: Call, callException: CallException) {
                        Log.d(TAG, "Call reconnecting: ${callException.message}")
                    }

                    override fun onReconnected(call: Call) {
                        Log.d(TAG, "Call reconnected: ${call.sid}")
                    }

                    override fun onDisconnected(call: Call, callException: CallException?) {
                        Log.d(TAG, "Call disconnected: ${call.sid}, error: ${callException?.message}")
                        activeCall = null
                        resetAudio()
                        callListener?.onCallDisconnected(call.sid, callException?.message)
                    }
                })

                callListener?.onCallConnecting(null)
                Log.d(TAG, "Voice.connect() called successfully for $toNumber")

            } catch (e: Exception) {
                Log.e(TAG, "Exception during Voice.connect: ${e.message}", e)
                activeCall = null
                resetAudio()
                callListener?.onCallFailed("Call error: ${e.message}")
            }
        }

        return true
    }

    private fun resetAudio() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (_: Exception) { }
    }

    fun joinConference(conferenceName: String, token: String) {
        if (!hasMicPermission()) {
            callListener?.onCallFailed("Microphone permission required")
            return
        }

        mainHandler.post {
            try {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = false

                // Connect with conference name as the "To" parameter
                // The server's voice webhook will route this into the conference
                val params = HashMap<String, String>()
                params["To"] = "conference:$conferenceName"

                val connectOptions = ConnectOptions.Builder(token)
                    .params(params)
                    .build()

                activeCall = Voice.connect(context, connectOptions, object : Call.Listener {
                    override fun onConnectFailure(call: Call, callException: CallException) {
                        Log.e(TAG, "Conference join failure: ${callException.message}")
                        activeCall = null
                        resetAudio()
                        callListener?.onCallFailed(callException.message ?: "Failed to join")
                    }

                    override fun onRinging(call: Call) {
                        callListener?.onCallRinging(call.sid)
                    }

                    override fun onConnected(call: Call) {
                        Log.d(TAG, "Joined conference: ${call.sid}")
                        callListener?.onCallConnected(call.sid)
                    }

                    override fun onReconnecting(call: Call, callException: CallException) { }
                    override fun onReconnected(call: Call) { }

                    override fun onDisconnected(call: Call, callException: CallException?) {
                        activeCall = null
                        resetAudio()
                        callListener?.onCallDisconnected(call.sid, callException?.message)
                    }
                })

                callListener?.onCallConnecting(null)
            } catch (e: Exception) {
                Log.e(TAG, "Error joining conference: ${e.message}", e)
                resetAudio()
                callListener?.onCallFailed("Error: ${e.message}")
            }
        }
    }

    fun hangup() {
        mainHandler.post {
            try {
                activeCall?.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error hanging up: ${e.message}")
            }
            activeCall = null
            resetAudio()
        }
    }

    fun mute(muted: Boolean) {
        mainHandler.post {
            try { activeCall?.mute(muted) } catch (_: Exception) { }
        }
    }

    fun isMuted(): Boolean {
        return activeCall?.isMuted ?: false
    }

    fun sendDigits(digits: String) {
        mainHandler.post {
            try { activeCall?.sendDigits(digits) } catch (_: Exception) { }
        }
    }

    fun isOnCall(): Boolean {
        return activeCall != null
    }

    fun getActiveCallSid(): String? {
        return activeCall?.sid
    }

    fun toggleSpeaker(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = !audioManager.isSpeakerphoneOn
        return audioManager.isSpeakerphoneOn
    }

    fun destroy() {
        mainHandler.post {
            try { activeCall?.disconnect() } catch (_: Exception) { }
            activeCall = null
            resetAudio()
        }
        callListener = null
    }
}
