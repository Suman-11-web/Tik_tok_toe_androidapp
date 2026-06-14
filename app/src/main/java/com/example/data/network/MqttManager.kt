package com.example.data.network

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.util.UUID

class MqttManager(
    val clientId: String = "TikTokToe_" + UUID.randomUUID().toString().take(6),
    private val onMessageReceived: (type: String, senderId: String, data: JSONObject) -> Unit,
    private val onConnectionStateChanged: (connected: Boolean, error: String?) -> Unit
) {
    private var mqttClient: MqttClient? = null
    private var currentRoomCode: String? = null
    
    private val connectionLock = Any()
    @Volatile private var isConnecting = false
    
    private var currentBrokerUrl = BROKER_URL_PRIMARY
    private var usingAlternative = false
    private var connectionAttempts = 0

    companion object {
        private const val BROKER_URL_PRIMARY = "tcp://broker.emqx.io:1883"
        private const val BROKER_URL_SECONDARY = "tcp://broker.hivemq.com:1883"
        private const val TOPIC_PREFIX = "tiktoktoe_cecb93d3/rooms/"
        private const val TAG = "MqttManager"
        private const val MAX_CONNECTION_ATTEMPTS = 3
    }

    fun isConnected(): Boolean {
        return mqttClient?.isConnected == true
    }

    fun connect() {
        synchronized(connectionLock) {
            if (isConnected()) {
                Log.d(TAG, "Skipping connect: Currently connected")
                onConnectionStateChanged(true, null)
                return
            }
            if (isConnecting) {
                Log.d(TAG, "Skipping connect: Connection is actively in progress.")
                return
            }
            isConnecting = true
        }

        Thread {
            try {
                Log.d(TAG, "Connecting to broker: $currentBrokerUrl (clientId: $clientId)")
                val client = MqttClient(currentBrokerUrl, clientId, MemoryPersistence())
                mqttClient = client
                
                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 6
                    keepAliveInterval = 20
                    isAutomaticReconnect = true
                }

                client.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        Log.d(TAG, "MQTT Connection completed (reconnect: $reconnect, URL: $serverURI)")
                        synchronized(connectionLock) {
                            isConnecting = false
                        }
                        onConnectionStateChanged(true, null)
                        
                        // Critical logic: If cleanSession is true, automatic reconnect wipes subscription states.
                        // We must resubscribe the room topic programmatically on every connect or reconnect.
                        currentRoomCode?.let { room ->
                            Log.d(TAG, "Connection completed; automatically subscribing/renewing subscription to room $room")
                            subscribeToRoomInternal(room)
                        }
                    }

                    override fun connectionLost(cause: Throwable?) {
                        Log.e(TAG, "Connection lost: ${cause?.message}")
                        synchronized(connectionLock) {
                            isConnecting = false
                        }
                        onConnectionStateChanged(false, cause?.message)
                    }

                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        if (message == null) return
                        val payload = String(message.payload)
                        Log.d(TAG, "Received message: $payload on topic $topic")
                        try {
                            val json = JSONObject(payload)
                            val senderId = json.optString("senderId")
                            
                            // Ignore messages sent by ourselves
                            if (senderId != clientId) {
                                val type = json.optString("type")
                                onMessageReceived(type, senderId, json)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing message: ${e.message}")
                        }
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                client.connect(options)
                Log.d(TAG, "Connected successfully to $currentBrokerUrl")
                
                synchronized(connectionLock) {
                    isConnecting = false
                }
                connectionAttempts = 0 // Reset attempts on successful connection
                onConnectionStateChanged(true, null)

                // Critical safety fallback subscription: In case connectComplete is not triggered on initial sync connect
                currentRoomCode?.let { room ->
                    Log.d(TAG, "Explicitly subscribing to room '$room' after successful connect.")
                    subscribeToRoomInternal(room)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Connection error on $currentBrokerUrl: ${e.message}")
                synchronized(connectionLock) {
                    isConnecting = false
                }
                
                if (connectionAttempts < MAX_CONNECTION_ATTEMPTS) {
                    connectionAttempts++
                    val delayMs = 1500L * connectionAttempts
                    Log.w(TAG, "Retrying connection (attempt $connectionAttempts of $MAX_CONNECTION_ATTEMPTS) in ${delayMs}ms...")
                    
                    // Failover toggle between brokers
                    if (!usingAlternative) {
                        usingAlternative = true
                        currentBrokerUrl = BROKER_URL_SECONDARY
                    } else {
                        usingAlternative = false
                        currentBrokerUrl = BROKER_URL_PRIMARY
                    }
                    
                    Thread {
                        try { Thread.sleep(delayMs) } catch (ignored: Exception) {}
                        connect()
                    }.start()
                } else {
                    // Out of retries, notify of failure and reset to primary for future manual tries
                    connectionAttempts = 0
                    usingAlternative = false
                    currentBrokerUrl = BROKER_URL_PRIMARY
                    onConnectionStateChanged(false, e.message)
                }
            }
        }.start()
    }

    fun subscribeToRoom(roomCode: String) {
        currentRoomCode = roomCode
        if (!isConnected()) {
            Log.w(TAG, "Cannot subscribe immediately - currently offline. Stashed roomCode $roomCode for auto-subscription when connected.")
            // Connect to make sure we make progress
            connect()
            return
        }
        subscribeToRoomInternal(roomCode)
    }

    private fun subscribeToRoomInternal(roomCode: String) {
        Thread {
            try {
                val topic = "$TOPIC_PREFIX$roomCode"
                mqttClient?.subscribe(topic, 1) // QOS 1
                Log.d(TAG, "Subscribed successfully to topic: $topic")
            } catch (e: Exception) {
                Log.e(TAG, "Subscription error: ${e.message}")
            }
        }.start()
    }

    fun unsubscribeFromCurrentRoom() {
        val roomCode = currentRoomCode ?: return
        if (!isConnected()) return

        Thread {
            try {
                val topic = "$TOPIC_PREFIX$roomCode"
                mqttClient?.unsubscribe(topic)
                Log.d(TAG, "Unsubscribed from $topic")
                currentRoomCode = null
            } catch (e: Exception) {
                Log.e(TAG, "Unsubscribe error: ${e.message}")
            }
        }.start()
    }

    fun sendMessage(type: String, builder: JSONObject.() -> Unit) {
        val roomCode = currentRoomCode ?: return
        if (!isConnected()) {
            Log.w(TAG, "Cannot send message '$type': Not connected to any broker.")
            return
        }

        Thread {
            try {
                val json = JSONObject().apply {
                    put("type", type)
                    put("senderId", clientId)
                    builder()
                }
                val topic = "$TOPIC_PREFIX$roomCode"
                val message = MqttMessage(json.toString().toByteArray()).apply {
                    qos = 1
                }
                mqttClient?.publish(topic, message)
                Log.d(TAG, "Sent message: $json to topic $topic")
            } catch (e: Exception) {
                Log.e(TAG, "Error publishing message: ${e.message}")
            }
        }.start()
    }

    fun disconnect() {
        Thread {
            try {
                unsubscribeFromCurrentRoom()
                mqttClient?.disconnect()
                mqttClient?.close()
                mqttClient = null
                Log.d(TAG, "Disconnected successfully")
                onConnectionStateChanged(false, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting: ${e.message}")
            }
        }.start()
    }
}
