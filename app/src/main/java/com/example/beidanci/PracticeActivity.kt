package com.example.beidanci

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.beidanci.ui.theme.BeidanciTheme
import kotlinx.coroutines.launch
import kotlin.random.Random

class PracticeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BeidanciTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PracticeScreen()
                }
            }
        }
    }
}

data class PracticeWord(
    val word: Word,
    val maskedText: String,
    val correctAnswer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeScreen() {
    val context = LocalContext.current
    var practiceWords by remember { mutableStateOf<List<PracticeWord>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var userInput by remember { mutableStateOf("") }
    var isCorrect by remember { mutableStateOf<Boolean?>(null) }
    var score by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var showResult by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // 加载练习单词
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val allWords = context.getAllCachedWords()
                if (allWords.isNotEmpty()) {
                    practiceWords = allWords.shuffled().take(10).map { word ->
                        createPracticeWord(word)
                    }
                    Log.d("PracticeScreen", "加载了 ${practiceWords.size} 个练习单词")
                } else {
                    Log.w("PracticeScreen", "没有找到缓存的单词")
                }
            } catch (e: Exception) {
                Log.e("PracticeScreen", "加载练习单词失败: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("默写练习") 
                },
                actions = {
                    Text(
                        text = "得分: $score/${practiceWords.size}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
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
        } else if (practiceWords.isEmpty()) {
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
                        text = "📝",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "没有可练习的单词",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "请先学习一些单词",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (showResult) {
            // 显示最终结果
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
                        text = "🎉",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "练习完成！",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "最终得分: $score/${practiceWords.size}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            // 重新开始
                            currentIndex = 0
                            score = 0
                            showResult = false
                            userInput = ""
                            isCorrect = null
                            scope.launch {
                                val allWords = context.getAllCachedWords()
                                practiceWords = allWords.shuffled().take(10).map { word ->
                                    createPracticeWord(word)
                                }
                            }
                        }
                    ) {
                        Text("再来一次")
                    }
                }
            }
        } else {
            // 显示当前练习题
            val currentPractice = practiceWords[currentIndex]
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 进度指示器
                LinearProgressIndicator(
                    progress = (currentIndex + 1).toFloat() / practiceWords.size,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "第 ${currentIndex + 1} / ${practiceWords.size} 题",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 中文翻译
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = currentPractice.word.translation ?: "暂无翻译",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 填空题
                Text(
                    text = "请填写单词:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = currentPractice.maskedText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 输入框
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    label = { Text("输入答案") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            checkAnswer(
                                userInput = userInput,
                                correctAnswer = currentPractice.correctAnswer,
                                onResult = { correct ->
                                    isCorrect = correct
                                    if (correct) score++
                                }
                            )
                        }
                    ),
                    isError = isCorrect == false
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 结果显示
                isCorrect?.let { correct ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (correct) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = if (correct) "✅ 正确！" else "❌ 错误",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (correct) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onErrorContainer
                                }
                            )
                            if (!correct) {
                                Text(
                                    text = "正确答案: ${currentPractice.word.text}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isCorrect == null) {
                        Button(
                            onClick = {
                                checkAnswer(
                                    userInput = userInput,
                                    correctAnswer = currentPractice.correctAnswer,
                                    onResult = { correct ->
                                        isCorrect = correct
                                        if (correct) score++
                                    }
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("提交答案")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (currentIndex < practiceWords.size - 1) {
                                    currentIndex++
                                    userInput = ""
                                    isCorrect = null
                                } else {
                                    showResult = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (currentIndex < practiceWords.size - 1) "下一题" else "完成")
                        }
                    }
                }
            }
        }
    }
}

// 创建练习单词（随机遮挡字母）
fun createPracticeWord(word: Word): PracticeWord {
    val text = word.text ?: return PracticeWord(word, "???", "")
    val length = text.length
    val numToMask = (length * 0.3).toInt().coerceAtLeast(1).coerceAtMost(length - 1)
    
    val indices = (0 until length).shuffled().take(numToMask)
    val maskedText = text.mapIndexed { index, char ->
        if (indices.contains(index)) '_' else char
    }.joinToString("")
    
    return PracticeWord(
        word = word,
        maskedText = maskedText,
        correctAnswer = text
    )
}

// 检查答案
fun checkAnswer(
    userInput: String,
    correctAnswer: String,
    onResult: (Boolean) -> Unit
) {
    val isCorrect = userInput.trim().lowercase() == correctAnswer.lowercase()
    onResult(isCorrect)
}

// 扩展函数：获取所有缓存的单词
suspend fun android.content.Context.getAllCachedWords(): List<Word> {
    val allRecords = getAllWordRecords()
    return allRecords.flatMap { it.words }
}