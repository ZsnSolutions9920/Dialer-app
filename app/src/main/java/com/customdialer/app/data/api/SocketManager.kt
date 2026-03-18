package com.customdialer.app.data.api

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class SocketManager {

    companion object {
        private const val TAG = "SocketManager"
    }

    private var socket: Socket? = null
    private var incomingCallListener: IncomingCallListener? = null

    interface IncomingCallListener {
        fun onIncomingCall(callSid: String, fromNumber: String, callerName: String?, conferenceName: String?)
        fun onCallMissed(callSid: String, fromNumber: String)
        fun onCallEnded()
    }

    fun setIncomingCallListener(listener: IncomingCallListener?) {
        incomingCallListener = listener
    }

    fun connect(baseUrl: String, token: String) {
        try {
            disconnect()

            val url = baseUrl.trimEnd('/')
            val options = IO.Options.builder()
                .setAuth(mapOf("token" to token))
                .setTransports(arrayOf("websocket", "polling"))
                .setReconnection(true)
                .setReconnectionAttempts(10)
                .setReconnectionDelay(2000)
                .build()

            socket = IO.socket(url, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Socket.IO connected")
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Socket.IO disconnected")
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = if (args.isNotEmpty()) args[0].toString() else "unknown"
                Log.e(TAG, "Socket.IO connect error: $error")
            }

            socket?.on("call:incoming") { args ->
                try {
                    if (args.isNotEmpty()) {
                        val data = args[0] as JSONObject
                        val callSid = data.optString("callSid", "")
                        val from = data.optString("from", "Unknown")
                        val callerName = data.optString("callerName", null)
                        val conferenceName = data.optString("conferenceName", null)
                        Log.d(TAG, "Incoming call: $callSid from $from ($callerName) conf=$conferenceName")
                        incomingCallListener?.onIncomingCall(callSid, from, callerName, conferenceName)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing incoming call: ${e.message}")
                }
            }

            socket?.on("call:missed") { args ->
                try {
                    if (args.isNotEmpty()) {
                        val data = args[0] as JSONObject
                        val callSid = data.optString("callSid", "")
                        val from = data.optString("from", "Unknown")
                        Log.d(TAG, "Missed call: $callSid from $from")
                        incomingCallListener?.onCallMissed(callSid, from)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing missed call: ${e.message}")
                }
            }

            socket?.on("call:ended") {
                Log.d(TAG, "Call ended event received")
                incomingCallListener?.onCallEnded()
            }

            // Also dismiss incoming on these events
            socket?.on("call:participant-left") {
                Log.d(TAG, "Participant left")
                incomingCallListener?.onCallEnded()
            }

            socket?.connect()
            Log.d(TAG, "Socket.IO connecting to $url")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect Socket.IO: ${e.message}", e)
        }
    }

    fun disconnect() {
        try {
            socket?.off()
            socket?.disconnect()
            socket = null
        } catch (_: Exception) { }
    }

    fun isConnected(): Boolean = socket?.connected() == true
}
