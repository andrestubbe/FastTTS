# FastTTS 0.1.2 [ALPHA-2026-08] [ALPHA-2026-08] — High-Performance Native Audio Engine for Java

[![Status](https://img.shields.io/badge/status-0.1.2-brightgreen.svg)](https://github.com/andrestubbe/FastTTS/releases/tag/0.1.2)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-0.1.2-green.svg)](https://jitpack.io/#andrestubbe/FastTTS)

---

**⚡ Hardware SIMD AVX2-accelerated native audio processing engine for Java.**

![Showcase](https://raw.githubusercontent.com/andrestubbe/FastTTS/main/docs/screenshot.png)

---

## Quick Start — Example

```java
import fasttts.*;

public class Demo {
    public static void main(String[] args) {
        System.out.println("FastTTS 0.1.2 initialized.");
    }
}
```

---

## Table of Contents

- [Why FastTTS?](#why-fasttts)
- [Key Features](#key-features)
- [Real-World Use Cases](#real-world-use-cases)
- [Performance Benchmarks](#performance-benchmarks)
- [API Reference](#api-reference)
- [Installation](#installation)
- [Documentation](#documentation)
- [License](#license)

---

## Key Features

* **⚡ AVX2 SIMD Audio Vector Acceleration** — 256-bit SIMD registers for low-latency audio processing.
* **💾 Off-Heap Zero-GC Memory** — Operates outside JVM Garbage Collection heap limits.
* **⚡ Full FastJava Interoperability** — Integrates seamlessly with **[FastAudioProcess](https://github.com/andrestubbe/FastAudioProcess)** and **[FastSIMD](https://github.com/andrestubbe/FastSIMD)**.

---

## Installation

```xml
<dependency>
    <groupId>com.github.andrestubbe</groupId>
    <artifactId>FastTTS</artifactId>
    <version>0.1.2</version>
</dependency>
```

---

## Documentation

- **[CHANGELOG.md](docs/CHANGELOG.md)**: Release notes.
- **[COMPILE.md](docs/COMPILE.md)**: Compilation guide.
- **[REFERENCE.md](docs/REFERENCE.md)**: Full API contracts.
- **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: Design principles.
- **[ROADMAP.md](docs/ROADMAP.md)**: Future goals.

---

## License

MIT License — See [LICENSE](LICENSE) file for details.
