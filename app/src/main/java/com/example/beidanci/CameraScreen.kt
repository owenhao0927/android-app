package com.example.beidanci

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.beidanci.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen() {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    var isNetworkCameraActive by remember { mutableStateOf(false) }
    var networkCameraUrl by remember { mutableStateOf("") }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Purple40,
                shadowElevation = 8.dp
            ) {
                TopAppBar(
                    title = { 
                        Text(
                            text = "📷 摄像头功能",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        ) 
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
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
                            Purple80.copy(alpha = 0.1f),
                            Purple80.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            if (!hasCameraPermission) {
                // 权限请求界面
                PermissionRequestScreen(
                    onRequestPermission = {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            } else {
                // 摄像头功能界面
                CameraFeaturesScreen(
                    modifier = Modifier.padding(paddingValues),
                    isNetworkCameraActive = isNetworkCameraActive,
                    networkCameraUrl = networkCameraUrl,
                    onNetworkCameraToggle = { active, url ->
                        isNetworkCameraActive = active
                        networkCameraUrl = url
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Camera,
            contentDescription = "摄像头",
            modifier = Modifier.size(80.dp),
            tint = Purple40
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "需要摄像头权限",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Purple40
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "为了使用拍照识别、录制练习等功能，\n需要访问您的摄像头",
            style = MaterialTheme.typography.bodyLarge,
            color = SecondaryText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = Purple40
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "授予摄像头权限",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }
    }
}

@Composable
fun CameraFeaturesScreen(
    modifier: Modifier = Modifier,
    isNetworkCameraActive: Boolean,
    networkCameraUrl: String,
    onNetworkCameraToggle: (Boolean, String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "选择摄像头功能",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            color = Purple40
        )
        
        // 拍照识别单词功能
        CameraFeatureCard(
            title = "📸 拍照识别单词",
            description = "拍摄文本内容，自动识别并生成单词卡片\n支持书本、文档、标识等文字识别",
            backgroundColor = BlueAccent,
            onClick = {
                // TODO: 启动拍照识别功能
            }
        )
        
        // 发音练习录制
        CameraFeatureCard(
            title = "🎥 发音练习录制",
            description = "录制您的发音练习视频\n对比标准发音，提升口语水平",
            backgroundColor = GreenAccent,
            onClick = {
                // TODO: 启动发音录制功能
            }
        )
        
        // 网络摄像头功能
        NetworkCameraCard(
            isActive = isNetworkCameraActive,
            url = networkCameraUrl,
            onToggle = onNetworkCameraToggle
        )
        
        // AR单词显示（未来功能）
        CameraFeatureCard(
            title = "🌟 AR单词显示",
            description = "实时摄像头画面中显示单词翻译\n指向物体即可看到英文单词（开发中）",
            backgroundColor = OrangeAccent,
            onClick = {
                // TODO: AR功能
            }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "💡 灵感来源于苹果随航功能",
            style = MaterialTheme.typography.bodyMedium,
            color = SecondaryText,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CameraFeatureCard(
    title: String,
    description: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        onClick = onClick
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
                            fontSize = 18.sp
                        ),
                        color = backgroundColor
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText,
                        lineHeight = 18.sp
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = backgroundColor,
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "摄像头",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkCameraCard(
    isActive: Boolean,
    url: String,
    onToggle: (Boolean, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            if (isActive) GreenAccent.copy(alpha = 0.1f) else RedAccent.copy(alpha = 0.1f),
                            if (isActive) GreenAccent.copy(alpha = 0.3f) else RedAccent.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📡 网络摄像头服务",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = if (isActive) GreenAccent else RedAccent
                        )
                        Text(
                            text = if (isActive) "正在运行" else "已停止",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText
                        )
                    }
                    
                    Switch(
                        checked = isActive,
                        onCheckedChange = { checked ->
                            if (checked) {
                                val newUrl = "http://192.168.1.100:8080/camera"
                                onToggle(true, newUrl)
                            } else {
                                onToggle(false, "")
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GreenAccent,
                            checkedTrackColor = GreenAccent.copy(alpha = 0.3f)
                        )
                    )
                }
                
                if (isActive && url.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GreenAccent.copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "📱 访问地址：",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = GreenAccent
                            )
                            Text(
                                text = url,
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "其他设备可通过此地址使用您的摄像头",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
} 