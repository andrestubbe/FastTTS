# The Philosophie of FastTTS

FastTTS is built on the principle that modern Java applications require a **native-first** approach to speech synthesis that bypasses the latency of standard libraries.

## Core Tenets

1.  **Zero Latency Execution**
    Traditional Java TTS wrappers often suffer from slow initialization and high call latency. FastTTS interacts directly with the **WinRT Speech API** to ensure instant response times.

2.  **Streaming by Default**
    FastTTS is designed for high-performance agents. It supports streaming audio chunks so that speech can begin before the entire sentence has been processed.

3.  **Direct Hardware Access**
    By utilizing JNI and native Windows components, FastTTS ensures optimal CPU and GPU utilization for neural voices, providing a premium acoustic experience.

4.  **Blueprint Consistency**
    As a core module of the **FastJava** ecosystem, FastTTS adheres to a standardized architecture:
    *   **Native Backend**: Direct C++/JNI implementation.
    *   **Unified Loading**: Powered by `FastCore` for seamless extraction.

---
**⚡ FastTTS — Powering the next generation of Native Java.**

