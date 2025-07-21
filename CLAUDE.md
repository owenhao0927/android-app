# CLAUDE.md

此文件为Claude Code (claude.ai/code) 在此代码库中工作时提供指导。

## 项目概述

背单词（Beidanci）是一个基于Kotlin和Jetpack Compose构建的Android词汇学习应用。该应用通过互动功能、游戏和AI生成的词汇帮助用户学习英语单词。

## 构建命令

### 基础命令
- **构建项目**: `./gradlew build`
- **清理构建**: `./gradlew clean build`
- **运行测试**: `./gradlew test`
- **运行仪器测试**: `./gradlew connectedAndroidTest`
- **安装调试APK**: `./gradlew installDebug`
- **构建发布版**: `./gradlew assembleRelease`

### 开发命令
- **构建并安装**: `./gradlew installDebug`
- **运行代码检查**: `./gradlew lint`
- **生成测试报告**: `./gradlew testDebugUnitTest`

## 架构概览

### 技术栈
- **编程语言**: Kotlin
- **UI框架**: Jetpack Compose with Material3
- **导航**: Navigation Compose
- **数据存储**: DataStore Preferences
- **HTTP客户端**: Retrofit + OkHttp
- **JSON解析**: Gson
- **协程**: Kotlinx Coroutines
- **最低SDK**: 24 (Android 7.0)
- **目标SDK**: 35

### 核心组件

#### 数据层
- **Word**: 主要数据类 (`word.kt:3-11`) 表示词汇项目，包含文本、音标、翻译、例句和其他形式
- **DataStore**: 用于单词、用户偏好和设置的持久化存储
- **ChatGptService**: AI服务集成，用于生成词汇和单词详情

#### UI层 (Jetpack Compose)
- **MainActivity**: 主入口点，包含导航设置
- **导航结构**: 底部导航包含5个主要界面：
  - 首页：每日词汇展示
  - 词库：词汇收藏
  - 收藏：保存的单词
  - 游戏：贪吃蛇和俄罗斯方块游戏
  - 设置：难度级别配置

#### 主要功能
- **AI单词生成**: ChatGPT集成，基于难度级别动态创建词汇
- **缓存系统**: 基于日期组织的持久化单词存储
- **难度级别**: 从小学到大学的4个级别
- **游戏集成**: 教育游戏（贪吃蛇、俄罗斯方块）
- **单词详情**: 全面的单词信息和相关单词导航

### 文件结构
```
app/src/main/java/com/example/beidanci/
├── MainActivity.kt         # 主活动和首页界面
├── word.kt                # 核心Word数据类
├── ChatGptService.kt      # AI集成服务
├── SettingsScreen.kt      # 设置界面和难度管理
├── WordDetailScreen.kt    # 单词详情页面
├── WordBankScreen.kt      # 词汇收藏页面
├── PracticeActivity.kt    # 练习活动
├── SnakeGameScreen.kt     # 贪吃蛇游戏实现
├── TetrisGameScreen.kt    # 俄罗斯方块游戏实现
└── ui/theme/              # Material3主题配置
```

## 开发指南

### 数据管理
- 单词通过基于日期的键值存储在DataStore中 (`words_YYYY-MM-DD`)
- 始终使用 `getTodayWords()` 和 `generateNewWords()` 扩展函数进行单词操作
- 优雅地处理API失败，使用备用单词列表

### UI模式
- 遵循Material3设计原则
- 使用Scaffold和TopAppBar保持一致的布局
- 使用CircularProgressIndicator实现合适的加载状态
- 导航使用URI编码进行复杂数据传递

### API集成
- ChatGPT API密钥在 `ChatGptService.kt:9` 中配置
- 始终为API失败实现备用机制
- 使用结构化提示获得一致的AI响应
- 仔细解析JSON响应并进行错误处理

### 测试
- 单元测试: `app/src/test/`
- 仪器测试: `app/src/androidTest/`
- 测试Word数据类和ChatGPT服务解析逻辑

### 常见任务

#### 添加新的单词来源
1. 修改 `ChatGptService.kt` 包含新的生成逻辑
2. 更新提示以匹配所需的词汇风格
3. 确保在 `parseWordsFromResponse()` 中正确解析JSON

#### 修改UI界面
1. 界面遵循Compose模式，使用remember状态
2. 使用LaunchedEffect进行数据加载
3. 实现适当的错误处理和加载状态

#### 添加新的难度级别
1. 更新 `SettingsScreen.kt:20-25` 中的 `DifficultyLevel` 枚举
2. 修改 `createWordGenerationPrompt()` 中的提示生成
3. 测试难度选择和单词生成

### 安全注意事项
- ChatGPT API需要网络权限
- TTS功能需要音频录制权限
- 生产构建时应将API密钥移至BuildConfig