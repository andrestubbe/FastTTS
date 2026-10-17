# FastTTS Project Roadmap

The long-term development milestones for `FastTTS`.

---

## Phase 1: Core Engine & Multi-Backend (Completed - v0.1.6)
- [x] Native Windows SAPI text-to-speech JNI integration (0.03x RTF).
- [x] Matcha-TTS Flow-Matching neural speech (0.09x RTF, > 10× real-time).
- [x] VITS deterministic low-latency variational inference (0.09x RTF).
- [x] F5-TTS non-autoregressive flow-matching zero-shot voice cloning.
- [x] Kokoro Studio offline neural speech (Sherpa-ONNX runtime).
- [x] Piper ONNX offline neural TTS backend with auto-discovery.
- [x] Pocket TTS (Sherpa-ONNX INT8 offline neural CPU synthesis).
- [x] OmniVoice Zero-Shot Voice Cloning (diffusion language model, 600+ languages).
- [x] Dia2 Dialogue Backend (GGUF 2B multi-speaker scripted conversation).
- [x] Cloud API integration (Hugging Face Spaces, ElevenLabs, Deepgram, OpenAI).
- [x] Engine & Model Manager CLI Installer (`run-installer.bat`).
- [x] Lifecycle contracts (`prepare()`, `isPrepared()`, `isAvailable()`, `AutoCloseable`).
- [x] Hardware AVX2 SIMD audio vector scaling.

---

## Phase 2: Streaming & Low Latency (In Progress - v0.2.0)
- [ ] Real-time chunked audio streaming API via callbacks.
- [ ] Off-heap ring buffer audio queueing.
- [ ] Native audio.cpp-dia2 CLI integration for Dia2 GGUF inference.
- [ ] Cross-platform Linux ALSA / PulseAudio native backends.

---

## Phase 3: Advanced Voice FX & Models (Future)
- [ ] Native AVX-512 SIMD pitch shifter and formant filter.
- [ ] Direct in-memory C++ ONNX runtime binding without subprocess boundary.
- [ ] macOS AVFoundation native TTS backend.
