# PiperONNX Demo

Demo für FastTTS PiperONNX Backend - direkte ONNX Inferenz mit FastAIModel.

## Voraussetzungen

1. **Piper ONNX Modell**: Lade ein Piper Modell von [GitHub Releases](https://github.com/rhasspy/piper/releases)
   - Beispielsweise: `de_DE-thorsten-medium.onnx`
   - Auch die dazugehörige `de_DE-thorsten-medium.onnx.json` Datei

2. **eSpeak-NG**: Installiere eSpeak-NG von [GitHub Releases](https://github.com/espeak-ng/espeak-ng/releases)
   - Windows: `espeak-ng.exe`

3. **Dateien im richtigen Verzeichnis**:
   - Platziere `.onnx` und `.onnx.json` im Hauptverzeichnis
   - Platziere `espeak-ng.exe` im Hauptverzeichnis

## Demo ausführen

### Option 1: Batch-Skript
```bash
run-piper-demo.bat
```

### Option 2: Manuelles Kompilieren und Ausführen
```bash
cd examples\PiperONNXDemo
mvn clean package
cd ..\..
java -cp examples\PiperONNXDemo\target\PiperONNXDemo-1.0.0.jar fasttts.PiperONNXDemo
```

## Was die Demo macht

1. Initialisiert FastTTS mit PiperONNX Backend
2. Konvertiert Text zu IPA Phonemen via eSpeak-NG
3. Mapped Phoneme zu IDs mittels config.json
4. Führt ONNX Inferenz direkt durch (kein piper.exe Prozess)
5. Speichert das Ergebnis als WAV Datei

## Anpassung

Bearbeite `PiperONNXDemo.java` um:
- Andere Modelle zu verwenden
- Andere Stimmen auszuwählen
- Synthese-Parameter zu ändern (noise_scale, length_scale, etc.)

## Vorteile gegenüber piper.exe

- **Kein Prozess-Overhead**: Direkte ONNX Inferenz
- **Bessere Kontrolle**: Direkter Zugriff auf Synthese-Parameter
- **Faster**: Keine IPC (Inter-Process Communication)
- **Flexibler**: Kann in Java-Anwendungen eingebettet werden
