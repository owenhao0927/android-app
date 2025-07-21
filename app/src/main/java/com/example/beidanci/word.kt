package com.example.beidanci

data class Word(
    val text: String?,              // 单词本体
    val phonetic: String?,          // 音标
    val partOfSpeech: String?,      // 词性
    val example: String?,           // 英文例句
    val translation: String?,       // 中文翻译
    val exampleTranslation: String?, // 例句翻译
    val otherForms: String?         // 其他词性
)