package fasttts;

import fasttts.backends.piper.PiperBackend;
import fasttts.backends.elevenlabs.ElevenLabsBackend;
import fasttts.backends.deepgram.DeepgramBackend;
import fasttts.backends.windows.WindowsTTSBackend;
import fasttts.core.FastTTSAudio;
import javax.sound.sampled.*;
import java.io.File;
import java.io.ByteArrayInputStream;

/**
 * Simple demo for all FastTTS backends.
 */
public class Demo {
    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                System.out.println("Usage: Demo <backend> [apikey] <text> [output_file]");
                System.out.println();
                System.out.println("Backends:");
                System.out.println("  piper       - Offline TTS (no API key needed)");
                System.out.println("  elevenlabs  - Cloud TTS (requires API key)");
                System.out.println("  deepgram    - Cloud TTS (requires API key)");
                System.out.println("  windows     - System SAPI (no API key needed)");
                System.out.println();
                System.out.println("Examples:");
                System.out.println("  Demo piper \"Hallo Welt\"");
                System.out.println("  Demo elevenlabs YOUR_API_KEY \"Hello World\"");
                System.out.println("  Demo deepgram YOUR_API_KEY \"Hello World\"");
                System.out.println("  Demo windows \"Hallo Welt\" output.wav");
                return;
            }

            String backend = args[0].toLowerCase();
            String text;
            String apiKey = null;
            String outputFile = null;

            if (backend.equals("piper") || backend.equals("windows")) {
                if (args.length < 2) {
                    System.out.println("Usage: Demo " + backend + " <text> [output_file]");
                    return;
                }
                text = args[1];
                if (args.length >= 3) {
                    outputFile = args[2];
                }
            } else {
                if (args.length < 3) {
                    System.out.println("Usage: Demo " + backend + " <api_key> <text> [output_file]");
                    return;
                }
                apiKey = args[1];
                text = args[2];
                if (args.length >= 4) {
                    outputFile = args[3];
                }
            }

            System.out.println("=== FastTTS Demo ===");
            System.out.println("Backend: " + backend);
            System.out.println("Text: " + text);
            System.out.println();

            FastTTSAudio audio;
            long loadTime = 0;
            long synthTime = 0;

            switch (backend) {
                case "piper":
                    String piperPath = findPiper();
                    String modelPath = findModel();
                    if (piperPath == null) {
                        System.err.println("piper.exe not found!");
                        return;
                    }
                    if (modelPath == null) {
                        System.err.println("Model file not found!");
                        return;
                    }
                    
                    System.out.println("Loading Piper backend...");
                    long loadStart = System.currentTimeMillis();
                    PiperBackend piperBackend = new PiperBackend(piperPath, modelPath);
                    loadTime = System.currentTimeMillis() - loadStart;
                    System.out.println("Backend loaded in " + loadTime + " ms");
                    
                    System.out.println("Synthesizing...");
                    long synthStart = System.currentTimeMillis();
                    audio = piperBackend.synthesize(text, null, null);
                    synthTime = System.currentTimeMillis() - synthStart;
                    break;

                case "elevenlabs":
                    System.out.println("Loading ElevenLabs backend...");
                    long loadStartEL = System.currentTimeMillis();
                    ElevenLabsBackend elevenBackend = new ElevenLabsBackend(apiKey);
                    loadTime = System.currentTimeMillis() - loadStartEL;
                    System.out.println("Backend loaded in " + loadTime + " ms");
                    
                    System.out.println("Synthesizing...");
                    long synthStartEL = System.currentTimeMillis();
                    audio = elevenBackend.synthesize(text, null, null);
                    synthTime = System.currentTimeMillis() - synthStartEL;
                    break;

                case "deepgram":
                    System.out.println("Loading Deepgram backend...");
                    long loadStartDG = System.currentTimeMillis();
                    DeepgramBackend deepgramBackend = new DeepgramBackend(apiKey);
                    loadTime = System.currentTimeMillis() - loadStartDG;
                    System.out.println("Backend loaded in " + loadTime + " ms");
                    
                    System.out.println("Synthesizing...");
                    long synthStartDG = System.currentTimeMillis();
                    audio = deepgramBackend.synthesize(text, null, null);
                    synthTime = System.currentTimeMillis() - synthStartDG;
                    break;

                case "windows":
                    System.out.println("Loading Windows SAPI backend...");
                    long loadStartWin = System.currentTimeMillis();
                    WindowsTTSBackend windowsBackend = new WindowsTTSBackend();
                    loadTime = System.currentTimeMillis() - loadStartWin;
                    System.out.println("Backend loaded in " + loadTime + " ms");
                    
                    System.out.println("Synthesizing...");
                    long synthStartWin = System.currentTimeMillis();
                    audio = windowsBackend.synthesize(text, null, null);
                    synthTime = System.currentTimeMillis() - synthStartWin;
                    break;

                default:
                    System.err.println("Unknown backend: " + backend);
                    return;
            }

            System.out.println();
            System.out.println("=== Results ===");
            System.out.println("Load time:   " + loadTime + " ms");
            System.out.println("Synth time:  " + synthTime + " ms");
            System.out.println("Total time:  " + (loadTime + synthTime) + " ms");
            System.out.println("Audio size:  " + audio.getData().length + " bytes");
            System.out.println("Sample rate: " + audio.getSampleRate() + " Hz");
            System.out.println("Duration:    " + String.format("%.2f", (double)audio.getData().length / (audio.getSampleRate() * 2)) + " seconds");

            // Save to file only if specified
            if (outputFile != null) {
                java.nio.file.Files.write(
                    java.nio.file.Paths.get(outputFile),
                    audio.getData()
                );
                System.out.println("Saved to: " + outputFile);
            } else {
                // Play audio directly without saving
                System.out.println("Playing audio...");
                playAudioInMemory(audio.getData(), audio.getSampleRate());
                System.out.println("Playback finished.");
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String findPiper() {
        // Check environment variable first
        String envPath = System.getenv("PIPER_PATH");
        if (envPath != null && new File(envPath).exists()) {
            return envPath;
        }
        
        // Check current directory
        if (new File("piper.exe").exists()) return "piper.exe";
        
        // Check standard installation paths
        String[] possiblePaths = {
            "C:\\Program Files\\Piper\\piper.exe",
            "C:\\Program Files (x86)\\Piper\\piper.exe",
            "C:\\Piper\\piper.exe"
        };
        
        for (String path : possiblePaths) {
            if (new File(path).exists()) {
                return path;
            }
        }
        
        return null;
    }

    private static String findModel() {
        File modelsDir = new File("models");
        if (modelsDir.exists()) {
            File[] models = modelsDir.listFiles((d, name) -> name.endsWith(".onnx"));
            if (models != null && models.length > 0) {
                return models[0].getAbsolutePath();
            }
        }
        // Fallback to current directory
        File dir = new File(".");
        File[] models = dir.listFiles((d, name) -> name.endsWith(".onnx"));
        if (models != null && models.length > 0) {
            return models[0].getAbsolutePath();
        }
        return null;
    }

    private static void playAudio(String filename) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", filename);
            pb.start();
        } catch (Exception e) {
            System.err.println("Could not play audio: " + e.getMessage());
        }
    }

    private static void playAudioInMemory(byte[] audioData, int sampleRate) {
        try {
            // Use AudioSystem to handle WAV format automatically
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bais);
            
            DataLine.Info info = new DataLine.Info(Clip.class, audioStream.getFormat());
            Clip clip = (Clip) AudioSystem.getLine(info);
            
            clip.open(audioStream);
            clip.start();
            
            // Wait for playback to complete
            while (!clip.isRunning()) Thread.sleep(10);
            while (clip.isRunning()) Thread.sleep(10);
            
            clip.close();
            audioStream.close();
        } catch (Exception e) {
            System.err.println("Could not play audio: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
