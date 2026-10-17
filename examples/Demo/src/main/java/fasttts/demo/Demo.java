package fasttts.demo;

import fasttts.FastTTS;
import fasttts.backends.deepgram.DeepgramBackend;
import fasttts.backends.dia2.Dia2Backend;
import fasttts.backends.elevenlabs.ElevenLabsBackend;
import fasttts.backends.huggingface.HuggingFaceBackend;
import fasttts.backends.kokoro.KokoroBackend;
import fasttts.backends.kokoro.KokoroVoice;
import fasttts.backends.openai.OpenAIBackend;
import fasttts.backends.omnivoice.OmniVoiceBackend;
import fasttts.backends.piper.PiperBackend;
import fasttts.backends.pockettts.PocketTTSBackend;
import fasttts.backends.windows.WindowsTTSBackend;
import fasttts.core.FastTTSAudio;
import fasttts.core.FastTTSVoice;
import fastaudio.FastAudioPlayer;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive & Benchmark Demo for all FastTTS engines.
 * Usage: Demo [backend] [text] [voice_id] [output_file]
 */
public class Demo {

    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                printUsage();
                runDefaultDemo();
                return;
            }

            if (args[0].equalsIgnoreCase("benchmark") || args[0].equalsIgnoreCase("--benchmark") || args[0].equalsIgnoreCase("-b")) {
                runBenchmark();
                return;
            }

            String backend = args[0].toLowerCase();
            String text = (args.length >= 2) ? args[1] : "Hello from FastTTS demo runner!";
            String voiceId = (args.length >= 3) ? args[2] : null;
            String outputFile = (args.length >= 4) ? args[3] : null;

            System.out.println("=== FastTTS Engine Demo ===");
            System.out.println("Selected Backend: " + backend);
            System.out.println("Text:             \"" + text + "\"");
            if (voiceId != null) System.out.println("Voice ID:         " + voiceId);
            System.out.println();

            FastTTSAudio audio;
            long tLoad0 = System.currentTimeMillis();
            long tLoad;
            long tSynth0;
            long tSynth;

            switch (backend) {
                case "windows":
                case "sapi":
                    WindowsTTSBackend winBackend = new WindowsTTSBackend();
                    tLoad = System.currentTimeMillis() - tLoad0;
                    tSynth0 = System.currentTimeMillis();
                    audio = winBackend.synthesize(text, null, null);
                    tSynth = System.currentTimeMillis() - tSynth0;
                    break;

                case "kokoro":
                    KokoroBackend kokoroBackend = new KokoroBackend();
                    tLoad = System.currentTimeMillis() - tLoad0;
                    tSynth0 = System.currentTimeMillis();
                    FastTTSVoice kVoice = (voiceId != null) ? KokoroVoice.fromName(voiceId) : KokoroVoice.AF_BELLA;
                    audio = kokoroBackend.synthesize(text, kVoice, null);
                    tSynth = System.currentTimeMillis() - tSynth0;
                    break;

                case "piper":
                    String piperModel = (voiceId != null) ? voiceId : "models/piper/de_DE-thorsten-medium.onnx";
                    if (!new File(piperModel).exists() && new File("models/de_DE-thorsten-medium.onnx").exists()) {
                        piperModel = "models/de_DE-thorsten-medium.onnx";
                    }
                    PiperBackend piperBackend = new PiperBackend();
                    tLoad = System.currentTimeMillis() - tLoad0;
                    tSynth0 = System.currentTimeMillis();
                    FastTTSVoice pVoice = new FastTTSVoice(piperModel, piperModel, "de-DE", "Neural", "piper");
                    audio = piperBackend.synthesize(text, pVoice, null);
                    tSynth = System.currentTimeMillis() - tSynth0;
                    break;

                case "omnivoice":
                case "omni":
                    OmniVoiceBackend omniBackend = new OmniVoiceBackend();
                    tLoad = System.currentTimeMillis() - tLoad0;
                    tSynth0 = System.currentTimeMillis();
                    FastTTSVoice oVoice = (voiceId != null) ? new FastTTSVoice(voiceId, voiceId, "multilingual", "Neural", "omnivoice") : null;
                    audio = omniBackend.synthesize(text, oVoice, null);
                    tSynth = System.currentTimeMillis() - tSynth0;
                    break;

                case "dia2":
                    Dia2Backend diaBackend = new Dia2Backend();
                    tLoad = System.currentTimeMillis() - tLoad0;
                    tSynth0 = System.currentTimeMillis();
                    FastTTSVoice dVoice = (voiceId != null) ? new FastTTSVoice(voiceId, voiceId, "en-US", "Dialogue", "dia2") : null;
                    audio = diaBackend.synthesize(text, dVoice, null);
                    tSynth = System.currentTimeMillis() - tSynth0;
                    break;

                case "huggingface":
                case "hf":
                    HuggingFaceBackend hfBackend = new HuggingFaceBackend();
                    tLoad = System.currentTimeMillis() - tLoad0;
                    tSynth0 = System.currentTimeMillis();
                    FastTTSVoice hfVoice = (voiceId != null) ? new FastTTSVoice(voiceId, voiceId, "en-US", "Cloud", "huggingface") : null;
                    audio = hfBackend.synthesize(text, hfVoice, null);
                    tSynth = System.currentTimeMillis() - tSynth0;
                    break;

                case "pocket":
                case "pockettts":
                    PocketTTSBackend pocketBackend = new PocketTTSBackend();
                    tLoad = System.currentTimeMillis() - tLoad0;
                    tSynth0 = System.currentTimeMillis();
                    FastTTSVoice pocketVoice = (voiceId != null) ? new FastTTSVoice(voiceId, voiceId, "en-US", "Pocket", "pocket") : null;
                    audio = pocketBackend.synthesize(text, pocketVoice, null);
                    tSynth = System.currentTimeMillis() - tSynth0;
                    break;

                case "elevenlabs":
                    String elKey = System.getenv("ELEVENLABS_API_KEY");
                    if (elKey == null || elKey.isBlank()) throw new IllegalStateException("ELEVENLABS_API_KEY env missing");
                    ElevenLabsBackend elBackend = new ElevenLabsBackend(elKey);
                    tLoad = System.currentTimeMillis() - tLoad0;
                    tSynth0 = System.currentTimeMillis();
                    FastTTSVoice elVoice = (voiceId != null) ? new FastTTSVoice(voiceId, voiceId, "en-US", "Cloud", "elevenlabs") : null;
                    audio = elBackend.synthesize(text, elVoice, null);
                    tSynth = System.currentTimeMillis() - tSynth0;
                    break;

                case "deepgram":
                    String dgKey = System.getenv("DEEPGRAM_API_KEY");
                    if (dgKey == null || dgKey.isBlank()) throw new IllegalStateException("DEEPGRAM_API_KEY env missing");
                    DeepgramBackend dgBackend = new DeepgramBackend(dgKey);
                    tLoad = System.currentTimeMillis() - tLoad0;
                    tSynth0 = System.currentTimeMillis();
                    FastTTSVoice dgVoice = (voiceId != null) ? new FastTTSVoice(voiceId, voiceId, "en-US", "Cloud", "deepgram") : null;
                    audio = dgBackend.synthesize(text, dgVoice, null);
                    tSynth = System.currentTimeMillis() - tSynth0;
                    break;

                default:
                    System.err.println("Unknown backend: " + backend);
                    printUsage();
                    return;
            }

            System.out.println("Load time:     " + tLoad + " ms");
            System.out.println("Synth time:    " + tSynth + " ms");
            System.out.println("Total time:    " + (tLoad + tSynth) + " ms");
            System.out.println("Audio size:    " + audio.getData().length + " bytes (" + String.format("%.1f", audio.getData().length / 1024.0) + " KB)");
            System.out.println("Sample rate:   " + audio.getSampleRate() + " Hz");
            double durationSec = (double) audio.getData().length / (audio.getSampleRate() * 2.0);
            System.out.println("Audio length:  " + String.format("%.2f", durationSec) + " seconds");

            if (outputFile != null) {
                java.nio.file.Files.write(java.nio.file.Paths.get(outputFile), audio.getData());
                System.out.println("Audio saved to: " + outputFile);
            } else {
                System.out.println("Playing audio out loud via FastAudioPlayer (WASAPI)...");
                playAudio(audio.getData());
                System.out.println("Playback complete.");
            }

        } catch (Exception e) {
            System.err.println("[FastTTS Demo Error]: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runDefaultDemo() {
        System.out.println("--- Running FastTTS Auto-Discovery Tour ---");
        FastTTS tts = FastTTS.standard();

        // 1. Windows Native
        try {
            System.out.print("[1/5] Testing Windows SAPI... ");
            FastTTSAudio audio = tts.speak("windows", "Windows SAPI native speech is online.", null, null);
            System.out.println("OK (" + (audio != null ? audio.getData().length : 0) + " bytes)");
            if (audio != null) playAudio(audio.getData());
        } catch (Exception e) {
            System.out.println("Skipped: " + e.getMessage());
        }

        // 2. Kokoro Studio
        try {
            System.out.print("[2/5] Testing Kokoro Studio (ONNX)... ");
            FastTTSAudio audio = tts.speak("kokoro", "Kokoro studio neural speech is active.", KokoroVoice.AF_BELLA, null);
            System.out.println("OK (" + (audio != null ? audio.getData().length : 0) + " bytes)");
            if (audio != null) playAudio(audio.getData());
        } catch (Exception e) {
            System.out.println("Skipped: " + e.getMessage());
        }

        // 3. Piper Neural
        try {
            System.out.print("[3/5] Testing Piper Neural... ");
            if (tts.getBackend("piper").isAvailable()) {
                FastTTSAudio audio = tts.speak("piper", "Piper offline neural speech is active.", null, null);
                System.out.println("OK (" + (audio != null ? audio.getData().length : 0) + " bytes)");
                if (audio != null) playAudio(audio.getData());
            } else {
                System.out.println("Not installed (run speak --download piper)");
            }
        } catch (Exception e) {
            System.out.println("Skipped: " + e.getMessage());
        }

        // 4. Dia2 Dialogue
        try {
            System.out.print("[4/5] Testing Dia2 GGUF 2B... ");
            if (tts.getBackend("dia2").isAvailable()) {
                FastTTSAudio audio = tts.speak("dia2", "[S1] Dia2 multi-speaker dialogue online.", null, null);
                System.out.println("OK (" + (audio != null ? audio.getData().length : 0) + " bytes)");
                if (audio != null) playAudio(audio.getData());
            } else {
                System.out.println("Model/Runner not active");
            }
        } catch (Exception e) {
            System.out.println("Skipped: " + e.getMessage());
        }

        // 5. Hugging Face Spaces
        try {
            System.out.print("[5/5] Testing Hugging Face Spaces... ");
            FastTTSAudio audio = tts.speak("huggingface", "Hugging Face cloud inference ready.", null, null);
            System.out.println("OK (" + (audio != null ? audio.getData().length : 0) + " bytes)");
            if (audio != null) playAudio(audio.getData());
        } catch (Exception e) {
            System.out.println("Skipped: " + e.getMessage());
        }

        System.out.println("✔ FastTTS tour completed.");
    }

    private static void printUsage() {
        System.out.println("Usage: run-demo <backend> [text] [voice_id] [output_file]");
        System.out.println();
        System.out.println("Supported Backends:");
        System.out.println("  windows      - Windows OneCore / SAPI (Native)");
        System.out.println("  kokoro       - Studio-Grade Offline Neural TTS (Sherpa-ONNX)");
        System.out.println("  piper        - Ultra-Fast Offline Neural TTS (ONNX)");
        System.out.println("  omnivoice    - Zero-Shot Voice Cloning (Diffusion, 600+ Languages)");
        System.out.println("  dia2         - Multi-Speaker Scripted Dialogue (GGUF 2B)");
        System.out.println("  huggingface  - Hugging Face Spaces / Inference API");
        System.out.println("  pocket       - Pocket TTS Lightweight Sherpa-ONNX");
        System.out.println("  elevenlabs   - ElevenLabs Cloud Voice API (ELEVENLABS_API_KEY)");
        System.out.println("  deepgram     - Deepgram Aura Cloud Voice API (DEEPGRAM_API_KEY)");
        System.out.println();
    }

    /**
     * Plays audio bytes out loud with ultra-low latency.
     * Uses FastAudioPlayer (WASAPI) with transparent fallback to SourceDataLine.
     */
    public static void playAudio(byte[] audioData) {
        if (audioData == null || audioData.length == 0) return;

        // 1. Preferred Native: FastAudioPlayer (WASAPI miniaudio)
        try {
            FastAudioPlayer player = new FastAudioPlayer();
            if (player.load(audioData)) {
                player.play();
                long durationMs = player.getDuration();
                long start = System.currentTimeMillis();
                while (player.isPlaying()) {
                    Thread.sleep(10);
                    if (durationMs > 0 && (System.currentTimeMillis() - start) > (durationMs + 300)) {
                        break;
                    }
                }
                player.close();
                return;
            }
        } catch (Throwable ignored) {
            // Fall back to Java Sound
        }

        // 2. High-Fidelity Fallback: Java Sound SourceDataLine (handles 24000 Hz, 16000 Hz, 44100 Hz, 48000 Hz)
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream in = AudioSystem.getAudioInputStream(bais);
            AudioFormat baseFormat = in.getFormat();
            AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );
            AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, in);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(decodedFormat);
            line.start();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = din.read(buffer, 0, buffer.length)) != -1) {
                line.write(buffer, 0, bytesRead);
            }
            line.drain();
            line.stop();
            line.close();
            din.close();
            in.close();
            return;
        } catch (Throwable t2) {
            // 3. Fallback: Standard Clip
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                AudioInputStream stream = AudioSystem.getAudioInputStream(bais);
                Clip clip = AudioSystem.getClip();
                clip.open(stream);
                clip.start();
                while (!clip.isRunning()) Thread.sleep(5);
                while (clip.isRunning()) Thread.sleep(5);
                clip.close();
                stream.close();
            } catch (Throwable t3) {
                System.err.println("[FastTTS Playback Error]: Unable to play audio: " + t3.getMessage());
            }
        }
    }

    private record BenchResult(String engine, String voice, String lang, long synthMs, long audioMs, double rtf, double kb, String status) {}

    public static void runBenchmark() {
        System.out.println("========================================================================================================");
        System.out.println("                               FastTTS Multi-Engine Benchmark Suite");
        System.out.println("========================================================================================================");
        System.out.println("Testing all available models & measuring synthesis time, audio duration, and Real-Time Factor (RTF)...");
        System.out.println();

        List<BenchResult> results = new ArrayList<>();
        String enText = "Speech synthesis benchmark with FastTTS hardware acceleration.";
        String deText = "Sprachsynthese Benchmark mit FastTTS Beschleunigung.";

        // 1. Windows Native
        try {
            System.out.print("[Benchmarking] Windows SAPI/OneCore... ");
            WindowsTTSBackend win = new WindowsTTSBackend();
            long t0 = System.currentTimeMillis();
            FastTTSAudio audio = win.synthesize(enText, null, null);
            long synthMs = System.currentTimeMillis() - t0;
            long audioMs = (long) ((audio.getData().length / (double) (audio.getSampleRate() * 2)) * 1000.0);
            double rtf = (audioMs > 0) ? (synthMs / (double) audioMs) : 0;
            double kb = audio.getData().length / 1024.0;
            results.add(new BenchResult("Windows SAPI", "OneCore/Default", "en-US", synthMs, audioMs, rtf, kb, "[SUCCESS]"));
            System.out.println(synthMs + " ms -> [Playing audio]");
            playAudio(audio.getData());
        } catch (Exception e) {
            System.out.println("Failed: " + e.getMessage());
            results.add(new BenchResult("Windows SAPI", "OneCore/Default", "en-US", 0, 0, 0, 0, "[FAILED: " + e.getMessage() + "]"));
        }

        // 2. Kokoro Studio voices
        KokoroBackend kokoro = new KokoroBackend();
        FastTTSVoice[] kokoroVoices = {
                KokoroVoice.AF_BELLA,
                KokoroVoice.AM_ADAM,
                KokoroVoice.BF_EMMA,
                KokoroVoice.BM_GEORGE
        };
        for (FastTTSVoice kv : kokoroVoices) {
            try {
                System.out.print("[Benchmarking] Kokoro Studio (" + kv.name() + ")... ");
                long t0 = System.currentTimeMillis();
                FastTTSAudio audio = kokoro.synthesize(enText, kv, null);
                long synthMs = System.currentTimeMillis() - t0;
                long audioMs = (long) ((audio.getData().length / (double) (audio.getSampleRate() * 2)) * 1000.0);
                double rtf = (audioMs > 0) ? (synthMs / (double) audioMs) : 0;
                double kb = audio.getData().length / 1024.0;
                results.add(new BenchResult("Kokoro Studio (ONNX)", kv.name(), kv.locale(), synthMs, audioMs, rtf, kb, "[SUCCESS]"));
                System.out.println(synthMs + " ms -> [Playing audio]");
                playAudio(audio.getData());
            } catch (Exception e) {
                System.out.println("Failed: " + e.getMessage());
                results.add(new BenchResult("Kokoro Studio (ONNX)", kv.name(), kv.locale(), 0, 0, 0, 0, "[FAILED: " + e.getMessage() + "]"));
            }
        }

        // 3. Piper Neural
        try {
            System.out.print("[Benchmarking] Piper Neural (thorsten-medium)... ");
            PiperBackend piper = new PiperBackend();
            String piperModel = "models/piper/de_DE-thorsten-medium.onnx";
            if (!new File(piperModel).exists()) {
                piperModel = "models/de_DE-thorsten-medium.onnx";
            }
            if (new File(piperModel).exists()) {
                long t0 = System.currentTimeMillis();
                FastTTSAudio audio = piper.synthesize(deText, new FastTTSVoice(piperModel, piperModel, "de-DE", "Neural", "piper"), null);
                long synthMs = System.currentTimeMillis() - t0;
                long audioMs = (long) ((audio.getData().length / (double) (audio.getSampleRate() * 2)) * 1000.0);
                double rtf = (audioMs > 0) ? (synthMs / (double) audioMs) : 0;
                double kb = audio.getData().length / 1024.0;
                results.add(new BenchResult("Piper Neural (ONNX)", "thorsten-medium", "de-DE", synthMs, audioMs, rtf, kb, "[SUCCESS]"));
                System.out.println(synthMs + " ms -> [Playing audio]");
                playAudio(audio.getData());
            } else {
                System.out.println("Skipped");
                results.add(new BenchResult("Piper Neural (ONNX)", "thorsten-medium", "de-DE", 0, 0, 0, 0, "[SKIPPED: Model missing]"));
            }
        } catch (Exception e) {
            System.out.println("Failed: " + e.getMessage());
            results.add(new BenchResult("Piper Neural (ONNX)", "thorsten-medium", "de-DE", 0, 0, 0, 0, "[FAILED: " + e.getMessage() + "]"));
        }

        // 4. Pocket TTS (Sherpa-ONNX)
        try {
            System.out.print("[Benchmarking] Pocket TTS (Sherpa-ONNX)... ");
            PocketTTSBackend pocket = new PocketTTSBackend();
            if (pocket.isAvailable()) {
                long t0 = System.currentTimeMillis();
                FastTTSAudio audio = pocket.synthesize(enText, null, null);
                long synthMs = System.currentTimeMillis() - t0;
                long audioMs = (long) ((audio.getData().length / (double) (audio.getSampleRate() * 2)) * 1000.0);
                double rtf = (audioMs > 0) ? (synthMs / (double) audioMs) : 0;
                double kb = audio.getData().length / 1024.0;
                results.add(new BenchResult("Pocket TTS (ONNX)", "int8-bria", "en-US", synthMs, audioMs, rtf, kb, "[SUCCESS]"));
                System.out.println(synthMs + " ms -> [Playing audio]");
                playAudio(audio.getData());
            } else {
                System.out.println("Skipped");
                results.add(new BenchResult("Pocket TTS (ONNX)", "int8-bria", "en-US", 0, 0, 0, 0, "[SKIPPED: Model missing]"));
            }
        } catch (Exception e) {
            System.out.println("Failed: " + e.getMessage());
            results.add(new BenchResult("Pocket TTS (ONNX)", "int8-bria", "en-US", 0, 0, 0, 0, "[FAILED: " + e.getMessage() + "]"));
        }

        // 5. Hugging Face Inference API
        HuggingFaceBackend hf = new HuggingFaceBackend();
        String[] hfModels = {"facebook/mms-tts-eng", "facebook/mms-tts-deu"};
        for (String m : hfModels) {
            String phrase = m.contains("deu") ? deText : enText;
            String lang = m.contains("deu") ? "de-DE" : "en-US";
            String shortName = m.substring(m.lastIndexOf('/') + 1);
            try {
                System.out.print("[Benchmarking] HuggingFace (" + shortName + ")... ");
                long t0 = System.currentTimeMillis();
                FastTTSAudio audio = hf.synthesize(phrase, new FastTTSVoice(m, m, lang, "Cloud", "huggingface"), null);
                long synthMs = System.currentTimeMillis() - t0;
                long audioMs = (long) ((audio.getData().length / (double) (audio.getSampleRate() * 2)) * 1000.0);
                double rtf = (audioMs > 0) ? (synthMs / (double) audioMs) : 0;
                double kb = audio.getData().length / 1024.0;
                results.add(new BenchResult("HuggingFace API", shortName, lang, synthMs, audioMs, rtf, kb, "[SUCCESS]"));
                System.out.println(synthMs + " ms -> [Playing audio]");
                playAudio(audio.getData());
            } catch (Exception e) {
                String msg = (System.getenv("HF_TOKEN") == null) ? "Requires HF_TOKEN" : (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                System.out.println("Notice: " + msg);
                results.add(new BenchResult("HuggingFace API", shortName, lang, 0, 0, 0, 0, "[" + msg + "]"));
            }
        }

        // 6. Dia2 Dialogue (GGUF)
        try {
            Dia2Backend dia2 = new Dia2Backend();
            if (dia2.isAvailable()) {
                System.out.print("[Benchmarking] Dia2 Dialogue (GGUF)... ");
                long t0 = System.currentTimeMillis();
                FastTTSAudio audio = dia2.synthesize("[S1] " + enText, null, null);
                long synthMs = System.currentTimeMillis() - t0;
                long audioMs = (long) ((audio.getData().length / (double) (audio.getSampleRate() * 2)) * 1000.0);
                double rtf = (audioMs > 0) ? (synthMs / (double) audioMs) : 0;
                double kb = audio.getData().length / 1024.0;
                results.add(new BenchResult("Dia2 Dialogue (GGUF)", "dia2-2b-q4_k", "en-US", synthMs, audioMs, rtf, kb, "[SUCCESS]"));
                System.out.println(synthMs + " ms -> [Playing audio]");
                playAudio(audio.getData());
            } else if (dia2.hasModel()) {
                results.add(new BenchResult("Dia2 Dialogue (GGUF)", "dia2-2b-q4_k", "en-US", 0, 0, 0, 0, "[MODEL READY: Needs audiocpp_cli]"));
            } else {
                results.add(new BenchResult("Dia2 Dialogue (GGUF)", "dia2-2b-q4_k", "en-US", 0, 0, 0, 0, "[PENDING: audiocpp_cli.exe]"));
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            results.add(new BenchResult("Dia2 Dialogue (GGUF)", "dia2-2b-q4_k", "en-US", 0, 0, 0, 0, "[FAILED: " + msg + "]"));
        }

        // 7. OmniVoice (Diffusion)
        try {
            OmniVoiceBackend omni = new OmniVoiceBackend();
            File omniWeights = new File("models/omnivoice/model.safetensors");
            if (omni.isAvailable() && omniWeights.exists() && omniWeights.length() > 2_400_000_000L) {
                System.out.print("[Benchmarking] OmniVoice (Diffusion)... ");
                long t0 = System.currentTimeMillis();
                fasttts.core.FastTTSConfig cfg = new fasttts.core.FastTTSConfig();
                cfg.setProperty("omnivoice.steps", "4");
                FastTTSAudio audio = omni.synthesize(enText, null, cfg);
                long synthMs = System.currentTimeMillis() - t0;
                long audioMs = (long) ((audio.getData().length / (double) (audio.getSampleRate() * 2)) * 1000.0);
                double rtf = (audioMs > 0) ? (synthMs / (double) audioMs) : 0;
                double kb = audio.getData().length / 1024.0;
                results.add(new BenchResult("OmniVoice (Diffusion)", "bria (Zero-Shot)", "en-US", synthMs, audioMs, rtf, kb, "[SUCCESS]"));
                System.out.println(synthMs + " ms -> [Playing audio]");
                playAudio(audio.getData());
            } else {
                long curMb = omniWeights.exists() ? (omniWeights.length() / (1024 * 1024)) : 0;
                results.add(new BenchResult("OmniVoice (Diffusion)", "bria (Zero-Shot)", "multilingual", 0, 0, 0, 0, "[DOWNLOADING: " + curMb + "/2450 MB]"));
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            results.add(new BenchResult("OmniVoice (Diffusion)", "bria (Zero-Shot)", "multilingual", 0, 0, 0, 0, "[FAILED: " + msg + "]"));
        }

        System.out.println();
        // Print Formatted Table
        System.out.printf("%-23s | %-24s | %-6s | %10s | %10s | %9s | %10s | %s%n",
                "ENGINE", "VOICE / MODEL", "LANG", "SYNTH (ms)", "AUDIO (ms)", "RTF", "SIZE (KB)", "STATUS");
        System.out.println("--------------------------------------------------------------------------------------------------------------------------------");

        for (BenchResult r : results) {
            if (r.status.equals("[SUCCESS]")) {
                System.out.printf("%-23s | %-24s | %-6s | %8d ms | %8d ms | %8.3fx | %8.1f KB | %s%n",
                        r.engine, r.voice, r.lang, r.synthMs, r.audioMs, r.rtf, r.kb, r.status);
            } else {
                System.out.printf("%-23s | %-24s | %-6s | %10s | %10s | %9s | %10s | %s%n",
                        r.engine, r.voice, r.lang, "-", "-", "-", "-", r.status);
            }
        }
        System.out.println("--------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("RTF (Real-Time Factor): values < 1.0x indicate faster than real-time synthesis! (e.g., 0.10x = 10x real-time speed)");
        System.out.println();
    }
}