package com.example.beidanci

import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailScreen(
    word: Word,
    onRelatedWordClick: ((String) -> Unit)? = null
) {
    Log.d("WordDetailScreen", "WordDetailScreen 开始初始化，单词: ${word.text}")
    Log.d("WordDetailScreen", "单词完整数据: text=${word.text}, phonetic=${word.phonetic}, partOfSpeech=${word.partOfSpeech}")
    Log.d("WordDetailScreen", "单词翻译和例句: translation=${word.translation}, example=${word.example}")
    Log.d("WordDetailScreen", "例句翻译: exampleTranslation=${word.exampleTranslation}")
    Log.d("WordDetailScreen", "其他词性: otherForms=${word.otherForms}")
    
    val context = LocalContext.current
    Log.d("WordDetailScreen", "获取到 Context")
    val ttsRef = remember { mutableStateOf<TextToSpeech?>(null) }
    var relatedWords by remember { mutableStateOf<List<RelatedWord>>(emptyList()) }
    var isLoadingRelated by remember { mutableStateOf(false) }
    Log.d("WordDetailScreen", "TTS 引用初始化完成")

    LaunchedEffect(Unit) {
        try {
            val newTts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    Log.d("TTS", "TTS 初始化成功")
                } else {
                    Log.e("TTS", "TTS 初始化失败，状态码: $status")
                }
            }
            ttsRef.value = newTts
            // 设置语言在初始化后
            newTts.language = Locale.US
            Log.d("TTS", "ttsRef 设置完成")
        } catch (e: Exception) {
            Log.e("TTS", "TTS 初始化时抛出异常: ${e.message}")
        }
    }
    
    // 获取关联单词
    LaunchedEffect(word.text) {
        if (!word.text.isNullOrEmpty()) {
            isLoadingRelated = true
            try {
                val relatedWordsService = RelatedWordsService()
                relatedWords = relatedWordsService.getRelatedWords(word.text)
                Log.d("WordDetailScreen", "获取到 ${relatedWords.size} 个关联单词")
            } catch (e: Exception) {
                Log.e("WordDetailScreen", "获取关联单词失败: ${e.message}", e)
            } finally {
                isLoadingRelated = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsRef.value?.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("单词详情", fontSize = 20.sp)
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 单词标题
            item {
                Text(
                    text = word.text ?: "未知单词", 
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // 按要求的顺序显示信息
            // 1. 音标
            item {
                InfoCard(
                    title = "音标",
                    content = word.phonetic ?: "暂无音标",
                    icon = "🔊"
                ) {
                    val tts = ttsRef.value
                    val textToSpeak = word.text ?: "unknown"
                    if (tts != null) {
                        Log.d("TTS", "正在播放发音：$textToSpeak")
                        tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "tts_id")
                    } else {
                        Log.w("TTS", "TTS 尚未初始化")
                    }
                }
            }
            
            // 2. 词性
            item {
                InfoCard(
                    title = "词性",
                    content = word.partOfSpeech ?: "暂无词性",
                    icon = "📝"
                )
            }
            
            // 3. 单词意思
            item {
                InfoCard(
                    title = "释义",
                    content = word.translation ?: "暂无释义",
                    icon = "💡"
                )
            }
            
            // 4. 例句
            item {
                InfoCard(
                    title = "例句",
                    content = word.example ?: "暂无例句",
                    icon = "📖"
                )
            }
            
            // 5. 例句翻译
            if (!word.example.isNullOrEmpty()) {
                item {
                    InfoCard(
                        title = "例句翻译",
                        content = word.exampleTranslation ?: "暂无例句翻译",
                        icon = "🌏"
                    )
                }
            }
            
            // 6. 其他词性
            item {
                InfoCard(
                    title = "其他词性",
                    content = word.otherForms ?: "暂无其他词性信息",
                    icon = "📚"
                )
            }
            
            // 关联单词部分
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🔗 关联单词",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (isLoadingRelated) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (relatedWords.isNotEmpty()) {
                items(relatedWords) { relatedWord ->
                    RelatedWordCard(
                        relatedWord = relatedWord,
                        onClick = {
                            onRelatedWordClick?.invoke(relatedWord.word)
                        }
                    )
                }
            } else {
                item {
                    Text(
                        text = "暂无关联单词",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    content: String,
    icon: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { modifier ->
                if (onClick != null) {
                    modifier.clickable { onClick() }
                } else {
                    modifier
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            if (onClick != null) {
                Text(
                    text = "▶",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun RelatedWordCard(
    relatedWord: RelatedWord,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = relatedWord.word,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = relatedWord.translation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = relatedWord.relationship,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Text(
                text = "→",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}