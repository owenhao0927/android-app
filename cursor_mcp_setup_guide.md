# Cursor MCP 服务器配置指南

## 在 Cursor 中添加 MCP 服务器的步骤：

### 1. Sequential Thinking Server
- **Name**: `sequential-thinking`
- **Command**: `npx`
- **Args**: `-y @smithery/cli@latest run @smithery-ai/server-sequential-thinking --config {}`

### 2. Fetch Server
- **Name**: `fetch`
- **Command**: `npx`
- **Args**: `-y @smithery/cli@latest run @smithery-ai/fetch --config {}`

### 3. Files Server
- **Name**: `files`
- **Command**: `npx`
- **Args**: `-y @modelcontextprotocol/server-filesystem /Users/owen`

### 4. Hot News Server
- **Name**: `hotnews`
- **Command**: `npx`
- **Args**: `@wopal/mcp-server-hotnews`

### 5. Playwright Server
- **Name**: `playwright`
- **Command**: `npx`
- **Args**: `-y @playwright/mcp@latest`

### 6. Hacker News Server
- **Name**: `hn-server`
- **Command**: `npx`
- **Args**: `-y @smithery/cli@latest run @pskill9/hn-server`

### 7. DuckDuckGo Server
- **Name**: `duckduckgo`
- **Command**: `npx`
- **Args**: `-y @smithery/cli@latest run @nickclyde/duckduckgo-mcp-server`

## 操作步骤：
1. 在 Cursor 中打开设置 (⌘+,)
2. 点击左侧的 "Tools & Integrations"
3. 在 "MCP Tools" 部分点击 "New MCP Server"
4. 逐个添加上述 7 个服务器配置
5. 每个服务器添加完成后，确保开关是打开状态

## 注意事项：
- 确保你的系统已安装 Node.js 和 npm
- 首次使用时，npm 包会自动下载
- 如果某个服务器无法启动，检查网络连接和 npm 权限 