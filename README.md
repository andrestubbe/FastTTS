# FastTTS 0.1.2 [ALPHA-2026-08] — Unified, Zero-Bloat TTS Backend Orchestration for Java

[![Status](https://img.shields.io/badge/status-0.1.2-brightgreen.svg)](https://github.com/andrestubbe/FastTTS/releases/tag/0.1.2)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-0.1.2-green.svg)](https://jitpack.io/#andrestubbe/FastTTS)

---

**⚡ Connect multiple TTS backends with a unified interface — Minimalist text-to-speech orchestration supporting offline and cloud providers.**

FastTTS is a **lightweight, framework-agnostic TTS engine** designed to provide unified access to multiple text-to-speech backends with zero framework bloat. It supports **Piper (offline)**, **Windows SAPI (system)**, **ElevenLabs (cloud)**, and **Deepgram (cloud)** through a single clean API.

---

[![Showcase](https://raw.githubusercontent.com/andrestubbe/FastTTS/main/docs/screenshot.png)](https://youtu.be/PyTXlm9bfxc)

---

## Quick Start — Example

```java
import fasttts.FastTTS;
import fasttts.backends.piper.PiperBackend;
import fasttts.backends.windows.WindowsTTSBackend;
import fasttts.backends.elevenlabs.ElevenLabsBackend;
import fasttts.backends.deepgram.DeepgramBackend;
import fasttts.core.FastTTSAudio;

public class Demo {
    public static void main(String[] args) throws Exception {
        FastTTS tts = new FastTTS();
        
        // Windows SAPI (no setup required)
        tts.registerBackend(new WindowsTTSBackend());
        FastTTSAudio audio = tts.speak("Hello World");
        
        // Piper (offline, requires piper.exe and models)
        tts.registerBackend(new PiperBackend("piper.exe", "models/de_DE-thorsten-medium.onnx"));
        FastTTSAudio germanAudio = tts.speak("Hallo Welt");
        
        // ElevenLabs (cloud, requires API key)
        tts.registerBackend(new ElevenLabsBackend("your-api-key"));
        FastTTSAudio cloudAudio = tts.speak("Hello World");
        
        // Deepgram (cloud, requires API key)
        tts.registerBackend(new DeepgramBackend("your-api-key"));
        FastTTSAudio deepgramAudio = tts.speak("Hello World");
    }
}
```

---

## Table of Contents

- [Why FastTTS?](#why-fasttts)
- [Key Features](#key-features)
- [Real-World Use Cases](#real-world-use-cases)
- [Performance Benchmarks](#performance-benchmarks)
- [Architecture Overview](#architecture-overview)
- [API Quick Reference](#api-quick-reference)
- [Installation](#installation)
- [Backend Setup](#backend-setup)
- [Documentation](#documentation)
- [Platform Support](#platform-support)
- [License](#license)
- [Related Projects](#related-projects)

---

## Why FastTTS?

Traditional TTS libraries force developers into heavyweight Python dependencies, complex cloud API integrations, or platform-specific code. `FastTTS` provides:

- **100% Native JVM Pipeline** — Orchestrates multiple TTS backends in a single JVM process with unified interface.
- **Offline and Cloud Support** — Seamlessly switch between local models (Piper) and cloud providers (ElevenLabs, Deepgram).
- **Model Agnostic** — Works with offline ONNX models, system voices, and cloud APIs through the same `FastTTSBackend` interface.
- **Zero Configuration Overlap** — Integrates seamlessly with existing FastJava ecosystem libraries.

---

## Key Features

---

## Real-World Use Cases

- 🗣️ **Conversational Voice AI Assistants**: Low-latency neural speech synthesis for AI chatbots and desktop voice assistants.
- 📖 **Audiobook & Content Reader**: High-speed offline speech synthesis for document reading and accessibility tools.
- 📢 **In-App & Game Audio Notifications**: Real-time voice announcements with zero Garbage Collection latency impact.
- 🌐 **Multi-Language Accessibility Engines**: Seamlessly switch between local Piper voices and cloud APIs (ElevenLabs, Deepgram).

---

## Key Features

---

## Real-World Use Cases

- 🗣️ **Conversational Voice AI Assistants**: Low-latency neural speech synthesis for AI chatbots and desktop voice assistants.
- 📖 **Audiobook & Content Reader**: High-speed offline speech synthesis for document reading and accessibility tools.
- 📢 **In-App & Game Audio Notifications**: Real-time voice announcements with zero Garbage Collection latency impact.
- 🌐 **Multi-Language Accessibility Engines**: Seamlessly switch between local Piper voices and cloud APIs (ElevenLabs, Deepgram).

* **🎭 Multiple Backend Support** — Unified interface for Piper (offline), Windows SAPI (system), ElevenLabs (cloud), and Deepgram (cloud).
* **📱 Offline Capable** — Run TTS locally with Piper models without internet connection.
* **☁️ Cloud Integration** — Access high-quality cloud voices from ElevenLabs and Deepgram.
* **⚡ Performance Focused** — Built for low-latency synthesis with detailed timing metrics.
* **🔌 Simple API** — Clean, intuitive interface for text-to-speech synthesis.

---

## Performance Benchmarks

`FastTTS` is built for high-performance text-to-speech synthesis. Based on the built-in demo timing metrics:

```text
Backend          Load Time    Synth Time    Total Time
Windows SAPI     104 ms       151 ms        255 ms
Piper (offline)  0 ms         1761 ms       1761 ms
```

> **Windows SAPI** provides the fastest synthesis (255ms total) for quick feedback, while **Piper** offers higher quality offline synthesis (1761ms total) with no internet dependency.

---

## Architecture Overview

**Windows SAPI Backend**  
System-native text-to-speech using Windows Speech API (SAPI). No external dependencies required.

**Piper Backend**  
Offline TTS using the Piper neural TTS engine. Requires `piper.exe` and ONNX model files for local synthesis.

**ElevenLabs Backend**  
Cloud-based TTS accessing ElevenLabs high-quality voices. Requires API key and internet connection.

**Deepgram Backend**  
Cloud-based TTS using Deepgram's fast synthesis API. Requires API key and internet connection.

**FastTTS (This Library — The Orchestration Layer)**  
Higher-level TTS framework that provides a unified interface for all backends, allowing seamless switching between offline and cloud providers.

---

## API Quick Reference

| Method | Description | Backend |
|--------|-------------|---------|
| `registerBackend(FastTTSBackend)` | Registers a TTS backend with the orchestrator. | All |
| `speak(String)` | Synthesizes text to audio using the active backend. | All |
| `speak(String, String, FastTTSVoice, FastTTSConfig)` | Synthesizes text with specific backend and configuration. | All |
| `use(String)` | Sets the active backend by name. | All |
| `getVoices()` | Returns available voices for all registered backends. | All |

---

## Installation

### Option 1: Maven (Recommended)

Add the JitPack repository and the complete dependency stack to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <!-- FastTTS Engine -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastTTS</artifactId>
        <version>0.1.2</version>
    </dependency>

    <!-- FastSIMD Hardware Vector Acceleration Engine -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastSIMD</artifactId>
        <version>0.1.3</version>
    </dependency>

    <!-- FastMemory Aligned Allocator -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastMemory</artifactId>
        <version>0.1.1</version>
    </dependency>

    <!-- FastPointer Address Wrapper -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastPointer</artifactId>
        <version>0.1.1</version>
    </dependency>

    <!-- FastAudioProcess Audio Engine -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastAudioProcess</artifactId>
        <version>0.1.1</version>
    </dependency>

    <!-- FastCore Unified JNI Loader -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastCore</artifactId>
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
    implementation 'com.github.andrestubbe:FastTTS:0.1.2'
    implementation 'com.github.andrestubbe:FastSIMD:0.1.3'
    implementation 'com.github.andrestubbe:FastMemory:0.1.1'
    implementation 'com.github.andrestubbe:FastPointer:0.1.1'
    implementation 'com.github.andrestubbe:FastAudioProcess:0.1.1'
    implementation 'com.github.andrestubbe:FastCore:0.1.0'
}
```

---

## Backend Setup

### Windows SAPI (System TTS)
- **No installation required** — uses Windows built-in voices
- Works immediately after FastTTS installation
- Multiple voices available (system default)

### Piper (Offline TTS)
- **Download Piper:** https://github.com/rhasspy/piper/releases
- Extract Piper to a directory (e.g., `C:\Piper\`)
- Set environment variable: `set PIPER_PATH=C:\Piper\piper.exe`
- Or copy `piper.exe` to your project directory
- Download models from: https://huggingface.co/models?search=piper
- Place model files in `models/` folder

**Available Models:**
- `de_DE-thorsten-medium` - German male voice
- `en_US-lessac-medium` - US English male
- `en_US-amy-medium` - US English female

### ElevenLabs (Cloud TTS)
- Requires API key from: https://elevenlabs.io
- High-quality voices
- Cloud-based (requires internet)

### Deepgram (Cloud TTS)
- Requires API key from: https://deepgram.com
- Fast cloud-based synthesis
- Multiple voice options

---

## Documentation

* **[REFERENCE.md](docs/REFERENCE.md)**: Core API reference manual.
* **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: Unified TTS pipeline design goals.
* **[COMPILE.md](docs/COMPILE.md)**: Maven build instructions.
* **[CHANGELOG.md](docs/CHANGELOG.md)**: Project history.
* **[ROADMAP.md](docs/ROADMAP.md)**: Future development goals.

---

## Platform Support

| Platform | Status |
|----------|--------|
| Windows 10/11 (x64) | ✅ Fully Supported |
| Linux | 🚧 Planned |
| macOS | 🚧 Planned |

---

## License

MIT License — See [LICENSE](LICENSE) file for details.

---

## Related Projects

- [FastCore](https://github.com/andrestubbe/FastCore) — Native JNI loader for FastJava libraries
- [FastAudioPlayer](https://github.com/andrestubbe/FastAudioPlayer) — Native audio playback for Java via WASAPI
- [FastAI](https://github.com/andrestubbe/fastai) — Unified lightweight AI model client interface
- [FastAIModel](https://github.com/andrestubbe/FastAIModel) — Embedded GGUF and ONNX runtimes for local feature embeddings
- [FastAIRag](https://github.com/andrestubbe/FastAIRag) — Unified, zero-bloat RAG pipeline client for Java
- [FastAIVectorDB](https://github.com/andrestubbe/FastAIVectorDB) — High-speed native C++ SIMD vector database
- [FastContentParse](https://github.com/andrestubbe/FastContentParse) — Standardized Java document parser for text extraction
- [FastContentChunk](https://github.com/andrestubbe/FastContentChunk) — High-performance native SIMD tokenizer and chunker

---

Part of the FastJava Ecosystem — Making the JVM faster. Small package. Maximum speed. Zero bloat. 🚀📋
