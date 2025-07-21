package com.example.beidanci

import android.app.Activity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import java.util.*

class TtsTestActivity : Activity() {
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val word = intent.getStringExtra("word") ?: ""

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                android.util.Log.d("TTS", "准备播放单词：$word")
                tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                Toast.makeText(this, "TTS 初始化失败", Toast.LENGTH_SHORT).show()
                android.util.Log.e("TTS", "TTS 初始化失败")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}