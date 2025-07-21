package com.example.beidanci

import android.util.Log

class RelatedWordsService {
    
    suspend fun getRelatedWords(word: String): List<RelatedWord> {
        return try {
            Log.d("RelatedWordsService", "获取单词 $word 的关联词")
            
            // 这里可以实现真正的关联词获取逻辑
            // 现在返回一些示例数据
            getStaticRelatedWords(word)
        } catch (e: Exception) {
            Log.e("RelatedWordsService", "获取关联词失败: ${e.message}", e)
            emptyList()
        }
    }
    
    private fun getStaticRelatedWords(word: String): List<RelatedWord> {
        // 根据不同的单词返回不同的关联词
        return when (word.lowercase()) {
            "explore" -> listOf(
                RelatedWord("discovery", "同义词", "发现"),
                RelatedWord("adventure", "相关词", "冒险"),
                RelatedWord("investigate", "同义词", "调查")
            )
            "creativity" -> listOf(
                RelatedWord("innovation", "同义词", "创新"),
                RelatedWord("imagination", "相关词", "想象力"),
                RelatedWord("artistic", "相关词", "艺术的")
            )
            "dedicate" -> listOf(
                RelatedWord("commit", "同义词", "承诺"),
                RelatedWord("devote", "同义词", "奉献"),
                RelatedWord("focus", "相关词", "专注")
            )
            "insight" -> listOf(
                RelatedWord("understanding", "同义词", "理解"),
                RelatedWord("wisdom", "相关词", "智慧"),
                RelatedWord("perception", "同义词", "感知")
            )
            "curious" -> listOf(
                RelatedWord("inquisitive", "同义词", "好奇的"),
                RelatedWord("interested", "相关词", "感兴趣的"),
                RelatedWord("wondering", "相关词", "想知道的")
            )
            else -> listOf(
                RelatedWord("related", "相关词", "相关的"),
                RelatedWord("similar", "相关词", "相似的"),
                RelatedWord("connected", "相关词", "连接的")
            )
        }
    }
} 