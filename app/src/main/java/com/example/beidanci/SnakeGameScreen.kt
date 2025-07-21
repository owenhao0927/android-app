package com.example.beidanci

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random
import androidx.compose.ui.graphics.vector.ImageVector

// 游戏状态
enum class GameState {
    READY, PLAYING, PAUSED, GAME_OVER
}

// 方向
enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

// 位置数据类
data class Position(val x: Int, val y: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnakeGameScreen() {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    var gameState by remember { mutableStateOf(GameState.READY) }
    var score by remember { mutableStateOf(0) }
    var highScore by remember { mutableStateOf(0) }
    var gameSpeed by remember { mutableStateOf(150L) }
    var difficulty by remember { mutableStateOf(1) }
    
    // 加载最高分
    LaunchedEffect(Unit) {
        context.dataStore.data.collect { preferences ->
            highScore = preferences[intPreferencesKey("snake_high_score")] ?: 0
        }
    }
    
    // 游戏参数
    val boardSize = 20
    
    // 蛇的状态
    var snake by remember { mutableStateOf(listOf(Position(10, 10))) }
    var direction by remember { mutableStateOf(Direction.RIGHT) }
    var food by remember { mutableStateOf(Position(15, 15)) }
    
    // 生成新食物
    fun generateFood() {
        var newFood: Position
        do {
            newFood = Position(
                Random.nextInt(0, boardSize),
                Random.nextInt(0, boardSize)
            )
        } while (snake.contains(newFood))
        food = newFood
    }
    
    // 保存最高分
    fun saveHighScore() {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[intPreferencesKey("snake_high_score")] = highScore
            }
        }
    }
    
    // 重置游戏
    fun resetGame() {
        snake = listOf(Position(10, 10))
        direction = Direction.RIGHT
        score = 0
        gameSpeed = 150L - (difficulty - 1) * 30L
        generateFood()
        gameState = GameState.PLAYING
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    
    // 游戏循环
    LaunchedEffect(gameState) {
        while (gameState == GameState.PLAYING) {
            delay(gameSpeed)
            
            val head = snake.first()
            val newHead = when (direction) {
                Direction.UP -> Position(head.x, head.y - 1)
                Direction.DOWN -> Position(head.x, head.y + 1)
                Direction.LEFT -> Position(head.x - 1, head.y)
                Direction.RIGHT -> Position(head.x + 1, head.y)
            }
            
            // 检查边界碰撞
            if (newHead.x < 0 || newHead.x >= boardSize || 
                newHead.y < 0 || newHead.y >= boardSize) {
                gameState = GameState.GAME_OVER
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                if (score > highScore) {
                    highScore = score
                    saveHighScore()
                }
                continue
            }
            
            // 检查自身碰撞
            if (snake.contains(newHead)) {
                gameState = GameState.GAME_OVER
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                if (score > highScore) {
                    highScore = score
                    saveHighScore()
                }
                continue
            }
            
            val newSnake = listOf(newHead) + snake
            
            // 检查是否吃到食物
            if (newHead == food) {
                score += 10 * difficulty
                snake = newSnake
                generateFood()
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                
                // 动态调整游戏速度
                if (score % (50 * difficulty) == 0 && gameSpeed > 80L) {
                    gameSpeed -= 10L
                }
            } else {
                snake = newSnake.dropLast(1)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🐍 贪吃蛇游戏") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 得分显示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScoreCard("得分", score)
                ScoreCard("最高分", highScore)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 游戏区域（支持滑动手势）
            GameBoard(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .pointerInput(gameState) {
                        if (gameState == GameState.PLAYING) {
                            detectDragGestures(
                                onDragEnd = { 
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            ) { _, dragAmount ->
                                val (dx, dy) = dragAmount
                                val threshold = 50f
                                
                                val newDirection = when {
                                    abs(dx) > abs(dy) -> {
                                        if (dx > threshold) Direction.RIGHT
                                        else if (dx < -threshold) Direction.LEFT
                                        else null
                                    }
                                    abs(dy) > threshold -> {
                                        if (dy > threshold) Direction.DOWN
                                        else Direction.UP
                                    }
                                    else -> null
                                }
                                
                                newDirection?.let { newDir ->
                                    val canChange = when (newDir) {
                                        Direction.UP -> direction != Direction.DOWN
                                        Direction.DOWN -> direction != Direction.UP
                                        Direction.LEFT -> direction != Direction.RIGHT
                                        Direction.RIGHT -> direction != Direction.LEFT
                                    }
                                    if (canChange) {
                                        direction = newDir
                                    }
                                }
                            }
                        }
                    },
                snake = snake,
                food = food,
                boardSize = boardSize
            )
            
            // 难度选择器
            if (gameState == GameState.READY || gameState == GameState.GAME_OVER) {
                DifficultySelector(
                    difficulty = difficulty,
                    onDifficultyChange = { difficulty = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 方向控制按钮
            if (gameState == GameState.PLAYING || gameState == GameState.PAUSED) {
                DirectionControls(
                    onDirectionChange = { newDirection ->
                        if (gameState == GameState.PLAYING) {
                            // 防止直接反向移动
                            val canChange = when (newDirection) {
                                Direction.UP -> direction != Direction.DOWN
                                Direction.DOWN -> direction != Direction.UP
                                Direction.LEFT -> direction != Direction.RIGHT
                                Direction.RIGHT -> direction != Direction.LEFT
                            }
                            if (canChange) {
                                direction = newDirection
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 控制按钮
            when (gameState) {
                GameState.READY -> {
                    GameButton("🎮 开始游戏") { resetGame() }
                }
                GameState.PLAYING -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GameButton("⏸️ 暂停") { gameState = GameState.PAUSED }
                        GameButton("🔄 重新开始") { resetGame() }
                    }
                }
                GameState.PAUSED -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GameButton("▶️ 继续") { gameState = GameState.PLAYING }
                        GameButton("🔄 重新开始") { resetGame() }
                    }
                }
                GameState.GAME_OVER -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val textScale by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy
                            ),
                            label = "game_over_text"
                        )
                        
                        Text(
                            text = "🎯 游戏结束！",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.scale(textScale)
                        )
                        Text(
                            text = "得分: $score",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (score == highScore && score > 0) {
                            Text(
                                text = "🏆 新纪录！",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        GameButton("🔄 再来一局") { resetGame() }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 游戏说明
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
                        text = "🎮 游戏说明",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• 点击方向按钮控制蛇的方向\n• 吃到红色食物得分\n• 避免撞到边界和自己\n• 挑战更高分数！",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun ScoreCard(title: String, score: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun GameButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun GameBoard(
    modifier: Modifier = Modifier,
    snake: List<Position>,
    food: Position,
    boardSize: Int
) {
    val density = LocalDensity.current
    
    Canvas(modifier = modifier) {
        val cellSize = size.width / boardSize
        
        // 绘制背景网格
        for (i in 0..boardSize) {
            // 垂直线
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(i * cellSize, 0f),
                end = Offset(i * cellSize, size.height),
                strokeWidth = 1.dp.toPx()
            )
            // 水平线
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(0f, i * cellSize),
                end = Offset(size.width, i * cellSize),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // 绘制食物
        drawFood(food, cellSize, Color.Red)
        
        // 绘制蛇
        snake.forEachIndexed { index, position ->
            val color = if (index == 0) {
                Color(0xFF4CAF50) // 蛇头 - 绿色
            } else {
                Color(0xFF8BC34A) // 蛇身 - 浅绿色
            }
            drawSnakeSegment(position, cellSize, color)
        }
    }
}

fun DrawScope.drawFood(position: Position, cellSize: Float, color: Color) {
    val padding = cellSize * 0.1f
    drawOval(
        color = color,
        topLeft = Offset(
            position.x * cellSize + padding,
            position.y * cellSize + padding
        ),
        size = Size(
            cellSize - 2 * padding,
            cellSize - 2 * padding
        )
    )
}

fun DrawScope.drawSnakeSegment(position: Position, cellSize: Float, color: Color) {
    val padding = cellSize * 0.05f
    drawRoundRect(
        color = color,
        topLeft = Offset(
            position.x * cellSize + padding,
            position.y * cellSize + padding
        ),
        size = Size(
            cellSize - 2 * padding,
            cellSize - 2 * padding
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cellSize * 0.1f)
    )
}

@Composable
fun DirectionControls(onDirectionChange: (Direction) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 上方向按钮
        DirectionButton(Icons.Default.KeyboardArrowUp, Direction.UP) { 
            onDirectionChange(Direction.UP) 
        }
        
        // 中间一行：左和右，使用固定间距确保对称
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DirectionButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, Direction.LEFT) { 
                onDirectionChange(Direction.LEFT) 
            }
            DirectionButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, Direction.RIGHT) { 
                onDirectionChange(Direction.RIGHT) 
            }
        }
        
        // 下方向按钮
        DirectionButton(Icons.Default.KeyboardArrowDown, Direction.DOWN) { 
            onDirectionChange(Direction.DOWN) 
        }
    }
}

@Composable
fun DirectionButton(icon: ImageVector, direction: Direction, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "button_scale"
    )
    
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(60.dp)
            .scale(scale)
            .clip(CircleShape),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = when (direction) {
                Direction.UP -> "向上"
                Direction.DOWN -> "向下"
                Direction.LEFT -> "向左"
                Direction.RIGHT -> "向右"
            },
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun DifficultySelector(
    difficulty: Int,
    onDifficultyChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎯 难度选择",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    1 to "🐣 简单",
                    2 to "🚀 中等", 
                    3 to "🔥 困难",
                    4 to "💀 地狱"
                ).forEach { (level, label) ->
                    FilterChip(
                        onClick = { onDifficultyChange(level) },
                        label = { Text(label, fontSize = 12.sp) },
                        selected = difficulty == level,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "得分倍率: ${difficulty}x | 速度: ${when(difficulty) {
                    1 -> "慢"
                    2 -> "中"
                    3 -> "快"
                    4 -> "极快"
                    else -> "未知"
                }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
} 