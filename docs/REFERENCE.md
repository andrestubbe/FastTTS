# FastTTS API Reference

## FastTTS

### Methods

| Method | Description | Parameters | Returns |
|--------|-------------|------------|---------|
| `registerBackend(FastTTSBackend)` | Registers a TTS backend with the orchestrator. | `backend` - TTS backend implementation | `void` |
| `speak(String)` | Synthesizes text to audio using the active backend. | `text` - Text to synthesize | `FastTTSAudio` |
| `speak(String, String, FastTTSVoice, FastTTSConfig)` | Synthesizes text with specific backend and configuration. | `backendName`, `text`, `voice`, `config` | `FastTTSAudio` |
| `use(String)` | Sets the active backend by name. | `name` - Backend name | `void` |
| `setDefaultBackend(String)` | Alias for `use(String)`. | `name` - Backend name | `void` |
| `getBackend(String)` | Returns a registered backend by name. | `name` - Backend name | `FastTTSBackend` |
| `getAllVoices()` | Returns available voices for all registered backends. | None | `List<FastTTSVoice>` |

## FastTTSBackend Interface

### Methods

| Method | Description | Parameters | Returns |
|--------|-------------|------------|---------|
| `synthesize(String, FastTTSVoice, FastTTSConfig)` | Synthesizes text to audio. | `text`, `voice`, `config` | `FastTTSAudio` |
| `stream(String, FastTTSVoice, FastTTSConfig, Consumer<byte[]>)` | Streams audio chunks during synthesis. | `text`, `voice`, `config`, `chunkConsumer` | `void` |
| `getVoices()` | Returns available voices for this backend. | None | `List<FastTTSVoice>` |
| `getName()` | Returns the backend name. | None | `String` |

## PiperBackend

### Constructor

```java
PiperBackend() // Uses default paths
PiperBackend(String piperPath, String modelPath) // Custom paths
```

### Notes
- Requires `piper.exe` and ONNX model files
- Supports environment variable `PIPER_PATH` for piper.exe location
- Model files should be in `models/` directory or specified path

## WindowsTTSBackend

### Constructor

```java
WindowsTTSBackend() // System default voice
```

### Notes
- Uses Windows Speech API (SAPI)
- No external dependencies required
- System default voice is used

## ElevenLabsBackend

### Constructor

```java
ElevenLabsBackend(String apiKey) // API key required
```

### Notes
- Requires valid ElevenLabs API key
- Cloud-based synthesis (requires internet)
- High-quality voices available

## DeepgramBackend

### Constructor

```java
DeepgramBackend(String apiKey) // API key required
```

### Notes
- Requires valid Deepgram API key
- Cloud-based synthesis (requires internet)
- Fast synthesis with multiple voice options

## FastTTSAudio

### Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `getData()` | Returns the raw audio data. | `byte[]` |
| `getSampleRate()` | Returns the audio sample rate. | `int` |

## FastTTSVoice

### Fields

| Field | Description |
|-------|-------------|
| `id()` | Voice identifier |
| `name()` | Voice display name |
| `language()` | Voice language code |
| `gender()` | Voice gender |
| `backend()` | Backend name |

## FastTTSConfig

### Fields

| Field | Description |
|-------|-------------|
| Speed parameters, voice settings, etc. |
