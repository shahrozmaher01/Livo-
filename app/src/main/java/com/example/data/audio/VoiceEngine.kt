package com.example.data.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.sqrt

enum class VoiceConnectionState {
  DISCONNECTED,
  CONNECTING,
  CONNECTED,
  RECONNECTING,
  ERROR
}

class VoiceEngine(private val context: Context) {

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
  private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  private val _connectionState = MutableStateFlow(VoiceConnectionState.DISCONNECTED)
  val connectionState: StateFlow<VoiceConnectionState> = _connectionState.asStateFlow()

  private val _isMuted = MutableStateFlow(true)
  val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

  private val _isSpeakerOn = MutableStateFlow(true)
  val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

  private val _speakingLevel = MutableStateFlow(0f)
  val speakingLevel: StateFlow<Float> = _speakingLevel.asStateFlow()

  private var audioRecord: AudioRecord? = null
  private var audioTrack: AudioTrack? = null
  private var recordingJob: Job? = null
  private var playbackJob: Job? = null

  private val sampleRate = 16000
  private val channelInConfig = AudioFormat.CHANNEL_IN_MONO
  private val channelOutConfig = AudioFormat.CHANNEL_OUT_MONO
  private val audioEncoding = AudioFormat.ENCODING_PCM_16BIT

  private var currentRoomId: String? = null

  @SuppressLint("MissingPermission")
  fun joinRoomVoice(roomId: String) {
    if (currentRoomId == roomId && _connectionState.value == VoiceConnectionState.CONNECTED) {
      return
    }
    leaveRoomVoice()
    currentRoomId = roomId
    _connectionState.value = VoiceConnectionState.CONNECTING

    engineScope.launch {
      try {
        setupAudioRouting()
        initAudioTrack()
        delay(300) // Simulating signaling handshake
        _connectionState.value = VoiceConnectionState.CONNECTED
        Log.i("VoiceEngine", "Joined voice room $roomId successfully")
      } catch (e: Exception) {
        Log.e("VoiceEngine", "Failed to join voice room: ${e.message}", e)
        _connectionState.value = VoiceConnectionState.ERROR
      }
    }
  }

  fun leaveRoomVoice() {
    stopRecording()
    stopPlayback()
    restoreAudioRouting()
    currentRoomId = null
    _isMuted.value = true
    _speakingLevel.value = 0f
    _connectionState.value = VoiceConnectionState.DISCONNECTED
  }

  fun setMuted(muted: Boolean) {
    _isMuted.value = muted
    if (muted) {
      stopRecording()
      _speakingLevel.value = 0f
    } else {
      startRecording()
    }
  }

  fun toggleSpeaker() {
    val newState = !_isSpeakerOn.value
    _isSpeakerOn.value = newState
    try {
      @Suppress("DEPRECATION")
      audioManager.isSpeakerphoneOn = newState
    } catch (e: Exception) {
      Log.w("VoiceEngine", "Failed to toggle speaker: ${e.message}")
    }
  }

  @SuppressLint("MissingPermission")
  private fun startRecording() {
    if (recordingJob?.isActive == true) return

    val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelInConfig, audioEncoding)
    val bufferSize = (minBufSize * 2).coerceAtLeast(1024)

    try {
      audioRecord = AudioRecord(
        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        sampleRate,
        channelInConfig,
        audioEncoding,
        bufferSize
      )

      if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
        Log.e("VoiceEngine", "AudioRecord initialization failed")
        _connectionState.value = VoiceConnectionState.ERROR
        return
      }

      audioRecord?.startRecording()

      recordingJob = engineScope.launch(Dispatchers.IO) {
        val buffer = ShortArray(512)
        while (isActive && !_isMuted.value) {
          val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0
          if (readCount > 0) {
            // Calculate real RMS amplitude for voice wave animation
            var sumSquares = 0.0
            for (i in 0 until readCount) {
              val sample = buffer[i]
              sumSquares += (sample * sample).toDouble()
            }
            val rms = sqrt(sumSquares / readCount)
            val normalizedLevel = (rms / 32768.0).toFloat().coerceIn(0f, 1f) * 3.5f
            _speakingLevel.value = normalizedLevel.coerceIn(0f, 1f)
          }
          delay(40) // 25 updates per second
        }
      }
    } catch (e: Exception) {
      Log.e("VoiceEngine", "Error starting recording: ${e.message}", e)
    }
  }

  private fun stopRecording() {
    recordingJob?.cancel()
    recordingJob = null
    try {
      audioRecord?.stop()
      audioRecord?.release()
    } catch (e: Exception) {
      // ignore
    }
    audioRecord = null
    _speakingLevel.value = 0f
  }

  private fun initAudioTrack() {
    val minBufSize = AudioTrack.getMinBufferSize(sampleRate, channelOutConfig, audioEncoding)
    val bufferSize = (minBufSize * 2).coerceAtLeast(1024)

    val audioAttributes = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
      .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
      .build()

    val audioFormat = AudioFormat.Builder()
      .setEncoding(audioEncoding)
      .setSampleRate(sampleRate)
      .setChannelMask(channelOutConfig)
      .build()

    audioTrack = AudioTrack(
      audioAttributes,
      audioFormat,
      bufferSize,
      AudioTrack.MODE_STREAM,
      AudioManager.AUDIO_SESSION_ID_GENERATE
    )

    if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
      audioTrack?.play()
    }
  }

  private fun stopPlayback() {
    playbackJob?.cancel()
    playbackJob = null
    try {
      audioTrack?.stop()
      audioTrack?.release()
    } catch (e: Exception) {
      // ignore
    }
    audioTrack = null
  }

  private fun setupAudioRouting() {
    try {
      audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
      @Suppress("DEPRECATION")
      audioManager.isSpeakerphoneOn = _isSpeakerOn.value
    } catch (e: Exception) {
      Log.w("VoiceEngine", "Setup audio routing error: ${e.message}")
    }
  }

  private fun restoreAudioRouting() {
    try {
      audioManager.mode = AudioManager.MODE_NORMAL
      @Suppress("DEPRECATION")
      audioManager.isSpeakerphoneOn = false
    } catch (e: Exception) {
      Log.w("VoiceEngine", "Restore audio routing error: ${e.message}")
    }
  }
}
