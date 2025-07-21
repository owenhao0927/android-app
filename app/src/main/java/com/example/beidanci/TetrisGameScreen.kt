package com.example.beidanci

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// 俄罗斯方块游戏状态
enum class TetrisGameState {
    READY, PLAYING, PAUSED, GAME_OVER
}

// 俄罗斯方块形状
enum class TetrominoType {
    I, O, T, S, Z, J, L
}

// 位置数据类
data class TetrisPosition(val x: Int, val y: Int)

// 俄罗斯方块数据类
data class Tetromino(
    val type: TetrominoType,
    val blocks: List<TetrisPosition>,
    val color: Color,
    val position: TetrisPosition = TetrisPosition(0, 0)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TetrisGameScreen() {
    android.util.Log.d("TetrisGameScreen", "TetrisGameScreen 开始渲染")
    var gameState by remember { mutableStateOf(TetrisGameState.READY) }
    var score by remember { mutableStateOf(0) }
    var lines by remember { mutableStateOf(0) }
    var level by remember { mutableStateOf(1) }
    var highScore by remember { mutableStateOf(0) }
    
    // 游戏参数
    val boardWidth = 10
    val boardHeight = 20
    var gameSpeed by remember { mutableStateOf(1000L) }
    
    // 游戏状态
    var board by remember { mutableStateOf(Array(boardHeight) { Array(boardWidth) { Color.Transparent } }) }
    var currentPiece by remember { mutableStateOf<Tetromino?>(null) }
    // 生成随机俄罗斯方块
    fun generateRandomTetromino(): Tetromino {
        val types = TetrominoType.values()
        val type = types[Random.nextInt(types.size)]
        return createTetromino(type)
    }
    
    var nextPiece by remember { mutableStateOf(generateRandomTetromino()) }
    
    // 检查位置是否有效
    fun isValidPosition(piece: Tetromino, newPos: TetrisPosition): Boolean {
        return piece.blocks.all { block ->
            val x = block.x + newPos.x
            val y = block.y + newPos.y
            x >= 0 && x < boardWidth && y >= 0 && y < boardHeight && 
            (y >= boardHeight || board[y][x] == Color.Transparent)
        }
    }
    
    // 放置方块到游戏板
    fun placePiece(piece: Tetromino) {
        piece.blocks.forEach { block ->
            val x = block.x + piece.position.x
            val y = block.y + piece.position.y
            if (y >= 0 && y < boardHeight && x >= 0 && x < boardWidth) {
                board[y][x] = piece.color
            }
        }
    }
    
    // 检查并清除完整行
    fun clearLines(): Int {
        var clearedLines = 0
        val newBoard = Array(boardHeight) { Array(boardWidth) { Color.Transparent } }
        var newRow = boardHeight - 1
        
        for (row in boardHeight - 1 downTo 0) {
            if (board[row].any { it == Color.Transparent }) {
                // 行不完整，保留
                for (col in 0 until boardWidth) {
                    newBoard[newRow][col] = board[row][col]
                }
                newRow--
            } else {
                // 行完整，清除
                clearedLines++
            }
        }
        
        board = newBoard
        return clearedLines
    }
    
    // 旋转方块
    fun rotatePiece(piece: Tetromino): Tetromino {
        if (piece.type == TetrominoType.O) return piece // O型方块不需要旋转
        
        val rotatedBlocks = piece.blocks.map { block ->
            TetrisPosition(-block.y, block.x)
        }
        return piece.copy(blocks = rotatedBlocks)
    }
    
    // 重置游戏
    fun resetGame() {
        board = Array(boardHeight) { Array(boardWidth) { Color.Transparent } }
        currentPiece = generateRandomTetromino().copy(position = TetrisPosition(boardWidth / 2 - 1, 0))
        nextPiece = generateRandomTetromino()
        score = 0
        lines = 0
        level = 1
        gameSpeed = 1000L
        gameState = TetrisGameState.PLAYING
    }
    
    // 移动方块
    fun movePiece(dx: Int, dy: Int = 0): Boolean {
        currentPiece?.let { piece ->
            val newPos = TetrisPosition(piece.position.x + dx, piece.position.y + dy)
            if (isValidPosition(piece, newPos)) {
                currentPiece = piece.copy(position = newPos)
                return true
            }
        }
        return false
    }
    
    // 游戏循环
    LaunchedEffect(gameState) {
        while (gameState == TetrisGameState.PLAYING) {
            delay(gameSpeed)
            
            currentPiece?.let { piece ->
                if (!movePiece(0, 1)) {
                    // 方块无法下移，放置到游戏板
                    placePiece(piece)
                    
                    // 清除完整行
                    val clearedLines = clearLines()
                    if (clearedLines > 0) {
                        lines += clearedLines
                        score += clearedLines * 100 * level
                        level = lines / 10 + 1
                        gameSpeed = maxOf(100L, 1000L - (level - 1) * 100L)
                    }
                    
                    // 生成新方块
                    currentPiece = nextPiece.copy(position = TetrisPosition(boardWidth / 2 - 1, 0))
                    nextPiece = generateRandomTetromino()
                    
                    // 检查游戏结束
                    if (!isValidPosition(currentPiece!!, currentPiece!!.position)) {
                        gameState = TetrisGameState.GAME_OVER
                        if (score > highScore) {
                            highScore = score
                        }
                    }
                }
            } ?: run {
                // 初始化第一个方块
                currentPiece = nextPiece.copy(position = TetrisPosition(boardWidth / 2 - 1, 0))
                nextPiece = generateRandomTetromino()
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🧩 俄罗斯方块") },
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
            // 游戏信息面板
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GameInfoCard("得分", score.toString())
                GameInfoCard("行数", lines.toString())
                GameInfoCard("等级", level.toString())
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 游戏区域和预览
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 游戏区域
                TetrisBoard(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.5f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    board = board,
                    currentPiece = currentPiece,
                    boardWidth = boardWidth,
                    boardHeight = boardHeight
                )
                
                // 下一个方块预览
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "下一个",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    NextPiecePreview(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        piece = nextPiece
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 控制按钮区域 - 使用Box确保按钮显示
            android.util.Log.d("TetrisGameScreen", "渲染控制按钮区域，当前游戏状态: $gameState")
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (gameState) {
                    TetrisGameState.READY -> {
                        android.util.Log.d("TetrisGameScreen", "渲染READY状态的按钮")
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { 
                                    android.util.Log.d("TetrisGameScreen", "开始游戏按钮被点击")
                                    resetGame() 
                                },
                                modifier = Modifier
                                    .height(60.dp)
                                    .padding(horizontal = 32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                android.util.Log.d("TetrisGameScreen", "渲染开始游戏按钮文本")
                                Text(
                                    text = "🎮 开始游戏",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Text(
                                text = "点击开始游戏按钮开始俄罗斯方块！",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    TetrisGameState.PLAYING -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 旋转按钮
                            TetrisButton("🔄 旋转") {
                                currentPiece?.let { piece ->
                                    val rotated = rotatePiece(piece)
                                    if (isValidPosition(rotated, piece.position)) {
                                        currentPiece = rotated.copy(position = piece.position)
                                    }
                                }
                            }
                            
                            // 方向控制
                            TetrisControls(
                                onLeft = { movePiece(-1) },
                                onRight = { movePiece(1) },
                                onDown = { movePiece(0, 1) },
                                onDrop = {
                                    // 快速下落
                                    currentPiece?.let { piece ->
                                        var dropDistance = 0
                                        while (movePiece(0, 1)) {
                                            dropDistance++
                                        }
                                        if (dropDistance > 0) {
                                            score += dropDistance
                                        }
                                    }
                                }
                            )
                            
                            // 暂停和重新开始
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                TetrisButton("⏸️ 暂停") { gameState = TetrisGameState.PAUSED }
                                TetrisButton("🔄 重新开始") { resetGame() }
                            }
                        }
                    }
                    TetrisGameState.PAUSED -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TetrisButton("▶️ 继续") { gameState = TetrisGameState.PLAYING }
                            TetrisButton("🔄 重新开始") { resetGame() }
                        }
                    }
                    TetrisGameState.GAME_OVER -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "🎯 游戏结束！",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "得分: $score\n消除行数: $lines\n达到等级: $level",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            if (score == highScore && score > 0) {
                                Text(
                                    text = "🏆 新纪录！",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TetrisButton("🔄 再来一局") { resetGame() }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// 创建俄罗斯方块
fun createTetromino(type: TetrominoType): Tetromino {
    return when (type) {
        TetrominoType.I -> Tetromino(
            type = type,
            blocks = listOf(
                TetrisPosition(0, 1), TetrisPosition(1, 1),
                TetrisPosition(2, 1), TetrisPosition(3, 1)
            ),
            color = Color.Cyan
        )
        TetrominoType.O -> Tetromino(
            type = type,
            blocks = listOf(
                TetrisPosition(0, 0), TetrisPosition(1, 0),
                TetrisPosition(0, 1), TetrisPosition(1, 1)
            ),
            color = Color.Yellow
        )
        TetrominoType.T -> Tetromino(
            type = type,
            blocks = listOf(
                TetrisPosition(1, 0),
                TetrisPosition(0, 1), TetrisPosition(1, 1), TetrisPosition(2, 1)
            ),
            color = Color.Magenta
        )
        TetrominoType.S -> Tetromino(
            type = type,
            blocks = listOf(
                TetrisPosition(1, 0), TetrisPosition(2, 0),
                TetrisPosition(0, 1), TetrisPosition(1, 1)
            ),
            color = Color.Green
        )
        TetrominoType.Z -> Tetromino(
            type = type,
            blocks = listOf(
                TetrisPosition(0, 0), TetrisPosition(1, 0),
                TetrisPosition(1, 1), TetrisPosition(2, 1)
            ),
            color = Color.Red
        )
        TetrominoType.J -> Tetromino(
            type = type,
            blocks = listOf(
                TetrisPosition(0, 0),
                TetrisPosition(0, 1), TetrisPosition(1, 1), TetrisPosition(2, 1)
            ),
            color = Color.Blue
        )
        TetrominoType.L -> Tetromino(
            type = type,
            blocks = listOf(
                TetrisPosition(2, 0),
                TetrisPosition(0, 1), TetrisPosition(1, 1), TetrisPosition(2, 1)
            ),
            color = Color(0xFFFFA500) // Orange
        )
    }
}

@Composable
fun GameInfoCard(title: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun TetrisButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(56.dp)
            .padding(horizontal = 16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TetrisControls(
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onDown: () -> Unit,
    onDrop: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 左右控制行
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TetrisControlButton(Icons.Default.KeyboardArrowLeft, "左移") { onLeft() }
            TetrisControlButton(Icons.Default.KeyboardArrowDown, "下落") { onDown() }
            TetrisControlButton(Icons.Default.KeyboardArrowRight, "右移") { onRight() }
        }
        
        // 快速下落按钮
        Button(
            onClick = onDrop,
            modifier = Modifier
                .width(120.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(
                text = "⬇️ 快落",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TetrisControlButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = PaddingValues(0.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun TetrisBoard(
    modifier: Modifier = Modifier,
    board: Array<Array<Color>>,
    currentPiece: Tetromino?,
    boardWidth: Int,
    boardHeight: Int
) {
    Canvas(modifier = modifier) {
        val cellSize = size.width / boardWidth
        
        // 绘制背景网格
        for (x in 0..boardWidth) {
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(x * cellSize, 0f),
                end = Offset(x * cellSize, size.height),
                strokeWidth = 1.dp.toPx()
            )
        }
        for (y in 0..boardHeight) {
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(0f, y * cellSize),
                end = Offset(size.width, y * cellSize),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // 绘制已放置的方块
        for (y in 0 until boardHeight) {
            for (x in 0 until boardWidth) {
                if (board[y][x] != Color.Transparent) {
                    drawTetrisBlock(x, y, cellSize, board[y][x])
                }
            }
        }
        
        // 绘制当前下落的方块
        currentPiece?.let { piece ->
            piece.blocks.forEach { block ->
                val x = block.x + piece.position.x
                val y = block.y + piece.position.y
                if (x >= 0 && x < boardWidth && y >= 0 && y < boardHeight) {
                    drawTetrisBlock(x, y, cellSize, piece.color)
                }
            }
        }
    }
}

@Composable
fun NextPiecePreview(
    modifier: Modifier = Modifier,
    piece: Tetromino
) {
    Canvas(modifier = modifier) {
        val cellSize = size.width / 4
        
        piece.blocks.forEach { block ->
            drawTetrisBlock(block.x, block.y, cellSize, piece.color)
        }
    }
}

fun DrawScope.drawTetrisBlock(x: Int, y: Int, cellSize: Float, color: Color) {
    val padding = cellSize * 0.05f
    drawRect(
        color = color,
        topLeft = Offset(
            x * cellSize + padding,
            y * cellSize + padding
        ),
        size = Size(
            cellSize - 2 * padding,
            cellSize - 2 * padding
        )
    )
    
    // 添加高光效果
    drawRect(
        color = color.copy(alpha = 0.3f),
        topLeft = Offset(
            x * cellSize + padding,
            y * cellSize + padding
        ),
        size = Size(
            cellSize - 2 * padding,
            cellSize * 0.3f
        )
    )
} 