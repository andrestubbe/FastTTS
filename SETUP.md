# FastTTS PiperONNX Setup Guide

## Required Files

To run the PiperONNX demos, you need two components:

### 1. Piper German Model
- **Model**: `de_DE-thorsten-medium.onnx` (~63 MB)
- **Config**: `de_DE-thorsten-medium.onnx.json`
- **Source**: [Hugging Face - Trelis/piper-de-de-thorsten-medium](https://huggingface.co/Trelis/piper-de-de-thorsten-medium)

### 2. eSpeak-NG
- **Executable**: `espeak-ng.exe`
- **Purpose**: Text-to-IPA phonemization
- **Source**: [GitHub Releases](https://github.com/espeak-ng/espeak-ng/releases)

## Quick Setup

### Option 1: Automated Download
```bash
download-models.bat
```
This script provides both manual instructions and automatic download options.

### Option 2: Manual Download

#### Piper Model:
1. Visit: https://huggingface.co/Trelis/piper-de-de-thorsten-medium/tree/main
2. Download:
   - `model.onnx` → rename to `de_DE-thorsten-medium.onnx`
   - `model.onnx.json` → rename to `de_DE-thorsten-medium.onnx.json`
3. Place both files in: `C:\Users\andre\Documents\2026-06-14-Work-FastJava\FastTTS`

#### eSpeak-NG:
1. Visit: https://github.com/espeak-ng/espeak-ng/releases
2. Download the latest Windows release (.msi installer)
3. Install eSpeak-NG
4. Find `espeak-ng.exe` (typically in `C:\Program Files\eSpeak NG\`)
5. Copy `espeak-ng.exe` to: `C:\Users\andre\Documents\2026-06-14-Work-FastJava\FastTTS`

## File Locations After Setup

Your FastTTS directory should contain:
```
FastTTS/
├── de_DE-thorsten-medium.onnx          # Piper model
├── de_DE-thorsten-medium.onnx.json     # Piper config
├── espeak-ng.exe                       # eSpeak-NG executable
├── run-piper-demo.bat                  # ONNX demo launcher
├── run-piper-benchmark.bat             # Benchmark launcher
└── download-models.bat                 # Setup helper
```

## Running the Demos

### PiperONNX Demo (with audio playback)
```bash
run-piper-demo.bat
```
This will:
- Synthesize German text using direct ONNX inference
- Save to `output.wav`
- Play the audio automatically
- Show performance metrics

### Piper Benchmark (ONNX vs piper.exe)
```bash
run-piper-benchmark.bat
```
This will:
- Compare PiperONNX vs original piper.exe
- Run multiple test iterations
- Show detailed performance statistics
- Calculate speedup factor

## Alternative: Use Installed Paths

If you don't want to copy files, you can modify the demo code to use installed paths:

```java
// In PiperONNXDemo.java, change:
String modelPath = "de_DE-thorsten-medium.onnx";
String espeakPath = "espeak-ng.exe";

// To installed paths:
String modelPath = "C:\\path\\to\\de_DE-thorsten-medium.onnx";
String espeakPath = "C:\\Program Files\\eSpeak NG\\espeak-ng.exe";
```

## Troubleshooting

### "Model file not found"
- Ensure `de_DE-thorsten-medium.onnx` is in the FastTTS directory
- Check file extension is correct (.onnx, not .onnx.txt)

### "espeak-ng.exe not found"
- Ensure `espeak-ng.exe` is in the FastTTS directory
- Or modify the demo to use the full path to the installed executable

### "ONNX Runtime errors"
- Ensure the FastAIModel dependency is properly installed
- Check that ONNX Runtime DLLs are available

### Audio playback issues
- Ensure your system has audio output configured
- Check that Java Sound API is working on your system

## Performance Expectations

With direct ONNX inference (PiperONNX), you should see:
- **Faster synthesis** than piper.exe (no process overhead)
- **Lower latency** (no IPC communication)
- **Better memory efficiency** (single process)

Typical speedup: **1.5x - 3x** depending on text length and system configuration.
