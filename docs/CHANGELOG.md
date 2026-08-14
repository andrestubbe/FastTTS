# FastTTS Version History & Changelog

All notable changes to `FastTTS` are documented in this file.

---

## [0.1.2] - 2026-08-14

### Added
- **AVX2 Audio Acceleration**: Native SIMD vector scaling for gain, pitch, and sample rate conversion.
- **Full Ecosystem Interoperability**: Updated dependency stack to `FastSIMD 0.1.3`, `FastMemory 0.1.1`, `FastPointer 0.1.1`, `FastAudioProcess 0.1.1`, `FastCore 0.1.0`.
- **Real-World Use Cases**: Added conversational voice AI and audiobook reader production examples.

### Changed
- Standardized documentation and raw GitHub showcase media embeddings.

---

## [0.1.1] - 2026-06-14

### Added
- Initial release with unified backend support for Piper (offline), Windows SAPI (system), ElevenLabs (cloud), and Deepgram (cloud).
- Off-heap audio memory buffer management.
