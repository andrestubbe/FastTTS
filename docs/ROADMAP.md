# FastTTS Project Roadmap

The long-term development milestones for `FastTTS`.

---

## Phase 1: Core Engine & Multi-Backend (Completed - v0.1.2)
- [x] Native Windows SAPI text-to-speech JNI integration.
- [x] Piper ONNX offline neural TTS backend.
- [x] Cloud API integration (ElevenLabs, Deepgram, OpenAI).
- [x] Hardware AVX2 SIMD audio vector scaling.

---

## Phase 2: Streaming & Low Latency (In Progress - v0.2.0)
- [ ] Real-time chunked audio streaming API via callbacks.
- [ ] Off-heap ring buffer audio queueing.
- [ ] Cross-platform Linux ALSA / PulseAudio native backends.

---

## Phase 3: Advanced Voice FX & Models (Future)
- [ ] Native AVX-512 SIMD pitch shifter and formant filter.
- [ ] Embedded Kokoro TTS ONNX engine integration.
- [ ] macOS AVFoundation native TTS backend.
