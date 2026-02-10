# Cronet Capture

<div align="center">

**一个用于捕获、转发和分析 Android Cronet 网络请求的逆向工程工具集**

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Rust](https://img.shields.io/badge/rust-1.70%2B-orange.svg)](https://www.rust-lang.org/)
[![Android](https://img.shields.io/badge/android-8.0%2B-green.svg)](https://developer.android.com/)

[English](README_EN.md) | 简体中文

</div>

---

> [!CAUTION]
> ## ⚠️ 免责声明 / DISCLAIMER
> 
> **本项目仅供学习、研究和技术交流使用，严禁用于任何非法用途。**
> 
> - **研究性质**：本项目是逆向工程研究的成果，用于学习网络协议和 Android Hook 技术
> - **教育目的**：代码仅用于教学和技术研讨，帮助理解网络拦截与协议设计
> - **禁止滥用**：严禁将本项目用于未经授权的数据拦截、窃取或其他违法行为
> - **法律责任**：使用者必须遵守所在地区的法律法规，因滥用本项目导致的任何法律后果由使用者自行承担
> - **安全风险**：本项目未实现完整的安全机制，**不建议在生产环境使用**
> - **隐私保护**：请勿使用本项目处理包含敏感信息或个人隐私的数据
> 
> **使用本项目即表示您已阅读、理解并同意上述条款。若不同意，请立即停止使用。**

---

## 📖 项目简介

Cronet Capture 是一个完整的 Android 网络请求捕获解决方案，专门针对使用 Google Cronet 网络库的应用。该项目通过 Xposed 框架 Hook Cronet API，实时捕获网络请求和响应数据，并通过自定义协议转发到后端服务器进行存储和分析。

### 🎯 核心特性

- ✅ **Cronet Hook**: 基于 LibXposed API 的现代化 Hook 实现
- ✅ **高性能转发**: Rust + JNI 实现的零拷贝数据转发，手机端无卡顿
- ✅ **自定义协议**: 基于 Magic Number 的二进制协议，支持流式传输
- ✅ **异步处理**: Tokio 异步运行时，高并发场景下性能优异
- ✅ **Redis 存储**: 数据持久化到 Redis Stream，方便后续分析
- ✅ **路由表**: 灵活的正则表达式路由匹配与转发
- ✅ **热重载**: 配置文件支持热重载，无需重启服务

### 📦 使用方法

1. 确保你的手机已经安装 lsposed 框架
2. 下载并安装 CronetForward_1.0.apk
3. 在 lsposed 中激活该插件，并确保对目标应用开启
4. 打开 CronetForward 前端配置页进行转发、反混淆等配置
5. 编译 CronetReceiver 并启动
6. 打开应用，所有经过 cronet 网络库的流量都会被捕获。

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                         Android 设备                             │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    目标应用 (被 Hook)                     │   │
│  │                         ↓                                  │   │
│  │                   Cronet 网络库                           │   │
│  │                         ↓                                  │   │
│  │  ┌──────────────────────────────────────────────────┐   │   │
│  │  │          Xposed 模块 (MainModule.java)            │   │   │
│  │  │  • Hook onReadCompleted()                         │   │   │
│  │  │  • Hook onSucceeded()                             │   │   │
│  │  │  • 提取 URL + Response Body                       │   │   │
│  │  └──────────────────┬───────────────────────────────┘   │   │
│  │                     ↓ (JNI Call)                         │   │
│  │  ┌──────────────────────────────────────────────────┐   │   │
│  │  │    Native 层 (cronet-forward/lib.rs)             │   │   │
│  │  │  • 封装自定义协议                                 │   │   │
│  │  │  • 异步发送到服务器                               │   │   │
│  │  └──────────────────┬───────────────────────────────┘   │   │
│  └────────────────────│────────────────────────────────────┘   │
└────────────────────────│─────────────────────────────────────┘
                         ↓ (TCP 协议)
┌─────────────────────────────────────────────────────────────────┐
│                      服务器端 (Cronet Receiver)                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              TCP Server (main.rs)                         │  │
│  │  • 接收并解析自定义协议包                                │  │
│  │  • 粘包处理 (ProtocolCodec)                              │  │
│  │  • 数据组装 (分块/单包)                                  │  │
│  └──────────────────┬───────────────────────────────────────┘  │
│                     ↓                                            │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │            路由表 + Redis 存储 (redis.rs)                │  │
│  │  • 基于正则表达式匹配路由                                │  │
│  │  • 异步写入 Redis Stream                                 │  │
│  └──────────────────┬───────────────────────────────────────┘  │
└────────────────────│─────────────────────────────────────────┘
                      ↓
              ┌─────────────┐
              │    Redis    │
              │   Storage   │
              └─────────────┘
```

## 📦 项目结构

```
cronet-capture/
├── xposed/                    # Android Xposed 模块
│   ├── app/src/main/java/cafe/ivory/cronet/
│   │   ├── MainModule.java    # Hook 核心逻辑
│   │   ├── Config*.java       # 配置管理
│   │   └── MainActivity.java  # UI 界面
│   └── build.gradle.kts       # Gradle 构建配置
│
├── libs/
│   ├── cronet-forward/        # Rust JNI 转发库
│   │   ├── src/lib.rs         # JNI 接口实现
│   │   └── docs/protocol.md   # 协议文档
│   │
│   └── cronet-receiver/       # Rust TCP 服务器
│       ├── src/
│       │   ├── main.rs        # 服务器主程序
│       │   ├── protocol.rs    # 协议解析
│       │   ├── redis.rs       # Redis 存储
│       │   └── config.rs      # 配置管理
│       ├── config.json        # 服务器配置
│       └── test/test_protocol.py  # 协议测试客户端
│
└── README.md                  # 本文档
```

## 🚀 快速开始

### 前置要求

- **Android 设备**: Root 权限 + LSPosed/EdXposed 框架
- **开发环境**:
  - Rust 1.70+ (推荐 1.80+)
  - Android NDK r25+
  - JDK 17+
  - Redis 5.0+

### 1️⃣ 构建 Native 转发库

```bash
cd libs/cronet-forward

# 安装 Android 目标
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi

# 配置 NDK 路径
export ANDROID_NDK_HOME=/path/to/ndk

# 构建 ARM64
cargo ndk build --release --target aarch64-linux-android

# 将生成的 .so 文件复制到 Xposed 项目
cp target/aarch64-linux-android/release/libcronet_forward.so \
   ../../xposed/app/src/main/jniLibs/arm64-v8a/
```

### 2️⃣ 构建并安装 Xposed 模块

```bash
cd xposed

# 构建 APK
./gradlew assembleRelease

# 安装到设备
adb install app/build/outputs/apk/release/app-release.apk
```

在 LSPosed 管理器中：
1. 启用 Cronet Capture 模块
2. 选择目标应用（如小红书）
3. 重启目标应用

### 3️⃣ 配置并运行接收服务器

```bash
cd libs/cronet-receiver

# 编辑配置文件
vim config.json
```

示例配置：
```json
{
    "auth_key": "my_secret_key",
    "listener": {
        "host": "0.0.0.0",
        "port": 9000
    },
    "redis": {
        "host": "localhost",
        "port": 6379
    },
    "route_table": {
        "default": {
            "pattern": ".*",
            "target_stream": "default_stream"
        },
        "api_route": {
            "pattern": "^/api/.*",
            "target_stream": "api_stream"
        }
    }
}
```

启动服务器：
```bash
cargo run --release
```

### 4️⃣ 验证运行

```bash
# 查看 Redis 数据
redis-cli

# 查看所有 Stream
SCAN 0 MATCH *_stream

# 读取捕获的数据
XREAD COUNT 10 STREAMS api_stream 0
```

## 📋 自定义协议格式

```
┌────────────┬──────────────┬─────────────┬──────────────┬──────────┬────────────┬──────────┬──────────┐
│ MagicNumber│ TagPackageLen│  TagUrlLen  │ PayloadLen   │ EndFlag  │ TagPackage │  TagUrl  │ Payload  │
│    4 B     │     2 B      │    2 B      │     4 B      │   4 B    │   M bytes  │ N bytes  │ K bytes  │
└────────────┴──────────────┴─────────────┴──────────────┴──────────┴────────────┴──────────┴──────────┘
```

### 字段说明

| 字段 | 长度 | 类型 | 说明 |
|-----|------|------|------|
| MagicNumber | 4B | 固定 | 魔数，用于协议校验（如 "i0v0"） |
| TagPackageLen | 2B | u16 | TagPackage 字段长度 (大端序) |
| TagUrlLen | 2B | u16 | TagUrl 字段长度 (大端序) |
| PayloadLen | 4B | u32 | Payload 字段长度 (大端序) |
| EndFlag | 4B | u32 | 结束标志：0=继续发送，非0=最后一包 |
| TagPackage | 可变 | UTF-8 | 应用包名 (如 "cafe.ivory.love") |
| TagUrl | 可变 | UTF-8 | 请求 URL |
| Payload | 可变 | bytes | 响应数据（通常为 JSON） |

详细协议说明请参考：
- [cronet-forward/docs/protocol.md](libs/cronet-forward/docs/protocol.md)
- [cronet-receiver/docs/protocol.md](libs/cronet-receiver/docs/protocol.md)

## 🔧 配置说明

### Xposed 模块配置

编辑 [MainModule.java](xposed/app/src/main/java/cafe/ivory/cronet/MainModule.java)：

```java
// 目标应用包名
public static String packageName = "cafe.ivory.love";

// 魔法数字（需与服务器配置一致）
public static String magicNumber = "i0v0";

// 服务器地址
public static String host = "192.168.1.100";
public static int port = 9000;

// 混淆类名（根据实际应用调整）
public static String callbackClassName = "kj5.g";
public static String urlRequestClassName = "org.chromium.net.h0";
// ...
```

### 服务器配置

[config.json](libs/cronet-receiver/example.config.json) 配置项：

```json
{
    "auth_key": "认证密钥（预留功能）",
    "listener": {
        "host": "监听地址",
        "port": 监听端口
    },
    "redis": {
        "host": "Redis 地址",
        "port": Redis 端口
    },
    "route_table": {
        "路由名称": {
            "pattern": "URL 正则表达式",
            "target_stream": "目标 Stream 名称"
        }
    }
}
```

## 🧪 测试

```bash
# 测试协议客户端
cd libs/cronet-receiver/test
python3 test_protocol.py
```

## 🛠️ 开发指南

### 添加新的 Hook 点

可以直接在 xposed 插件的前端页面中增加配置

或者修改 xposed 内 java hook 对应的源码。

### 修改协议格式

1. 同步修改 `cronet-forward/src/lib.rs` 和 `cronet-receiver/src/protocol.rs`
2. 更新魔数或字段顺序后需重新构建并部署
3. 使用 `test_protocol.py` 验证兼容性

### 自定义路由规则

在 `config.json` 中添加路由：

```json
"route_table": {
    "user_api": {
        "pattern": "^/api/user/.*",
        "target_stream": "user_stream"
    },
    "upload": {
        "pattern": "^/upload/.*",
        "target_stream": "upload_stream"
    }
}
```

## 🐛 常见问题

<details>
<summary><b>Q: 为什么没有捕获到数据？</b></summary>

1. 确认 Xposed 模块已激活并重启目标应用
2. 检查 `packageName` 是否正确
3. 验证服务器地址和端口配置
4. 使用 `adb logcat` 查看日志：
   ```bash
   adb logcat | grep -E "NativeTransport|CronetCapture"
   ```
</details>

<details>
<summary><b>Q: JNI 库加载失败？</b></summary>

1. 确认 `.so` 文件在正确的 ABI 目录（如 `arm64-v8a/`）
2. 检查库文件名：`libcronet_forward.so`
3. 确保编译目标架构与设备匹配
</details>

<details>
<summary><b>Q: 服务器收不到数据？</b></summary>

1. 检查防火墙和端口是否开放
2. 确认 Magic Number 配置一致
3. 使用 Wireshark 抓包验证网络连接
</details>

## 📝 许可证

本项目采用 [MIT License](LICENSE) 开源。

## 🙏 致谢

- [LSPosed](https://github.com/LSPosed/LSPosed) - 现代化 Xposed 框架
- [libxposed](https://github.com/libxposed/api) - Xposed API 库
- [Tokio](https://tokio.rs/) - Rust 异步运行时
- [Redis](https://redis.io/) - 数据存储

## 📧 联系方式

- Issues: [GitHub Issues](https://github.com/tiwe0/cronet-capture/issues)
- Discussions: [GitHub Discussions](https://github.com/tiwe0/cronet-capture/discussions)

---

<div align="center">
<b>⚠️ 请负责任地使用本工具，尊重他人隐私和数据安全 ⚠️</b>
</div>
