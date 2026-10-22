# FastTTS Version History & Changelog

All notable changes to `FastTTS` are documented in this file.

---

## [0.1.7] - 2026-10-22

### Added
- **Orpheus TTS Backend (CanopyAI Orpheus-3B via CrispASR)**: Added `OrpheusBackend` and `OrpheusVoice` featuring Llama-3B speech-LLM inference with zero-shot voice cloning and embedded emotion control tags (e.g. `[laugh]`, `[cough]`). Built-in support for 8 preset voices (`tara`, `leah`, `jess`, `leo`, `dan`, `mia`, `zac`, `zoe`).
- **CosyVoice 3 Backend (Alibaba FunAudioLLM via cosyvoice.cpp)**: Added `CosyVoiceBackend` and `CosyVoiceVoice` featuring multilingual zero-shot voice cloning across 9 languages (EN, DE, FR, ES, ZH, JA, KO, IT, RU) via quantized GGUF CPU inference.
- **OpenAI TTS Backend Auto-Registration & Environment Discovery**: Added zero-arg constructor to `OpenAIBackend` resolving `OPENAI_API_KEY` from environment and system properties, pre-registering it directly in `FastTTS.standard()`.
- **UTF-8 Payload Hardening**: Hardened JSON serialization across `DeepgramBackend` and `ElevenLabsBackend` with explicit UTF-8 charset encoding.

## [0.1.6] - 2026-10-17

### Added
- **Core Library Voice & Model Installer**: Promoted `Installer` into the core library (`fasttts.installer.Installer`) for universal programmatic and CLI invocation without depending on example modules.
- **Standalone Engine Management**: Provides headless CLI options (`--install <engine>`, `--deinstall <engine>`, `--status`) for automated deployment pipelines.

---

## [0.1.5] - 2026-10-17

### Added
- **Matcha-TTS Flow Matching Engine**: Integrated `MatchaBackend` featuring Optimal-Transport Flow Matching neural speech via `sherpa-onnx` and Vocos neural vocoder at 22.05 kHz (**~0.09x RTF**, > 10× real-time speed).
- **VITS Deterministic Low-Latency Engine**: Integrated `VITSBackend` featuring variational inference neural speech via Piper ONNX (`amy-low`, **~0.09x RTF**).
- **F5-TTS Flow Matching Zero-Shot Cloning**: Integrated `F5TTSBackend` supporting non-autoregressive flow matching voice cloning via `audiocpp_cli.exe`.
- **Engine & Model Manager CLI Installer**: Added `Installer` (`run-installer.bat`) for interactive management, integrity verification, auto-extraction (`.tar.bz2`, `.zip`), and clean uninstallation (`--install <engine>`, `--deinstall <engine>`, `--status`).
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
