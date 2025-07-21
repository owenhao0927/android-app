package com.example.beidanci

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

// ChatGPT API 数据类
data class ChatGptRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<ChatMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 1000,
    val temperature: Double = 0.7
)

data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

data class ChatGptResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage?
)

data class Choice(
    val index: Int,
    val message: ChatMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)

// API 接口
interface ChatGptApiService {
    @POST("chat/completions")
    @Headers("Content-Type: application/json")
    suspend fun generateWords(
        @retrofit2.http.Header("Authorization") authorization: String,
        @Body request: ChatGptRequest
    ): Response<ChatGptResponse>
}

// 生成的单词响应数据类
data class GeneratedWordsResponse(
    val words: List<WordWithRelated>
)

data class WordWithRelated(
    val word: String,
    val phonetic: String,
    val partOfSpeech: String,
    val definition: String,
    val example: String,
    val translation: String,
    val relatedWords: List<RelatedWord>
)

data class RelatedWord(
    val word: String,
    val relationship: String, // 关系类型：同义词、反义词、词根相关等
    val translation: String
) 