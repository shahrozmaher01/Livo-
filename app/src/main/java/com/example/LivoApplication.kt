package com.example

import android.app.Application
import com.example.data.audio.VoiceEngine
import com.example.data.local.LivoDatabase
import com.example.data.repository.LivoRepository

class LivoApplication : Application() {
  lateinit var database: LivoDatabase
    private set

  lateinit var voiceEngine: VoiceEngine
    private set

  lateinit var repository: LivoRepository
    private set

  override fun onCreate() {
    super.onCreate()
    database = LivoDatabase.getDatabase(this)
    voiceEngine = VoiceEngine(this)
    repository = LivoRepository(this, database, voiceEngine)
  }
}
