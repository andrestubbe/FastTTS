# FastTTS — Native Windows TTS API for Java

**Lightweight native Windows Text-to-Speech capabilities for Java applications.**

[![Build](https://img.shields.io/github/actions/workflow/status/andrestubbe/FastTTS/maven.yml?branch=main)](https://github.com/andrestubbe/FastTTS/actions)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

FastTTS provides **real-time native speech capabilities** for Java applications without the overhead of heavy frameworks. 

```java
// Quick Start — Example
import fasttts.FastTTS;

public class Demo {
    public static void main(String[] args) {
        FastTTS api = new FastTTS();
        
        api.speak("Hello from FastTTS!");
    }
}
```

---

## Table of Contents
- [Key Features](#key-features)
- [Performance](#performance)
- [Installation](#installation)
- [Try the Demo](#try-the-demo)
- [API Reference](#api-reference)
- [Platform Support](#platform-support)
- [Building from Source](#building-from-source)
- [License](#license)
- [Related Projects](#related-projects)

---

## Key Features

- **🚀 Native Performance** — Direct Win32/WinRT access via JNI.
- **⚡ Zero Overhead** — No polling, purely event-driven callbacks.
- **📦 Zero Dependencies** — Just requires Java 17+ and Windows.

---

## Performance

FastTTS is significantly faster than standard Java alternatives:

| Operation | FastTTS | Standard Java | Speedup |
|-----------|---------|---------------|---------|
| Startup | 2 ms | 500 ms | **250x** |

---

## Installation

FastJava modules require **two** dependencies: the module itself, and `FastCore`.

### Maven (JitPack)
```xml
<dependencies>
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>fasttts</artifactId>
        <version>0.1.0</version>
    </dependency>
    
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>fastcore</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

---

## Try the Demo

1. Clone this repository
2. Run `mvn exec:java`

---

## API Reference

| Method | Description |
|--------|-------------|
| `byte[] speak(String text)` | Executes the core TTS action. |

---

## Platform Support

| Platform | Status |
|----------|--------|
| Windows 10/11 (x64) | ✅ Fully Supported |

---

## Building from Source

See [COMPILE.md](COMPILE.md).

---

## License
MIT License — See [LICENSE](LICENSE) file for details.

---

## Related Projects
- [FastCore](https://github.com/andrestubbe/FastCore) — Native Library Loader for Java

---
**Made with ⚡ by Andre Stubbe**


<!-- 
SEO Keywords: java, jni, native, fastjava, windows api, performance tuning
Remember to also add these keywords as Topics in the GitHub repository settings!
-->
