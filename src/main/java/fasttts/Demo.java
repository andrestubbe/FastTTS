package fasttts;

import fasttts.backends.piper.PiperBackend;
import fasttts.backends.elevenlabs.ElevenLabsBackend;
import fasttts.backends.deepgram.DeepgramBackend;
import fasttts.backends.windows.WindowsTTSBackend;
import fasttts.backends.openai.OpenAIBackend;
import fasttts.core.FastTTSAudio;
import fasttts.core.FastTTSVoice;
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
                System.out.println("  windows     - System SAPI (no API key needed)");
                System.out.println("  piper       - Offline TTS (no API key needed)");
                System.out.println("  openai      - OpenAI TTS (OPENAI_API_KEY env var)");
                System.out.println("  elevenlabs  - Cloud TTS (ELEVENLABS_API_KEY env var)");
                System.out.println("  deepgram    - Cloud TTS (DEEPGRAM_API_KEY env var)");
                System.out.println();
                System.out.println("Examples:");
                System.out.println("  run-demo windows \"Hello World\"");
                System.out.println("  run-demo windows \"Hello World\" output.wav");
                System.out.println("  run-demo piper \"Hello World\" path/to/model.onnx");
                System.out.println("  run-demo openai \"Hello World\" [voice]");
                System.out.println("  run-demo elevenlabs \"Hello World\" [voice_id]");
                System.out.println("  run-demo deepgram \"Hello World\" [voice_id]");
                return;
            }

            String backend = args[0].toLowerCase();
            String text;
            String apiKey = null;
            String outputFile = null;
            String modelPath = null;
            String voiceId = null;

            if (backend.equals("windows")) {
                if (args.length < 2) {
                    System.out.println("Usage: Demo windows <text> [output_file]");
                    return;
                }
                text = args[1];
                if (args.length >= 3) {
                    outputFile = args[2];
                }
            } else if (backend.equals("piper")) {
                if (args.length < 3) {
                    System.out.println("Usage: Demo piper <text> <model_path> [output_file]");
                    return;
                }
                text = args[1];
                modelPath = args[2];
                if (args.length >= 4) {
                    outputFile = args[3];
                }
            } else {
                if (args.length < 2) {
                    System.out.println("Usage: Demo " + backend + " <text> [voice_id] [output_file]");
                    return;
                }
                text = args[1];
                if (args.length >= 3) {
                    voiceId = args[2];
                }
                if (args.length >= 4) {
                    outputFile = args[3];
                }
            }

            System.out.println("=== FastTTS Demo ===");
            System.out.println("Loading " + backend + " backend...");
            System.out.println("Synthesizing...");

            FastTTSAudio audio;
            long loadTime = 0;
            long synthTime = 0;

            switch (backend) {
                case "windows":
                    long loadStartWin = System.currentTimeMillis();
                    WindowsTTSBackend windowsBackend = new WindowsTTSBackend();
                    loadTime = System.currentTimeMillis() - loadStartWin;
                    
                    long synthStartWin = System.currentTimeMillis();
                    audio = windowsBackend.synthesize(text, null, null);
                    synthTime = System.currentTimeMillis() - synthStartWin;
                    break;

                case "piper":
                    String piperPath = findPiper();
                    if (piperPath == null) {
                        System.err.println("piper.exe not found!");
                        return;
                    }
                    if (modelPath == null) {
                        System.err.println("Model path required for piper!");
                        return;
                    }
                    
                    long loadStart = System.currentTimeMillis();
                    PiperBackend piperBackend = new PiperBackend(piperPath, modelPath);
                    loadTime = System.currentTimeMillis() - loadStart;
                    
                    long synthStart = System.currentTimeMillis();
                    audio = piperBackend.synthesize(text, null, null);
                    synthTime = System.currentTimeMillis() - synthStart;
                    break;

                // case "dia2":
                //     if (modelPath == null) {
                //         System.err.println("Model path required for dia2!");
                //         return;
                //     }
                //     
                //     long loadStartDia2 = System.currentTimeMillis();
                //     Dia2Backend dia2Backend = new Dia2Backend(modelPath);
                //     loadTime = System.currentTimeMillis() - loadStartDia2;
                //     
                //     long synthStartDia2 = System.currentTimeMillis();
                //     audio = dia2Backend.synthesize(text, null, null);
                //     synthTime = System.currentTimeMillis() - synthStartDia2;
                //     break;

                // case "dia":
                //     long loadStartHF = System.currentTimeMillis();
                //     HuggingFaceBackend hfBackend = new HuggingFaceBackend(apiKey);
                //     loadTime = System.currentTimeMillis() - loadStartHF;
                //     
                //     long synthStartHF = System.currentTimeMillis();
                //     audio = hfBackend.synthesize(text, null, null);
                //     synthTime = System.currentTimeMillis() - synthStartHF;
                //     break;

                case "elevenlabs":
                    String elevenLabsKey = System.getenv("ELEVENLABS_API_KEY");
                    if (elevenLabsKey == null) {
                        System.err.println("ELEVENLABS_API_KEY environment variable not set!");
                        return;
                    }
                    long loadStartEL = System.currentTimeMillis();
                    ElevenLabsBackend elevenBackend = new ElevenLabsBackend(elevenLabsKey);
                    if (voiceId != null) {
                        elevenBackend.setDefaultVoiceId(voiceId);
                    }
                    loadTime = System.currentTimeMillis() - loadStartEL;
                    
                    long synthStartEL = System.currentTimeMillis();
                    audio = elevenBackend.synthesize(text, null, null);
                    synthTime = System.currentTimeMillis() - synthStartEL;
                    break;

                case "openai":
                    String openaiKey = System.getenv("OPENAI_API_KEY");
                    if (openaiKey == null) {
                        System.err.println("OPENAI_API_KEY environment variable not set!");
                        return;
                    }
                    long loadStartOpenAI = System.currentTimeMillis();
                    OpenAIBackend openaiBackend = new OpenAIBackend(openaiKey);
                    if (voiceId != null) {
                        openaiBackend.setDefaultVoice(voiceId);
                    }
                    loadTime = System.currentTimeMillis() - loadStartOpenAI;
                    
                    long synthStartOpenAI = System.currentTimeMillis();
                    audio = openaiBackend.synthesize(text, null, null);
                    synthTime = System.currentTimeMillis() - synthStartOpenAI;
                    break;

                case "deepgram":
                    String deepgramKey = System.getenv("DEEPGRAM_API_KEY");
                    if (deepgramKey == null) {
                        System.err.println("DEEPGRAM_API_KEY environment variable not set!");
                        return;
                    }
                    long loadStartDG = System.currentTimeMillis();
                    DeepgramBackend deepgramBackend = new DeepgramBackend(deepgramKey);
                    loadTime = System.currentTimeMillis() - loadStartDG;
                    
                    long synthStartDG = System.currentTimeMillis();
                    FastTTSVoice dgVoice = voiceId != null ? new FastTTSVoice(voiceId, voiceId, voiceId, null, "deepgram") : null;
                    audio = deepgramBackend.synthesize(text, dgVoice, null);
                    synthTime = System.currentTimeMillis() - synthStartDG;
                    break;

                default:
                    System.err.println("Unknown backend: " + backend);
                    return;
            }

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

            System.out.println();

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
