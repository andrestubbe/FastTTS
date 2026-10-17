# FastTTS Design Philosophy

`FastTTS` was architected to solve the performance, bloat, and fragmentation bottlenecks of traditional Java text-to-speech libraries.

---

## Core Architecture Principles

1. **Unified Multi-Backend Orchestration**  
   Developers interact with a single, clean `FastTTS` API, seamlessly orchestrating between Flow Matching engines (Matcha-TTS, F5-TTS), deterministic neural models (VITS), local studio neural models (Kokoro), zero-shot multilingual diffusion cloning (OmniVoice), compact INT8 CPU models (Pocket TTS), offline ONNX models (Piper), multi-speaker GGUF dialogue (Dia2), native OS voices (Windows SAPI), and cloud APIs (Hugging Face Spaces, ElevenLabs, Deepgram, OpenAI).

2. **Deterministic Lifecycle & Zero Resource Leaks**  
   All backends adhere to explicit lifecycle semantics via `AutoCloseable`, supporting pre-warming (`prepare()`) to pull model weights into the OS page cache for sub-millisecond dispatch, timeout handling, and leak-proof file and process destruction.

3. **Hardware AVX2 SIMD Acceleration**  
   Audio buffer scaling, volume modulation, pitch shifting, and sample rate conversion are accelerated natively using 256-bit AVX2 vector registers.

4. **Zero Garbage Collection Churn**  
   Transient audio buffers leverage direct off-heap memory allocations managed by **FastMemory** and **FastPointer**, preventing JVM GC pauses during real-time speech synthesis.

5. **Framework-Agnostic Minimalist Footprint**  
   Zero heavy Python runtimes, zero bloated web frameworks — lightweight native binaries, ONNX runtimes, and C++ DLL bindings designed for high-throughput, low-latency Java applications.

