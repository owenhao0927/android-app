package com.example.beidanci

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.media.AudioManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.beidanci.ui.theme.*
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.NavController
import androidx.navigation.compose.*
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.example.beidanci.TtsTestActivity
import com.example.beidanci.PracticeActivity

val Context.dataStore by preferencesDataStore(name = "word_store")

suspend fun Context.clearAllWordCache() {
    try {
        Log.d("clearAllWordCache", "开始清除所有单词缓存")
        dataStore.edit { preferences ->
            // 清除所有以 "words_" 开头的缓存
            val keysToRemove = preferences.asMap().keys.filter { 
                it.name.startsWith("words_") 
            }
            keysToRemove.forEach { key ->
                preferences.remove(key)
            }
            // 清除最后一次单词日期
            preferences.remove(stringPreferencesKey("last_words_date"))
        }
        Log.d("clearAllWordCache", "缓存清除完成")
    } catch (e: Exception) {
        Log.e("clearAllWordCache", "清除缓存失败: ${e.message}", e)
    }
}

suspend fun Context.getTodayWords(): List<Word> {
    Log.d("getTodayWords", "开始获取今日单词")
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        
        // 首先尝试获取今天的单词
        val todayKey = stringPreferencesKey("words_$today")
        val prefs = dataStore.data.first()
        val todayJson = prefs[todayKey]
        
        if (todayJson != null) {
            Log.d("getTodayWords", "找到今日单词缓存")
            val wordArray = Gson().fromJson(todayJson, Array<Word>::class.java)
            return wordArray.toList()
        }
        
        // 如果今天没有单词，查找最近的单词记录
        val lastWordsKey = stringPreferencesKey("last_words_date")
        val lastWordsDate = prefs[lastWordsKey]
        
        if (lastWordsDate != null) {
            val lastWordsKey = stringPreferencesKey("words_$lastWordsDate")
            val lastWordsJson = prefs[lastWordsKey]
            if (lastWordsJson != null) {
                Log.d("getTodayWords", "使用最后一次的单词: $lastWordsDate")
                val wordArray = Gson().fromJson(lastWordsJson, Array<Word>::class.java)
                return wordArray.toList()
            }
        }
        
        // 如果都没有，生成默认单词
        Log.d("getTodayWords", "生成默认单词")
        val defaultWords = listOf(
            Word("explore", "[ɪkˈsplɔːr]", "v.", "I love to explore new ideas.", "探索；探讨", "我喜欢探索新想法。", "n. exploration 探索"),
            Word("creativity", "[ˌkriːeɪˈtɪvəti]", "n.", "Creativity helps us solve problems.", "创造力", "创造力帮助我们解决问题。", "adj. creative 创造性的"),
            Word("dedicate", "[ˈdedɪkeɪt]", "v.", "She dedicates her time to learning.", "献身；致力于", "她把时间投入到学习中。", "n. dedication 奉献"),
            Word("insight", "[ˈɪnsaɪt]", "n.", "This book gave me great insight.", "洞察力；深刻见解", "这本书让我受益匪浅。", "adj. insightful 有洞察力的"),
            Word("curious", "[ˈkjʊəriəs]", "adj.", "He is curious about how things work.", "好奇的", "他对事物的运作方式很好奇。", "n. curiosity 好奇心")
        )
        
        // 保存默认单词到今天的缓存
        saveWordRecord(today, defaultWords)
        return defaultWords
        
    } catch (e: Exception) {
        Log.e("getTodayWords", "获取今日单词发生异常: ${e.message}", e)
        throw e
    }
}

suspend fun Context.generateNewWords(): List<Word> {
    Log.d("generateNewWords", "开始生成新单词")
    return try {
        val difficulty = getDifficultyLevel()
        val chatGptService = ChatGptService()
        val newWords = chatGptService.generateDailyWords(difficulty)
        
        // 获取今天已有的单词并合并
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(Date())
        val existingWords = getTodayWordsFromCache(today)
        
        // 合并单词（避免重复）
        val allWords = mutableListOf<Word>()
        allWords.addAll(existingWords)
        
        // 添加新单词（检查重复）
        newWords.forEach { newWord ->
            val isDuplicate = allWords.any { existingWord ->
                existingWord.text?.lowercase() == newWord.text?.lowercase()
            }
            if (!isDuplicate) {
                allWords.add(newWord)
            }
        }
        
        // 保存合并后的单词到今天的缓存
        saveWordRecord(today, allWords)
        
        // 更新最后一次单词日期
        val lastWordsKey = stringPreferencesKey("last_words_date")
        dataStore.edit { preferences ->
            preferences[lastWordsKey] = today
        }
        
        Log.d("generateNewWords", "新单词已保存到缓存，总数: ${allWords.size}")
        Log.d("generateNewWords", "返回的单词列表: ${allWords.map { it.text }}")
        allWords
    } catch (e: Exception) {
        Log.e("generateNewWords", "生成新单词失败: ${e.message}", e)
        throw e
    }
}

