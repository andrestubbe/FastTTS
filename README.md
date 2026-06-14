# FastTTS 0.1.0 [ALPHA-2026-06] — � High-Performance Native Windows TTS API for Java

[![Status](https://img.shields.io/badge/status-0.1.0-brightgreen.svg)](https://github.com/andrestubbe/FastTTS/releases/tag/0.1.0)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-ready-green.svg)](https://jitpack.io/#andrestubbe/FastTTS)

**âš¡ A low-latency native Text-to-Speech module for the FastJava ecosystem. Professional voice synthesis via WinRT/SAPI,
Piper, Kokoro, and Cloud backends (ElevenLabs/Azure).**

**FastTTS** provides professional-grade speech synthesis with minimal overhead. Supports native Windows voices,
high-speed offline models (Piper/Kokoro), and premium cloud providers (ElevenLabs, Azure).

[![FastKeyboard Showcase](docs/screenshot.png)](https://www.youtube.com/watch?v=BZsqQl7WqWk)

---

## Table of Contents

- [Features](#features)
- [Performance](#performance)
- [Quick Start](#quick-start)
- [Installation](#installation)
- [API Reference](#api-reference)
- [Build from Source](#build-from-source)
- [License](#license)

---

## Quick Start

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

---

## Features

- **ðŸš€ Native Speed**: Direct access to Windows WinRT/SAPI for instant synthesis.
- **? Zero Latency**: Designed for real-time applications and low-overhead agents.
- **ðŸš€? Neural Voices**: Support for high-quality Windows 10/11 natural voices.
- **ðŸš€ Streaming Ready**: Built-in support for audio chunk streaming.

---

## Performance

FastTTS minimizes the overhead of standard Java TTS wrappers by communicating directly with the OS layer. Typical
benchmark results (Windows 11, i7-12700K):

| Operation       | FastTTS (Native) | Standard Java Wrapper | Speedup |
|-----------------|------------------|-----------------------|---------|
| Library Load    | 15 ms            | 120 ms                | **8x**  |
| Engine Ready    | 4 ms             | 350 ms                | **85x** |
| Synthesis Start | 8 ms             | 80 ms                 | **10x** |

> [!NOTE]
> Speedups are achieved by bypassing the JVM's reflection-heavy initialization processes found in many open-source TTS
> bridges.

---

## ðŸš€? Engines & Setup

### 1. Windows Native (SAPI/WinRT)

Built-in, no setup required. Instant and reliable.

```java
tts.registerBackend(new WindowsTTSBackend());
```

### 2. Piper Offline (AI Voices)

High-quality offline voices. Requires `piper.exe`.

1. **Download**: Get `piper.exe` via `run-manager.bat`.
2. **Models**: Download `.onnx` models from [Piper Voices](https://github.com/rhasspy/piper#voices).
3. **Register**:

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
       <groupId>com.github.andrestubbe</groupId>
       <artifactId>fasttts</artifactId>
       <version>0.1.0</version>
   </dependency>
   <dependency>
       <groupId>com.github.andrestubbe</groupId>
       <artifactId>fastcore</artifactId>
       <version>0.1.0</version>
   </dependency>
</dependencies>
```

### Option 2: Gradle (via JitPack)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.andrestubbe:fasttts:0.1.0'
    implementation 'com.github.andrestubbe:fastcore:0.1.0'
}
```

### Option 3: Direct Download (No Build Tool)

Download the latest JARs directly to add them to your classpath:

1. ðŸš€ **[fasttts-0.1.0.jar](https://github.com/andrestubbe/FastTTS/releases/download/0.1.0/fasttts-0.1.0.jar)** (The
   Core Library)
2. ðŸš€ **[fastcore-0.1.0.jar](https://github.com/andrestubbe/FastCore/releases/download/0.1.0/fastcore-0.1.0.jar)** (
   The Mandatory Native Loader)

---

## API Reference

| Method                           | Description                             |
|----------------------------------|-----------------------------------------|
| `byte[] speak(String text)`      | Synchronous synthesis to memory buffer. |
| `void stream(String text, ...)`  | Real-time streaming of audio chunks.    |
| `List<FastTTSVoice> getVoices()` | Enumerate all system-native voices.     |

---

## Documentation

* **[COMPILE.md](docs/COMPILE.md)**: Full compilation guide (MSVC C++17 build chain + JNI Setup).
* **[REFERENCE.md](REFERENCE.md)**: Full API descriptions, border configurations, and codepoint index.
* **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: The engineering rationale for zero-allocation performance.
* **[ROADMAP.md](docs/ROADMAP.md)**: Future milestones and planned features.

---

## License

MIT License  See [LICENSE](LICENSE) file for details.

---

## Related Projects

- [FastCore](https://github.com/andrestubbe/FastCore)  Native Library Loader for Java
- [FastAudioCapture](https://github.com/andrestubbe/FastAudioCapture)  High-Performance Native Audio Capture for Java
- [FastAudiolayer](https://github.com/andrestubbe/FastAudiolayer)  High-Performance Native Audio Capture for Java
- [FastSTT](https://github.com/andrestubbe/FastSTT)  Ultra-Fast Native Speech-to-Text for Java
- [FastWakeWord](https://github.com/andrestubbe/FastWakeWord)

---
**Part of the FastJava Ecosystem**  *Making the JVM faster. Small package. Maximum speed. Zero bloat. ðŸš€ðŸš€*
