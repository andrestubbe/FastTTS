# FastTTS Design Philosophy

`FastTTS` was architected to solve the performance and fragmentation bottlenecks of traditional Java text-to-speech libraries.

---

## Core Architecture Principles

1. **Unified Orchestration Interface**  
   Developers write code against a single clean `FastTTS` interface, seamlessly switching between offline local models (Piper), system voices (Windows SAPI), and high-quality cloud APIs.

2. **Hardware AVX2 SIMD Acceleration**  
   Audio buffer scaling, pitch shifting, and sample rate conversion are executed directly in native C++ using 256-bit AVX2 vector registers.

3. **Zero Garbage Collection Overhead**  
   Transient audio buffers use direct off-heap memory allocations managed by **FastMemory** and **FastPointer**, eliminating JVM GC pauses during real-time speech synthesis.

4. **Framework-Agnostic Minimalist Footprint**  
   Zero heavy Python dependencies, zero bloated web wrappers — lightweight native C++ DLL bindings for high-throughput Java applications.
