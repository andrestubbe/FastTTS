# FastTTS API Reference Manual

`FastTTS` provides high-performance native Windows SAPI text-to-speech, Kokoro Studio & Piper ONNX offline neural TTS synthesis, Pocket TTS Sherpa-ONNX INT8 CPU inference, OmniVoice zero-shot diffusion voice cloning, Dia2 GGUF dialogue, and cloud API integration for Java applications.

---

## 1. FastTTS Engine API

### `FastTTS.standard()`
```java
public static FastTTS standard()
```
Returns a fully configured orchestrator with auto-discovered local and cloud backends pre-registered (`windows`, `kokoro`, `piper`, `pocket`, `omnivoice`, `dia2`, `huggingface`).

---

### `registerBackend`
```java
public void registerBackend(FastTTSBackend backend)
```
Registers a TTS backend implementation.

---

### `speak`
```java
public FastTTSAudio speak(String text) throws Exception
public FastTTSAudio speak(String backendName, String text, FastTTSVoice voice, FastTTSConfig config) throws Exception
```
Synthesizes the input text using the default or specified registered backend and returns a `FastTTSAudio` buffer.

---

### `prepare` & `close` (Lifecycle API)
```java
public void prepare()
public void prepare(String backendName)
public void prepareAll()
public void close() // Implements AutoCloseable
```
Pre-warms engines into memory / OS page cache for sub-millisecond dispatch, and releases native subprocesses, threads, and file handles upon shutdown.

---

## 2. Supported Backends

### `WindowsTTSBackend`
- **Native Implementation**: Uses Windows SAPI (`sapi.h`) via `fasttts.dll` (AVX2-optimized).
- **Zero Config**: Ultra-fast fallback (~150 ms latency, 0.03x RTF) without downloading external model files.

### `KokoroBackend`
- **Studio Neural Speech**: Powered by Sherpa-ONNX (`models/kokoro/kokoro-en-v0_19`).
- **Voices**: Studio-grade voices (`af_bella`, `am_adam`, `bf_emma`, `bm_george`).

### `PiperBackend`
- **Neural Offline TTS**: Integrates Piper ONNX engine (`piper.exe`).
- **Models**: Resolves models in `models/piper/` or `~/.speak/models/piper/`.

### `PocketTTSBackend`
- **Neural Offline CPU TTS**: Integrates Sherpa-ONNX INT8 quantized neural TTS (`sherpa-onnx-offline-tts.exe`).
- **Models**: Auto-resolves INT8 ONNX models and test WAV references without external Python or heavy ML runtimes (~0.96x RTF).

### `OmniVoiceBackend`
- **Zero-Shot Voice Cloning**: Diffusion language model supporting 600+ languages and voice cloning from reference WAV files.
- **Config**: Configurable diffusion steps via `config.setProperty("omnivoice.steps", "4")`.

### `Dia2Backend`
- **Multi-Speaker Scripted Dialogue**: GGUF quantized models (`dia2-2b-q4_k.gguf`) with `[S1]`, `[S2]` speaker turn parsing and memory page cache warming.

### `HuggingFaceBackend`
- **Cloud Spaces & Inference API**: Dual fallback between Serverless Inference API and Spaces Gradio endpoints with full control character JSON escaping.

### Cloud Backends (`ElevenLabsBackend`, `DeepgramBackend`, `OpenAIBackend`)
- **Cloud APIs**: Real-time HTTP/WebSocket streaming synthesis via respective API keys.

---

## 3. FastTTSAudio Class

```java
public class FastTTSAudio {
    public byte[] getData();
    public int getSampleRate();
    public int getChannels();
}
```
Encapsulates 16-bit PCM WAV audio buffers with metadata, ready for playback via `FastAudioPlayer` (WASAPI) or direct disk serialization.
