# Cronet Capture

<div align="center">

**A Reverse Engineering Toolkit for Capturing, Forwarding, and Analyzing Android Cronet Network Requests**

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Rust](https://img.shields.io/badge/rust-1.70%2B-orange.svg)](https://www.rust-lang.org/)
[![Android](https://img.shields.io/badge/android-8.0%2B-green.svg)](https://developer.android.com/)

English | [简体中文](README.md)

</div>

---

> [!CAUTION]
> ## ⚠️ DISCLAIMER
> 
> **This project is for educational, research, and technical exchange purposes only. Any illegal use is strictly prohibited.**
> 
> - **Research Nature**: This project is a result of reverse engineering research, designed to learn network protocols and Android Hook techniques
> - **Educational Purpose**: The code is for teaching and technical discussion only, helping to understand network interception and protocol design
> - **No Abuse**: Strictly prohibited for unauthorized data interception, theft, or any illegal activities
> - **Legal Responsibility**: Users must comply with local laws and regulations. Any legal consequences arising from misuse are the user's sole responsibility
> - **Security Risks**: This project does not implement complete security mechanisms. **Not recommended for production environments**
> - **Privacy Protection**: Do not use this project to process data containing sensitive information or personal privacy
> 
> **By using this project, you acknowledge that you have read, understood, and agreed to the above terms. If you disagree, please stop using immediately.**

---

## 📖 Project Overview

Cronet Capture is a complete Android network request capture solution specifically designed for applications using the Google Cronet network library. This project hooks the Cronet API through the Xposed framework to capture network requests and response data in real-time, then forwards them to a backend server via a custom protocol for storage and analysis.

### 🎯 Core Features

- ✅ **Cronet Hook**: Modern Hook implementation based on LibXposed API
- ✅ **High-Performance Forwarding**: Zero-copy data forwarding with Rust + JNI, no lag on mobile devices
- ✅ **Custom Protocol**: Binary protocol based on Magic Number, supporting streaming transmission
- ✅ **Async Processing**: Tokio async runtime with excellent performance in high-concurrency scenarios
- ✅ **Redis Storage**: Data persistence to Redis Stream for easy analysis
- ✅ **Routing Table**: Flexible regex-based route matching and forwarding
- ✅ **Hot Reload**: Configuration file hot reload without service restart

### 📦 Usage

1. Ensure your phone has LSPosed framework installed
2. Download and install CronetForward_1.0.apk
3. Activate the plugin in LSPosed and enable it for target applications
4. Open CronetForward frontend configuration page to set up forwarding, deobfuscation, etc.
5. Compile and start CronetReceiver
6. Open the application, all traffic through the Cronet network library will be captured.

## 🏗️ Architecture Design

```
┌─────────────────────────────────────────────────────────────────┐
│                         Android Device                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                  Target App (Hooked)                      │   │
│  │                         ↓                                  │   │
│  │                   Cronet Library                          │   │
│  │                         ↓                                  │   │
│  │  ┌──────────────────────────────────────────────────┐   │   │
│  │  │          Xposed Module (MainModule.java)          │   │   │
│  │  │  • Hook onReadCompleted()                         │   │   │
│  │  │  • Hook onSucceeded()                             │   │   │
│  │  │  • Extract URL + Response Body                    │   │   │
│  │  └──────────────────┬───────────────────────────────┘   │   │
│  │                     ↓ (JNI Call)                         │   │
│  │  ┌──────────────────────────────────────────────────┐   │   │
│  │  │    Native Layer (cronet-forward/lib.rs)           │   │   │
│  │  │  • Encapsulate custom protocol                    │   │   │
│  │  │  • Async send to server                           │   │   │
│  │  └──────────────────┬───────────────────────────────┘   │   │
│  └────────────────────│────────────────────────────────────┘   │
└────────────────────────│─────────────────────────────────────┘
                         ↓ (TCP Protocol)
┌─────────────────────────────────────────────────────────────────┐
│                    Server Side (Cronet Receiver)                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              TCP Server (main.rs)                         │  │
│  │  • Receive and parse custom protocol packets             │  │
│  │  • Handle TCP sticky packets (ProtocolCodec)             │  │
│  │  • Data assembly (chunked/single packet)                 │  │
│  └──────────────────┬───────────────────────────────────────┘  │
│                     ↓                                            │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         Routing Table + Redis Storage (redis.rs)          │  │
│  │  • Regex-based route matching                             │  │
│  │  • Async write to Redis Stream                            │  │
│  └──────────────────┬───────────────────────────────────────┘  │
└────────────────────│─────────────────────────────────────────┘
                      ↓
              ┌─────────────┐
              │    Redis    │
              │   Storage   │
              └─────────────┘
```

## 📦 Project Structure

```
cronet-capture/
├── xposed/                    # Android Xposed Module
│   ├── app/src/main/java/cafe/ivory/cronet/
│   │   ├── MainModule.java    # Hook core logic
│   │   ├── Config*.java       # Configuration management
│   │   └── MainActivity.java  # UI interface
│   └── build.gradle.kts       # Gradle build config
│
├── libs/
│   ├── cronet-forward/        # Rust JNI forwarding library
│   │   ├── src/lib.rs         # JNI interface implementation
│   │   └── docs/protocol.md   # Protocol documentation
│   │
│   └── cronet-receiver/       # Rust TCP server
│       ├── src/
│       │   ├── main.rs        # Server main program
│       │   ├── protocol.rs    # Protocol parsing
│       │   ├── redis.rs       # Redis storage
│       │   └── config.rs      # Configuration management
│       ├── config.json        # Server configuration
│       └── test/test_protocol.py  # Protocol test client
│
└── README.md                  # This document
```

## 🚀 Quick Start

### Prerequisites

- **Android Device**: Root permission + LSPosed/EdXposed framework
- **Development Environment**:
  - Rust 1.70+ (recommended 1.80+)
  - Android NDK r25+
  - JDK 17+
  - Redis 5.0+

### 1️⃣ Build Native Forwarding Library

```bash
cd libs/cronet-forward

# Install Android targets
rustup target add aarch64-linux-android
rustup target add armv7-linux-androideabi

# Configure NDK path
export ANDROID_NDK_HOME=/path/to/ndk

# Build for ARM64
cargo ndk build --release --target aarch64-linux-android

# Copy generated .so file to Xposed project
cp target/aarch64-linux-android/release/libcronet_forward.so \
   ../../xposed/app/src/main/jniLibs/arm64-v8a/
```

### 2️⃣ Build and Install Xposed Module

```bash
cd xposed

# Build APK
./gradlew assembleRelease

# Install to device
adb install app/build/outputs/apk/release/app-release.apk
```

In LSPosed Manager:
1. Enable Cronet Capture module
2. Select target application (e.g., XiaoHongShu/RedNote)
3. Restart target application

### 3️⃣ Configure and Run Receiver Server

```bash
cd libs/cronet-receiver

# Edit configuration file
vim config.json
```

Example configuration:
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

Start server:
```bash
cargo run --release
```

### 4️⃣ Verify Operation

```bash
# View Redis data
redis-cli

# View all Streams
SCAN 0 MATCH *_stream

# Read captured data
XREAD COUNT 10 STREAMS api_stream 0
```

## 📋 Custom Protocol Format

```
┌────────────┬──────────────┬─────────────┬──────────────┬──────────┬────────────┬──────────┬──────────┐
│ MagicNumber│ TagPackageLen│  TagUrlLen  │ PayloadLen   │ EndFlag  │ TagPackage │  TagUrl  │ Payload  │
│    4 B     │     2 B      │    2 B      │     4 B      │   4 B    │   M bytes  │ N bytes  │ K bytes  │
└────────────┴──────────────┴─────────────┴──────────────┴──────────┴────────────┴──────────┴──────────┘
```

### Field Description

| Field | Length | Type | Description |
|-------|--------|------|-------------|
| MagicNumber | 4B | Fixed | Magic number for protocol validation (e.g., "i0v0") |
| TagPackageLen | 2B | u16 | TagPackage field length (big-endian) |
| TagUrlLen | 2B | u16 | TagUrl field length (big-endian) |
| PayloadLen | 4B | u32 | Payload field length (big-endian) |
| EndFlag | 4B | u32 | End flag: 0=continue, non-zero=last packet |
| TagPackage | Variable | UTF-8 | Application package name (e.g., "cafe.ivory.love") |
| TagUrl | Variable | UTF-8 | Request URL |
| Payload | Variable | bytes | Response data (usually JSON) |

Detailed protocol documentation:
- [cronet-forward/docs/protocol.md](libs/cronet-forward/docs/protocol.md)
- [cronet-receiver/docs/protocol.md](libs/cronet-receiver/docs/protocol.md)

## 🔧 Configuration

### Xposed Module Configuration

Edit [MainModule.java](xposed/app/src/main/java/cafe/ivory/cronet/MainModule.java):

```java
// Target application package name
public static String packageName = "cafe.ivory.love";

// Magic number (must match server configuration)
public static String magicNumber = "i0v0";

// Server address
public static String host = "192.168.1.100";
public static int port = 9000;

// Obfuscated class names (adjust according to actual app)
public static String callbackClassName = "kj5.g";
public static String urlRequestClassName = "org.chromium.net.h0";
// ...
```

### Server Configuration

[config.json](libs/cronet-receiver/example.config.json) configuration options:

```json
{
    "auth_key": "Authentication key (reserved feature)",
    "listener": {
        "host": "Listen address",
        "port": Listen port
    },
    "redis": {
        "host": "Redis address",
        "port": Redis port
    },
    "route_table": {
        "route_name": {
            "pattern": "URL regex pattern",
            "target_stream": "Target Stream name"
        }
    }
}
```

## 🧪 Testing

```bash
# Test protocol client
cd libs/cronet-receiver/test
python3 test_protocol.py
```

## 🛠️ Development Guide

### Adding New Hook Points

You can directly add configurations in the Xposed plugin's frontend page.

Or modify the Java hook corresponding source code in Xposed.

### Modifying Protocol Format

1. Synchronize modifications in `cronet-forward/src/lib.rs` and `cronet-receiver/src/protocol.rs`
2. After updating magic number or field order, rebuild and redeploy
3. Use `test_protocol.py` to verify compatibility

### Custom Routing Rules

Add routes in `config.json`:

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

## 🐛 FAQ

<details>
<summary><b>Q: Why is no data being captured?</b></summary>

1. Confirm Xposed module is activated and restart target app
2. Check if `packageName` is correct
3. Verify server address and port configuration
4. Use `adb logcat` to check logs:
   ```bash
   adb logcat | grep -E "NativeTransport|CronetCapture"
   ```
</details>

<details>
<summary><b>Q: JNI library loading failed?</b></summary>

1. Confirm `.so` file is in correct ABI directory (e.g., `arm64-v8a/`)
2. Check library filename: `libcronet_forward.so`
3. Ensure compiled target architecture matches device
</details>

<details>
<summary><b>Q: Server not receiving data?</b></summary>

1. Check firewall and port availability
2. Confirm Magic Number configuration is consistent
3. Use Wireshark to verify network connection
</details>

## 📝 License

This project is open-sourced under the [MIT License](LICENSE).

## 🙏 Acknowledgments

- [LSPosed](https://github.com/LSPosed/LSPosed) - Modern Xposed framework
- [libxposed](https://github.com/libxposed/api) - Xposed API library
- [Tokio](https://tokio.rs/) - Rust async runtime
- [Redis](https://redis.io/) - Data storage

## 📧 Contact

- Issues: [GitHub Issues](https://github.com/tiwe0/cronet-capture/issues)
- Discussions: [GitHub Discussions](https://github.com/tiwe0/cronet-capture/discussions)

---

<div align="center">
<b>⚠️ Please use this tool responsibly and respect others' privacy and data security ⚠️</b>
</div>
