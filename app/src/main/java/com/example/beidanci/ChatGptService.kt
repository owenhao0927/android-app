package com.example.beidanci

import android.util.Log
import com.google.gson.Gson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ChatGptService {
    // 移除硬编码的API密钥，改为从BuildConfig读取
    private val apiKey = BuildConfig.OPENAI_API_KEY
    
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val apiService = retrofit.create(ChatGptApiService::class.java)
    
    suspend fun generateDailyWords(difficulty: DifficultyLevel = DifficultyLevel.HIGH_SCHOOL): List<Word> {
        return try {
            Log.d("ChatGptService", "开始调用ChatGPT API生成单词，难度: ${difficulty.displayName}")
            
            val prompt = createWordGenerationPrompt(difficulty)
            val request = ChatGptRequest(
                messages = listOf(
                    ChatMessage("system", "你是一个专业的英语教学助手，专门帮助中文用户学习英语单词。"),
                    ChatMessage("user", prompt)
                ),
                maxTokens = 1500,
                temperature = 0.8
            )
            
            val response = apiService.generateWords("Bearer $apiKey", request)
            
            if (response.isSuccessful) {
                val chatResponse = response.body()
                val content = chatResponse?.choices?.firstOrNull()?.message?.content
                
                Log.d("ChatGptService", "API响应: $content")
                
                if (content != null) {
                    parseWordsFromResponse(content)
                } else {
                    Log.e("ChatGptService", "API响应内容为空")
                    getFallbackWords()
                }
            } else {
                Log.e("ChatGptService", "API调用失败: ${response.code()} - ${response.message()}")
                getFallbackWords()
            }
        } catch (e: Exception) {
            Log.e("ChatGptService", "生成单词时发生异常: ${e.message}", e)
            getFallbackWords()
        }
    }
    
    suspend fun getWordDetails(wordText: String): Word? {
        return try {
            Log.d("ChatGptService", "开始获取单词详情: $wordText")
            
            val prompt = """
请为英语单词 "$wordText" 提供详细信息：

请严格按照以下JSON格式返回，不要添加任何其他文字：

{
  "words": [
    {
      "text": "$wordText",
      "phonetic": "[音标]",
      "partOfSpeech": "主要词性",
      "translation": "单词的中文翻译",
      "example": "英文例句",
      "exampleTranslation": "例句的中文翻译",
      "otherForms": "其他词性及释义（如：v. 动词释义；adj. 形容词释义）"
    }
  ]
}

重要说明：
- translation字段是单词本身的翻译（如：apple → 苹果，brave → 勇敢的）
- exampleTranslation字段是例句的翻译
- 这两个字段不能混淆！
- 提供准确的音标
- 给出主要词性和单词的中文翻译
- 提供一个实用的英文例句及其翻译
- 如果有其他常用词性，请列出
- 确保JSON格式正确
            """.trimIndent()
            
            val request = ChatGptRequest(
                messages = listOf(
                    ChatMessage("system", "你是一个专业的英语词典助手。"),
                    ChatMessage("user", prompt)
                ),
                maxTokens = 800,
                temperature = 0.3
            )
            
            val response = apiService.generateWords("Bearer $apiKey", request)
            
            if (response.isSuccessful) {
                val chatResponse = response.body()
                val content = chatResponse?.choices?.firstOrNull()?.message?.content
                
                Log.d("ChatGptService", "单词详情API响应: $content")
                
                if (content != null) {
                    val words = parseWordsFromResponse(content)
                    words.firstOrNull()
                } else {
                    Log.e("ChatGptService", "单词详情API响应内容为空")
                    null
                }
            } else {
                Log.e("ChatGptService", "单词详情API调用失败: ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ChatGptService", "获取单词详情时发生异常: ${e.message}", e)
            null
        }
    }
    
    private fun createWordGenerationPrompt(difficulty: DifficultyLevel): String {
        val difficultyDescription = when (difficulty) {
            DifficultyLevel.ELEMENTARY -> "小学水平的基础单词，如动物、颜色、数字、家庭成员、日常用品等简单词汇"
            DifficultyLevel.MIDDLE_SCHOOL -> "中学水平的常用单词，适合日常交流和基础阅读，包含常见动词、形容词等"
            DifficultyLevel.HIGH_SCHOOL -> "高中水平的进阶单词，包含学术词汇和复杂概念，适合深入学习"
            DifficultyLevel.UNIVERSITY -> "大学水平的高级单词，包含专业术语、抽象概念和学术表达"
        }
        
        return """
请为${difficulty.displayName}水平的英语学习者生成5个实用的英语单词，要求：

1. 单词难度：$difficultyDescription
2. 涵盖不同词性和主题
3. 每个单词需要包含：
   - 单词本身
   - 音标
   - 主要词性
   - 单词的中文翻译（如：brave → 勇敢的）
   - 英文例句
   - 例句的中文翻译
   - 其他词性（如果有的话）

请严格按照以下JSON格式返回，不要添加任何其他文字：

{
  "words": [
    {
      "text": "单词",
      "phonetic": "[音标]",
      "partOfSpeech": "主要词性",
      "translation": "单词的中文翻译",
      "example": "英文例句",
      "exampleTranslation": "例句的中文翻译",
      "otherForms": "其他词性及释义（如：v. 动词释义；adj. 形容词释义）"
    }
  ]
}

重要说明：
- translation字段是单词本身的翻译（如：apple → 苹果，brave → 勇敢的）
- exampleTranslation字段是例句的翻译
- 这两个字段不能混淆！
- 选择符合${difficulty.displayName}水平且有实际使用价值的单词
- 例句要自然实用，符合该难度水平
- 如果单词没有其他常用词性，otherForms可以为空字符串
- 确保JSON格式正确
        """.trimIndent()
    }
    
    private fun parseWordsFromResponse(content: String): List<Word> {
        return try {
            // 提取JSON部分
            val jsonStart = content.indexOf("{")
            val jsonEnd = content.lastIndexOf("}") + 1
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonContent = content.substring(jsonStart, jsonEnd)
                Log.d("ChatGptService", "提取的JSON: $jsonContent")
                
                val gson = Gson()
                val response = gson.fromJson(jsonContent, SimpleGeneratedWordsResponse::class.java)
                
                response.words.map { wordData ->
                    Word(
                        text = wordData.text,
                        phonetic = wordData.phonetic,
                        partOfSpeech = wordData.partOfSpeech,
                        example = wordData.example,
                        translation = wordData.translation,
                        exampleTranslation = wordData.exampleTranslation?.takeIf { it.isNotEmpty() } 
                            ?: if (wordData.example.isNotEmpty()) "${wordData.example}的中文翻译" else null,
                        otherForms = wordData.otherForms?.takeIf { it.isNotEmpty() } 
                            ?: "暂无其他词性信息"
                    )
                }
            } else {
                Log.e("ChatGptService", "无法找到有效的JSON格式")
                getFallbackWords()
            }
        } catch (e: Exception) {
            Log.e("ChatGptService", "解析单词数据失败: ${e.message}", e)
            getFallbackWords()
        }
    }
    
    private fun getFallbackWords(): List<Word> {
        Log.d("ChatGptService", "使用备用单词列表")
        return listOf(
            Word("perseverance", "[ˌpɜːrsəˈvɪrəns]", "n.", "Success requires perseverance and hard work.", "毅力；坚持不懈", "成功需要毅力和努力工作。", "v. persevere 坚持不懈"),
            Word("eloquent", "[ˈeləkwənt]", "adj.", "She gave an eloquent speech about climate change.", "雄辩的；口才好的", "她就气候变化发表了一次雄辩的演讲。", "n. eloquence 雄辩，口才"),
            Word("meticulous", "[məˈtɪkjələs]", "adj.", "He is meticulous about every detail in his work.", "细致的；一丝不苟的", "他对工作中的每个细节都很细致。", "adv. meticulously 细致地"),
            Word("versatile", "[ˈvɜːrsətl]", "adj.", "She is a versatile artist who works in many mediums.", "多才多艺的；通用的", "她是一位多才多艺的艺术家，涉猎多种媒介。", "n. versatility 多才多艺"),
            Word("profound", "[prəˈfaʊnd]", "adj.", "The book had a profound impact on my thinking.", "深刻的；深远的", "这本书对我的思维产生了深远的影响。", "adv. profoundly 深刻地")
        )
    }
}

// 简化的数据类，用于解析ChatGPT响应
private data class SimpleGeneratedWordsResponse(
    val words: List<SimpleGeneratedWord>
)

private data class SimpleGeneratedWord(
    val text: String,
    val phonetic: String,
    val partOfSpeech: String,
    val example: String,
    val translation: String,
    val exampleTranslation: String? = "",
    val otherForms: String? = ""
) 