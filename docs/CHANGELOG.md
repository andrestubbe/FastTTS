# FastTTS Version History & Changelog

All notable changes to `FastTTS` are documented in this file.

---

## [0.1.6] - 2026-10-17

### Added
- **Core Library Voice & Model Installer**: Promoted `FastTTSInstaller` into the core library (`fasttts.installer.FastTTSInstaller`) for universal programmatic and CLI invocation without depending on example modules.
- **Standalone Engine Management**: Provides headless CLI options (`--install <engine>`, `--deinstall <engine>`, `--status`) for automated deployment pipelines.

---

## [0.1.5] - 2026-10-17

### Added
- **Matcha-TTS Flow Matching Engine**: Integrated `MatchaBackend` featuring Optimal-Transport Flow Matching neural speech via `sherpa-onnx` and Vocos neural vocoder at 22.05 kHz (**~0.09x RTF**, > 10× real-time speed).
- **VITS Deterministic Low-Latency Engine**: Integrated `VITSBackend` featuring variational inference neural speech via Piper ONNX (`amy-low`, **~0.09x RTF**).
- **F5-TTS Flow Matching Zero-Shot Cloning**: Integrated `F5TTSBackend` supporting non-autoregressive flow matching voice cloning via `audiocpp_cli.exe`.
- **Engine & Model Manager CLI Installer**: Added `FastTTSInstaller` (`run-installer.bat`) for interactive management, integrity verification, auto-extraction (`.tar.bz2`, `.zip`), and clean uninstallation (`--install <engine>`, `--deinstall <engine>`, `--status`).
- **Benchmark Expansion**: Expanded benchmark suite with Matcha-TTS, VITS, and F5-TTS metrics.

---

## [0.1.4] - 2026-10-17

### Added
- **Kokoro Studio Engine**: Integrated `KokoroBackend` powered by Sherpa-ONNX (`models/kokoro/kokoro-en-v0_19`) featuring studio-grade voices (`af_bella`, `am_adam`, `bf_emma`, `bm_george`).
- **OmniVoice Zero-Shot Cloning**: Added `OmniVoiceBackend` supporting 600+ languages, zero-shot voice cloning from reference audio, and diffusion step configuration via `FastTTSConfig`.
- **Dia2 Multi-Speaker Dialogue**: Added `Dia2Backend` supporting GGUF quantized models (`dia2-2b-q4_k.gguf`), multi-speaker dialogue scripts (`[S1]`, `[S2]`), and memory page caching.
- **Hugging Face Spaces & Inference API**: Added `HuggingFaceBackend` with fallback between Serverless Inference API and Gradio Space endpoints.
- **Lifecycle Management**: Extended `FastTTSBackend` with `prepare()`, `isPrepared()`, `isAvailable()`, and `close()` (`AutoCloseable`). `FastTTS` orchestrator now implements `AutoCloseable` with complete resource teardown.
- **Multi-Engine Benchmark Suite**: Added comprehensive latency, throughput, and Real-Time Factor (RTF) benchmark suite with automated WASAPI playback via `FastAudioPlayer`.

### Fixed
- **Process Safety & Timeout Handling**: Added enforced 60s/300s process timeouts and process destruction on hang across all external binary runners (`piper`, `pocket`, `omnivoice`, `dia2`).
- **Zero Resource Leaks**: Guaranteed deletion of temporary `.wav` files via strict `try-finally` blocks.
- **Control Character JSON Escaping**: Hardened JSON serialization in `HuggingFaceBackend`, `OpenAIBackend`, `ElevenLabsBackend`, and `DeepgramBackend` against raw control characters and quotes.
- **PocketTTS Availability**: Implemented explicit binary and model existence verification in `PocketTTSBackend.isAvailable()`.

---

## [0.1.3] - 2026-10-10

### Added
- **Pocket TTS Integration**: Added `PocketTTSBackend` powered by Sherpa-ONNX offline CPU neural engine (`sherpa-onnx-offline-tts.exe` with INT8 quantized models).
- **Auto-Resolution Architecture**: Automated discovery for Pocket TTS binary paths and model directories in workspace `bin/pocket/` / `models/pocket/` as well as user home `.speak/`.

### Removed
- **EdgeTTS Backend**: Removed `EdgeTTSBackend` in favor of self-contained offline neural Pocket TTS.

---

## [0.1.2] - 2026-08-14

### Added
- **AVX2 Audio Acceleration**: Native SIMD vector scaling for gain, pitch, and sample rate conversion.
- **Piper Local Binary Auto-Resolution**: Enhanced `PiperBackend` with automatic discovery in `bin/piper/piper.exe` and `~/.speak/models/piper`.
- **Full Ecosystem Interoperability**: Updated dependency stack to `FastSIMD 0.1.3`, `FastMemory 0.1.1`, `FastPointer 0.1.1`, `FastAudioProcess 0.1.1`, `FastCore 0.1.0`.
- **Real-World Use Cases**: Added conversational voice AI and audiobook reader production examples.

### Changed
- Standardized documentation and raw GitHub showcase media embeddings.
- Zero-copy audio synthesis buffering for native WASAPI pipeline interoperability.

---

## [0.1.1] - 2026-06-14

### Added
- Initial release with unified backend support for Piper (offline), Windows SAPI (system), ElevenLabs (cloud), and Deepgram (cloud).
- Off-heap audio memory buffer management.
