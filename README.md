# Android Code Assistant

一个类似 Claude 的安卓代码助手应用，支持配置大模型。

## 功能特性

### ✅ Phase 1 (已实现)
- 对话界面 - 类似 ChatGPT/Claude 的聊天 UI
- 模型配置页面
  - 支持 OpenAI / Anthropic / 自定义 API
  - 可配置 API Key、Base URL、模型名称
  - 支持温度、max_tokens 参数调节
- 对话历史
  - 侧边栏显示历史对话
  - 新建/删除对话
  - Room 数据库持久化

### 🚧 Phase 2 (待实现)
- 代码语法高亮
- Markdown 渲染
- 流式响应显示优化
- 文件改动预览

## 技术栈
- **语言**: Kotlin
- **UI**: Jetpack Compose
- **架构**: MVVM
- **网络**: Retrofit + OkHttp
- **存储**: Room + DataStore

## 项目结构

```
app/src/main/java/com/codeassistant/
├── MainActivity.kt           # 主活动
├── CodeAssistantApp.kt       # Application类
├── data/
│   ├── api/                  # API接口定义
│   ├── database/             # Room数据库
│   ├── model/                # 数据模型
│   └── repository/           # 数据仓库
└── ui/
    ├── chat/                 # 聊天界面
    ├── settings/             # 设置界面
    └── theme/                # 主题配置
```

## 如何使用

### 1. 克隆项目
```bash
git clone https://github.com/your-username/CodeAssistant.git
```

### 2. 用 Android Studio 打开
- Android Studio Hedgehog 或更高版本
- JDK 17

### 3. 配置模型
在应用中点击设置按钮，配置：
- API Key（从 OpenAI/Anthropic 获取）
- Base URL（默认 OpenAI，可改为兼容 API）
- 模型名称（如 gpt-4, claude-3-opus）

### 4. 运行
连接 Android 设备或模拟器，点击运行

## 支持的 API

### OpenAI
- Base URL: `https://api.openai.com/v1`
- 模型: gpt-4, gpt-4-turbo, gpt-3.5-turbo

### Anthropic
- Base URL: `https://api.anthropic.com/v1`
- 模型: claude-3-opus, claude-3-sonnet, claude-3-haiku

### 自定义
支持任何 OpenAI 兼容的 API，如：
- 本地部署的 Ollama
- Azure OpenAI
- 其他兼容服务

## 许可证
MIT License