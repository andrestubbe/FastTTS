# The Philosophy of FastTTS

FastTTS is built on the principle that modern Java applications require a **unified interface** for text-to-speech that supports both offline and cloud backends without framework bloat.

## Core Tenets

1.  **Unified Backend Interface**
    Traditional TTS libraries force developers into platform-specific code or cloud-only solutions. FastTTS provides a single `FastTTSBackend` interface that works seamlessly with offline models (Piper), system voices (Windows SAPI), and cloud providers (ElevenLabs, Deepgram).

2.  **Offline-First Architecture**
    FastTTS prioritizes offline capabilities through Piper neural TTS models, ensuring applications can function without internet connectivity while maintaining high audio quality.

3.  **Performance Transparency**
    The framework includes built-in timing metrics (load time, synthesis time, total time) to help developers optimize their TTS pipelines and make informed backend selection decisions.

4.  **Ecosystem Consistency**
    As a core module of the **FastJava** ecosystem, FastTTS adheres to a standardized architecture:
    *   **Minimal Dependencies**: Only essential dependencies required
    *   **Clean API**: Intuitive interface for all backends
    *   **Cross-Backend Compatibility**: Seamless switching between providers

5.  **Flexibility Over Monolith**
    Unlike monolithic TTS frameworks, FastTTS allows developers to choose the right backend for their use case—system voices for quick feedback, Piper for offline quality, or cloud APIs for premium voices.

---

**⚡ FastTTS — Powering the next generation of Unified Java TTS.**

