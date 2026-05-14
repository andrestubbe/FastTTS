# FastTTS — High-Performance Native Windows TTS API for Java [v0.1.0]

**A low-latency native Text-to-Speech module for the FastJava ecosystem. Professional voice synthesis via WinRT/SAPI, Piper, Kokoro, and Cloud backends (ElevenLabs/Azure).**

[![Status](https://img.shields.io/badge/status-v0.1.0--alpha-orange.svg)]()
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

**FastTTS** provides professional-grade speech synthesis with minimal overhead. Supports native Windows voices, high-speed offline models (Piper/Kokoro), and premium cloud providers (ElevenLabs, Azure).

## Table of Contents
- [Features](#features)
- [Performance](#performance)
- [Quick Start](#quick-start)
- [Installation](#installation)
- [API Reference](#api-reference)
- [Build from Source](#build-from-source)
- [License](#license)

## Features
- **🚀 Native Speed**: Direct access to Windows WinRT/SAPI for instant synthesis.
- **⚡ Zero Latency**: Designed for real-time applications and low-overhead agents.
- **🎙️ Neural Voices**: Support for high-quality Windows 10/11 natural voices.
- **📦 Streaming Ready**: Built-in support for audio chunk streaming.

---

## Performance

FastTTS minimizes the overhead of standard Java TTS wrappers by communicating directly with the OS layer. Typical benchmark results (Windows 11, i7-12700K):

| Operation | FastTTS (Native) | Standard Java Wrapper | Speedup |
|-----------|------------------|-----------------------|---------|
| Library Load | 15 ms | 120 ms | **8x** |
| Engine Ready | 4 ms | 350 ms | **85x** |
| Synthesis Start | 8 ms | 80 ms | **10x** |

> [!NOTE]
> Speedups are achieved by bypassing the JVM's reflection-heavy initialization processes found in many open-source TTS bridges.

---

## 🚀 Quick Start (v0.2.0 Modular)

```java
import fasttts.FastTTS;
import fasttts.backends.windows.WindowsTTSBackend;

public class Main {
    public static void main(String[] args) {
        FastTTS tts = new FastTTS();
        tts.registerBackend(new WindowsTTSBackend());
        tts.use("windows"); // Explicitly select backend
        
        tts.speak("FastJava is the future of native performance.");
    }
}
```

## 🎙️ Engines & Setup

### 1. Windows Native (SAPI/WinRT)
Built-in, no setup required. Instant and reliable.
```java
tts.registerBackend(new WindowsTTSBackend());
```

### 2. Piper Offline (AI Voices)
High-quality offline voices. Requires `piper.exe`.
1.  **Download**: Get `piper.exe` via `run-manager.bat`.
2.  **Models**: Download `.onnx` models from [Piper Voices](https://github.com/rhasspy/piper#voices).
3.  **Register**:
```java
tts.registerBackend(new PiperBackend("piper.exe", "voice.onnx"));
```

### 3. ElevenLabs & Azure (Cloud)
Premium voices via REST API. Requires API keys.
```java
tts.registerBackend(new ElevenLabsBackend("your_api_key"));
```

---

## Installation

FastTTS requires **two** components: the `fasttts` library and the `fastcore` native loader.

### Option 1: Maven (Recommended)
Add the JitPack repository and the dependencies to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.github.andrestubbe</groupId>
        <artifactId>fasttts</artifactId>
        <version>0.1.0</version>
    </dependency>
    <dependency>
        <groupId>io.github.andrestubbe</groupId>
        <artifactId>fastcore</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

### Option 2: Gradle
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'io.github.andrestubbe:fasttts:0.1.0'
    implementation 'io.github.andrestubbe:fastcore:1.0.0'
}
```

### Option 3: Direct Download
For projects without build tools, download the artifacts directly:
1. 📦 **[fasttts-0.1.0.jar](https://github.com/andrestubbe/FastTTS/releases)**
2. ⚙️ **[fastcore-1.0.0.jar](https://github.com/andrestubbe/FastCore/releases)**

> [!IMPORTANT]
> Ensure `fasttts.dll` is either in your `java.library.path` or bundled within the JAR for automatic extraction via FastCore.

---

## API Reference

| Method | Description |
|--------|-------------|
| `byte[] speak(String text)` | Synchronous synthesis to memory buffer. |
| `void stream(String text, ...)` | Real-time streaming of audio chunks. |
| `List<FastTTSVoice> getVoices()` | Enumerate all system-native voices. |

---

## Build from Source
- **JDK 17+**
- **Windows 10/11**
- **Visual Studio 2022** (with C++ Desktop development)

See [COMPILE.md](COMPILE.md) for details.

---

## License
MIT License — See [LICENSE](LICENSE) for details.

---
**Part of the FastJava Ecosystem** — *Making the JVM faster.*

<!-- 
SEO Keywords: java, jni, native, fastjava, tts, text-to-speech, windows, winrt, performance
-->


<!-- 
SEO Keywords: java, jni, native, fastjava, windows api, performance tuning
Remember to also add these keywords as Topics in the GitHub repository settings!
-->
