package com.example.beidanci

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class DifficultyLevel(val displayName: String, val description: String) {
    ELEMENTARY("小学", "基础词汇，适合初学者"),
    MIDDLE_SCHOOL("中学", "常用词汇，日常交流"),
    HIGH_SCHOOL("高中", "进阶词汇，学术基础"),
    UNIVERSITY("大学", "高级词汇，专业表达")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var selectedDifficulty by remember { mutableStateOf(DifficultyLevel.HIGH_SCHOOL) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    // 加载当前设置
    LaunchedEffect(Unit) {
        try {
            selectedDifficulty = context.getDifficultyLevel()
            Log.d("SettingsScreen", "当前难度: ${selectedDifficulty.displayName}")
        } catch (e: Exception) {
            Log.e("SettingsScreen", "加载设置失败: ${e.message}", e)
        } finally {
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Text(
                    text = "英语难度设置",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "选择适合你的英语水平，这将影响生成单词的难度",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Column(Modifier.selectableGroup()) {
                    DifficultyLevel.values().forEach { difficulty ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .selectable(
                                    selected = (difficulty == selectedDifficulty),
                                    onClick = {
                                        selectedDifficulty = difficulty
                                        scope.launch {
                                            try {
                                                context.saveDifficultyLevel(difficulty)
                                                Log.d("SettingsScreen", "难度设置已保存: ${difficulty.displayName}")
                                            } catch (e: Exception) {
                                                Log.e("SettingsScreen", "保存设置失败: ${e.message}", e)
                                            }
                                        }
                                    },
                                    role = Role.RadioButton
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (difficulty == selectedDifficulty) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (difficulty == selectedDifficulty),
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = difficulty.displayName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = difficulty.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "💡 提示",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "• 选择合适的难度有助于更好的学习效果\n• 可以随时调整难度设置\n• 新的难度设置将在下次刷新单词时生效",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// 扩展函数：保存难度设置
suspend fun Context.saveDifficultyLevel(difficulty: DifficultyLevel) {
    val difficultyKey = stringPreferencesKey("difficulty_level")
    dataStore.edit { preferences ->
        preferences[difficultyKey] = difficulty.name
    }
}

// 扩展函数：获取难度设置
suspend fun Context.getDifficultyLevel(): DifficultyLevel {
    val difficultyKey = stringPreferencesKey("difficulty_level")
    val preferences = dataStore.data.first()
    val difficultyName = preferences[difficultyKey] ?: DifficultyLevel.HIGH_SCHOOL.name
    return try {
        DifficultyLevel.valueOf(difficultyName)
    } catch (e: Exception) {
        DifficultyLevel.HIGH_SCHOOL
    }
} 