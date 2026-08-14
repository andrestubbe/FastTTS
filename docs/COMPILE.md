# FastTTS Compilation & Build Guide

This guide details how to compile the native C++ AVX2 shared library (`fasttts.dll`) and package the Java JAR artifact.

---

## Prerequisites

- **Windows OS**: Windows 10 / 11 (x64)
- **C++ Compiler**: Microsoft Visual Studio 2022 or 2026 Build Tools (MSVC `cl.exe` with C++17 support)
- **JDK**: Java Development Kit 17 or higher (JDK 21/25 recommended)
- **Build Tool**: Apache Maven 3.8+

---

## Native Build Instructions

1. Open a PowerShell terminal in the `FastTTS` root directory.
2. Run the native build script:
   ```cmd
   .\compile.bat
   ```
   *This compiles `native/fasttts.cpp` into `build/fasttts.dll` using `/arch:AVX2` flags and copies it to `src/main/resources/native/`.*

---

## Maven Package Assembly

Package the Java uber-JAR with pre-compiled native binaries:
```bash
mvn clean package -DskipTests
```

The resulting artifact `FastTTS-0.1.2.jar` will be generated in `target/`.
