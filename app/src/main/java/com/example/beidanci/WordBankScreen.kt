package com.example.beidanci

import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

data class WordRecord(
    val date: String,
    val words: List<Word>,
    val displayDate: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordBankScreen(onWordClick: (Word) -> Unit) {
    val context = LocalContext.current
    var wordRecords by remember { mutableStateOf<List<WordRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    
    // 加载所有词库记录
    LaunchedEffect(Unit) {
        try {
            wordRecords = context.getAllWordRecords()
            Log.d("WordBankScreen", "加载了 ${wordRecords.size} 条词库记录")
        } catch (e: Exception) {
            Log.e("WordBankScreen", "加载词库失败: ${e.message}", e)
        } finally {
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("词库") },
                actions = {
                    IconButton(onClick = { /* TODO: 打开日历选择器 */ }) {
                        Icon(Icons.Default.DateRange, contentDescription = "选择日期")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (wordRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📚",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "词库为空",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "开始学习单词后，这里会显示你的学习记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "学习记录",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                items(wordRecords) { record ->
                    WordRecordCard(
                        record = record,
                        isExpanded = selectedDate == record.date,
                        onHeaderClick = {
                            selectedDate = if (selectedDate == record.date) null else record.date
                        },
                        onWordClick = onWordClick
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun WordRecordCard(
    record: WordRecord,
    isExpanded: Boolean,
    onHeaderClick: () -> Unit,
    onWordClick: (Word) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // 日期头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onHeaderClick() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = record.displayDate,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${record.words.size} 个单词",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (isExpanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // 展开的单词列表
            if (isExpanded) {
                Divider()
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    record.words.forEachIndexed { index, word ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onWordClick(word) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = word.text ?: "未知单词",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = word.translation ?: "暂无翻译",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = word.partOfSpeech ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (index < record.words.size - 1) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

// 扩展函数：获取所有词库记录
suspend fun Context.getAllWordRecords(): List<WordRecord> {
    val preferences = dataStore.data.first()
    val records = mutableListOf<WordRecord>()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displaySdf = SimpleDateFormat("MM月dd日 EEEE", Locale.getDefault())
    
    // 查找所有以 "words_" 开头的键
    preferences.asMap().forEach { (key, value) ->
        if (key.name.startsWith("words_") && value is String) {
            try {
                val date = key.name.removePrefix("words_")
                val wordArray = Gson().fromJson(value, Array<Word>::class.java)
                val parsedDate = sdf.parse(date)
                val displayDate = if (parsedDate != null) {
                    displaySdf.format(parsedDate)
                } else {
                    date
                }
                
                records.add(
                    WordRecord(
                        date = date,
                        words = wordArray.toList(),
                        displayDate = displayDate
                    )
                )
            } catch (e: Exception) {
                Log.e("WordBankScreen", "解析词库记录失败: ${e.message}", e)
            }
        }
    }
    
    // 按日期倒序排列
    return records.sortedByDescending { it.date }
}

// 扩展函数：保存词库记录（按日期）
suspend fun Context.saveWordRecord(date: String, words: List<Word>) {
    val wordKey = stringPreferencesKey("words_$date")
    val json = Gson().toJson(words)
    dataStore.edit { preferences ->
        preferences[wordKey] = json
    }
} 