// 获取今天缓存的单词
suspend fun Context.getTodayWordsFromCache(date: String): List<Word> {
    return try {
        val todayKey = stringPreferencesKey("words_$date")
        val prefs = dataStore.data.first()
        val todayJson = prefs[todayKey]
        
        if (todayJson != null) {
            val wordArray = Gson().fromJson(todayJson, Array<Word>::class.java)
            // 数据迁移：确保所有字段都存在
            wordArray.map { word ->
                Word(
                    text = word.text,
                    phonetic = word.phonetic,
                    partOfSpeech = word.partOfSpeech,
                    example = word.example,
                    translation = word.translation,
                    exampleTranslation = word.exampleTranslation ?: if (!word.example.isNullOrEmpty()) "暂无例句翻译" else null,
                    otherForms = word.otherForms ?: "暂无其他词性信息"
                )
            }.toList()
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        Log.e("getTodayWordsFromCache", "获取今日缓存单词失败: ${e.message}", e)
        emptyList()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("MainActivity", "onCreate 开始")
        super.onCreate(savedInstanceState)
        try {
            Log.d("MainActivity", "开始设置 Content")
            setContent {
                Log.d("MainActivity", "进入 setContent 块")
                MaterialTheme {
                    Log.d("MainActivity", "进入 MaterialTheme")
                    WordApp()
                }
            }
            Log.d("MainActivity", "onCreate 完成")
        } catch (e: Exception) {
            Log.e("MainActivity", "onCreate 发生异常: ${e.message}", e)
            throw e
        }
    }
}

@Composable
fun WordApp() {
    Log.d("WordApp", "WordApp 开始初始化")
    val navController = rememberNavController()
    Log.d("WordApp", "NavController 创建成功")
    
    val favoriteWords = remember { mutableStateListOf<Word>() }
    Log.d("WordApp", "FavoriteWords 初始化成功")

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                Log.d("WordApp", "进入 home 页面")
                HomePage(navController, favoriteWords)
            }
            composable("wordbank") {
                WordBankScreen { word ->
                    val wordJson = Uri.encode(Gson().toJson(word))
                    navController.navigate("detail/$wordJson")
                }
            }
            composable("favorites") {
                Log.d("WordApp", "进入 favorites 页面")
                FavoritesPage(favoriteWords)
            }
            composable("game") {
                GameSelectionScreen(navController)
            }
            composable("snake") {
                SnakeGameScreen()
            }
            composable("tetris") {
                TetrisGameScreen()
            }
            composable("settings") {
                SettingsScreen()
            }
            composable("detail/{word}") { backStackEntry ->
                Log.d("WordApp", "进入 detail 页面")
                val wordJson = backStackEntry.arguments?.getString("word")
                Log.d("WordApp", "获取到 wordJson: $wordJson")
                if (wordJson != null) {
                    val word = Gson().fromJson(wordJson, Word::class.java)
                    Log.d("WordApp", "Word 解析成功: ${word.text}")
                    WordDetailScreen(
                        word = word,
                        onRelatedWordClick = { relatedWordText ->
                            // 使用协程获取关联单词的完整信息
                            val scope = CoroutineScope(Dispatchers.Main)
                            scope.launch {
                                try {
                                    Log.d("WordApp", "点击关联单词: $relatedWordText")
                                    val chatGptService = ChatGptService()
                                    val relatedWord = chatGptService.getWordDetails(relatedWordText)
                                    
                                    if (relatedWord != null) {
                                        Log.d("WordApp", "获取到关联单词详情: ${relatedWord.text}")
                                        val relatedWordJson = Uri.encode(Gson().toJson(relatedWord))
                                        navController.navigate("detail/$relatedWordJson")
                                    } else {
                                        Log.w("WordApp", "无法获取关联单词详情，使用基础信息")
                                        // 如果API调用失败，创建基础的Word对象
                                        val basicWord = Word(
                                            text = relatedWordText,
                                            phonetic = "正在获取音标...",
                                            partOfSpeech = "正在获取词性...",
                                            translation = "正在获取释义...",
                                            example = "正在获取例句...",
                                            exampleTranslation = null,
                                            otherForms = null
                                        )
                                        val basicWordJson = Uri.encode(Gson().toJson(basicWord))
                                        navController.navigate("detail/$basicWordJson")
                                    }
                                } catch (e: Exception) {
                                    Log.e("WordApp", "获取关联单词详情失败: ${e.message}", e)
                                    // 异常时也创建基础的Word对象
                                    val basicWord = Word(
                                        text = relatedWordText,
                                        phonetic = "获取失败",
                                        partOfSpeech = "获取失败",
                                        translation = "获取失败",
                                        example = "获取失败",
                                        exampleTranslation = null,
                                        otherForms = null
                                    )
                                    val basicWordJson = Uri.encode(Gson().toJson(basicWord))
                                    navController.navigate("detail/$basicWordJson")
                                }
                            }
                        }
                    )
                } else {
                    Log.e("WordApp", "wordJson 为空")
                }
            }
        }
    }
    Log.d("WordApp", "WordApp 初始化完成")
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem("home", "首页", Icons.Default.Home),
        BottomNavItem("wordbank", "词库", Icons.Default.Star),
        BottomNavItem("favorites", "收藏", Icons.Default.Favorite),
        BottomNavItem("game", "游戏", Icons.Default.PlayArrow),
        BottomNavItem("settings", "设置", Icons.Default.Settings)
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 16.dp,
        color = androidx.compose.ui.graphics.Color.White
    ) {
        NavigationBar(
            containerColor = androidx.compose.ui.graphics.Color.White,
            contentColor = BlueAccent,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val iconColor = if (isSelected) BlueAccent else SecondaryText
                val backgroundColor = if (isSelected) BlueAccent.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.Transparent
                
                NavigationBarItem(
                    icon = { 
                        Box(
                            modifier = Modifier
                                .background(
                                    color = backgroundColor,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                item.icon, 
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    label = { 
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = iconColor
                        ) 
                    },
                    selected = isSelected,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BlueAccent,
                        selectedTextColor = BlueAccent,
                        unselectedIconColor = SecondaryText,
                        unselectedTextColor = SecondaryText,
                        indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(navController: NavController, favoriteWords: MutableList<Word>) {
    val context = LocalContext.current
    var todayWords by remember { mutableStateOf<List<Word>>(emptyList()) }
    var totalWordsCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    
    // 加载单词（只显示最新的5个）
    val loadWords = suspend {
        try {
            val allWords = context.getTodayWords()
            todayWords = allWords.takeLast(5) // 只显示最新的5个单词
            totalWordsCount = allWords.size
            Log.d("HomePage", "显示最新5个单词，总词库有 ${allWords.size} 个")
        } catch (e: Exception) {
            Log.e("HomePage", "获取今日单词失败: ${e.message}", e)
        }
    }
    
    // 生成新单词
    val generateNewWords = suspend {
        isLoading = true
        try {
            val allWords = context.generateNewWords()
            todayWords = allWords.takeLast(5) // 只显示最新的5个单词
            totalWordsCount = allWords.size
            refreshKey++ // 强制UI重新渲染
            Log.d("HomePage", "UI更新完成，显示最新5个单词，总词库有 ${allWords.size} 个")
        } catch (e: Exception) {
            Log.e("HomePage", "生成新单词失败: ${e.message}", e)
            // 即使失败也要重新加载当前单词
            try {
                val allWords = context.getTodayWords()
                todayWords = allWords.takeLast(5)
                totalWordsCount = allWords.size
                Log.d("HomePage", "重新加载最新5个单词，总词库有 ${allWords.size} 个")
            } catch (reloadException: Exception) {
                Log.e("HomePage", "重新加载单词也失败: ${reloadException.message}", reloadException)
            }
        } finally {
            isLoading = false
        }
    }
    
    LaunchedEffect(Unit) {
        loadWords()
    }

    Scaffold(
        topBar = { 
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BlueAccent,
                shadowElevation = 8.dp
            ) {
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                text = "📚 今日词汇",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = androidx.compose.ui.graphics.Color.White
                            )
                            if (totalWordsCount > 0) {
                                Text(
                                    text = "词库共 ${totalWordsCount} 个单词",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    actions = {
                        // 刷新按钮
                        Button(
                            onClick = {
                                scope.launch {
                                    generateNewWords()
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
                                contentColor = androidx.compose.ui.graphics.Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            if (isLoading) {
                                Text("生成中...", fontSize = 12.sp)
                            } else {
                                Text("🔄 刷新", fontSize = 12.sp)
                            }
                        }
                        
                        // 清除缓存按钮
                        Button(
                            onClick = {
                                scope.launch {
                                    context.clearAllWordCache()
                                    loadWords()
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedAccent.copy(alpha = 0.8f),
                                contentColor = androidx.compose.ui.graphics.Color.White
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("🗑️", fontSize = 12.sp)
                        }
                    }
                ) 
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    context.startActivity(Intent(context, PracticeActivity::class.java))
                },
                containerColor = GreenAccent,
                contentColor = androidx.compose.ui.graphics.Color.White,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .size(64.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "开始练习",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "练习",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BackgroundGradient1,
                            BackgroundGradient2
                        )
                    )
                )
        ) {
            if (todayWords.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
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
                        text = "暂无单词",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "点击刷新按钮生成新单词",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            } else if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = BlueAccent,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "正在加载单词...",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = SecondaryText
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                items(
                    items = todayWords,
                    key = { word -> word.text ?: System.currentTimeMillis() }
                ) { word ->
                    WordCard(
                        word = word,
                        onClick = {
                            val wordJson = Uri.encode(Gson().toJson(word))
                            navController.navigate("detail/$wordJson")
                        },
                        onFavorite = {
                            if (!favoriteWords.contains(word)) {
                                favoriteWords.add(word)
                            }
                        }
                    )
                }
                
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // 为FAB留出空间
                    }
                }
            }
        }
    }
}

@Composable
fun WordCard(
    word: Word,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardGradient1
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(CardGradient1, CardGradient2)
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // 单词主体
                        Text(
                            text = word.text ?: "未知单词",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = BlueAccent
                        )
                        
                        // 音标
                        if (!word.phonetic.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "/${word.phonetic}/",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 16.sp
                                ),
                                color = SecondaryText
                            )
                        }
                        
                        // 词性
                        if (!word.partOfSpeech.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = LightBlue.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = word.partOfSpeech!!,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = DeepBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    
                    // 右侧按钮组
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 发音按钮
                        IconButton(
                            onClick = { /* TODO: 添加TTS发音功能 */ },
                            modifier = Modifier
                                .background(
                                    color = GreenAccent.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "发音",
                                tint = GreenAccent
                            )
                        }
                        
                        // 收藏按钮
                        IconButton(
                            onClick = onFavorite,
                            modifier = Modifier
                                .background(
                                    color = RedAccent.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "收藏",
                                tint = RedAccent
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 翻译
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BackgroundGradient2.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = word.translation ?: "暂无翻译",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = PrimaryText,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                // 例句
                if (!word.example.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = OrangeAccent.copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "💡 例句",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = DeepOrange
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = word.example!!,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                ),
                                color = SecondaryText
                            )
                            // 例句翻译
                            if (!word.exampleTranslation.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = word.exampleTranslation!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesPage(favoriteWords: List<Word>) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("我的收藏") }) }
    ) { padding ->
        if (favoriteWords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💝",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无收藏",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "收藏一些单词开始学习吧",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                items(favoriteWords) { word ->
                    WordCard(
                        word = word,
                        onClick = { /* TODO: 导航到详情页 */ },
                        onFavorite = { /* 已收藏 */ }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSelectionScreen(navController: NavController) {
    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DeepOrange,
                shadowElevation = 8.dp
            ) {
                TopAppBar(
                    title = { 
                        Text(
                            text = "🎮 游戏中心",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = androidx.compose.ui.graphics.Color.White
                        ) 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            LightOrange.copy(alpha = 0.1f),
                            LightOrange.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "选择你喜欢的游戏",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = DeepOrange
                )
                
                // 贪吃蛇游戏卡片
                EnhancedGameCard(
                    title = "🐍 贪吃蛇",
                    description = "经典贪吃蛇游戏\n控制蛇吃食物，避免撞到边界和自己",
                    backgroundColor = GreenAccent,
                    onClick = { navController.navigate("snake") }
                )
                
                // 俄罗斯方块游戏卡片
                EnhancedGameCard(
                    title = "🧩 俄罗斯方块",
                    description = "经典俄罗斯方块游戏\n旋转和移动方块，消除完整行获得分数",
                    backgroundColor = BlueAccent,
                    onClick = { navController.navigate("tetris") }
                )
                
                // 新功能页面入口
                val context = LocalContext.current
                EnhancedGameCard(
                    title = "✨ 新功能体验",
                    description = "探索应用的新功能\n一个全新的页面等你来体验",
                    backgroundColor = Purple40,
                    onClick = { 
                        // TODO: 添加新功能页面
                    }
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "更多游戏敬请期待...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
        }
    }
}

@Composable
fun EnhancedGameCard(
    title: String,
    description: String,
    backgroundColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.1f),
                            backgroundColor.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = backgroundColor
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        lineHeight = 20.sp
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = backgroundColor,
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "开始游戏",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

