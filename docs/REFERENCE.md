# FastTTS API Reference Manual

`FastTTS` provides high-performance native Windows SAPI text-to-speech, Piper ONNX offline neural TTS synthesis, and ElevenLabs / Deepgram cloud TTS integration for Java applications with zero GC pressure.

---

## 1. FastTTS Engine API

### `FastTTS()`
```java
public FastTTS()
```
Constructs a new minimalist TTS orchestrator instance with zero pre-loaded backends.

---

### `registerBackend`
```java
public void registerBackend(FastTTSBackend backend)
```
Registers a TTS backend (e.g. `WindowsTTSBackend`, `PiperBackend`, `ElevenLabsBackend`, `DeepgramBackend`, `OpenAIBackend`).

---

### `speak`
```java
public FastTTSAudio speak(String text) throws Exception
public FastTTSAudio speak(String text, String voiceId, FastTTSVoice voice, FastTTSConfig config) throws Exception
```
Synthesizes the input text using the default or specified registered backend and returns a `FastTTSAudio` buffer.

---

## 2. Supported Backends

### `WindowsTTSBackend`
- **Native Implementation**: Uses Windows SAPI (`sapi.h`) via `fasttts.dll` (AVX2-optimized).
- **Zero Config**: Uses system default voices without downloading external model files.

### `PiperBackend`
- **Neural Offline TTS**: Integrates Piper ONNX engine (`piper.exe`).
- **Models**: Requires `.onnx` weights and `.onnx.json` config in `models/`.

### `ElevenLabsBackend` & `DeepgramBackend` & `OpenAIBackend`
- **Cloud APIs**: Real-time HTTP/WebSocket streaming synthesis.
- **Authentication**: Configured via API keys or `ELEVENLABS_API_KEY` / `OPENAI_API_KEY` environment variables.

---

## 3. FastTTSAudio Class

```java
public class FastTTSAudio {
    public byte[] getPcmData();
    public int getSampleRate();
    public int getChannels();
    public void saveWav(File outputFile);
}
```
Encapsulates 16-bit PCM or 32-bit float audio buffers with metadata and WAV export capabilities.
