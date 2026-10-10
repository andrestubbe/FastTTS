package fasttts.demo;
import fasttts.FastTTS;

public class Demo {
    public static void main(String[] args) {
        System.out.println("--- FastTTS 0.1.3 Demo ---");
        try {
            FastTTS tts = new FastTTS();
            
            // 1. Windows SAPI (Native, no setup required)
            tts.registerBackend(new fasttts.backends.windows.WindowsTTSBackend());
            System.out.println("Synthesizing via Windows SAPI...");
            fasttts.core.FastTTSAudio audio = tts.speak("FastJava SIMD Hardware Vector Synthesis Engine 2026!");
            byte[] wav = audio != null ? audio.getData() : null;
            System.out.println("  Synthesized WAV size: " + (wav != null ? wav.length : 0) + " bytes");

            // 2. Pocket TTS (Offline neural Sherpa-ONNX)
            try {
                fasttts.backends.pockettts.PocketTTSBackend pocket = new fasttts.backends.pockettts.PocketTTSBackend();
                if (pocket.isAvailable()) {
                    tts.registerBackend(pocket);
                    System.out.println("Synthesizing via Pocket TTS (Sherpa-ONNX)...");
                    fasttts.core.FastTTSAudio pocketAudio = tts.speak("pocket", "Pocket TTS offline neural inference active!", null, null);
                    System.out.println("  Pocket WAV size: " + (pocketAudio != null && pocketAudio.getData() != null ? pocketAudio.getData().length : 0) + " bytes");
                } else {
                    System.out.println("  [Notice] Pocket TTS skipped (sherpa binary or models not found in standard paths)");
                }
            } catch (Exception ex) {
                System.out.println("  [Notice] Pocket TTS init skipped: " + ex.getMessage());
            }

            // 3. Piper (Offline neural TTS if model & executable exist)
            java.io.File piperExe = new java.io.File("piper.exe");
            java.io.File piperModel = new java.io.File("models/de_DE-thorsten-medium.onnx");
            if (piperExe.exists() && piperModel.exists()) {
                tts.registerBackend(new fasttts.backends.piper.PiperBackend(piperExe.getAbsolutePath(), piperModel.getAbsolutePath()));
                System.out.println("Synthesizing via Piper (Offline)...");
                fasttts.core.FastTTSAudio piperAudio = tts.speak("piper", "Hallo Welt von FastTTS!", null, null);
                System.out.println("  Piper WAV size: " + (piperAudio != null && piperAudio.getData() != null ? piperAudio.getData().length : 0) + " bytes");
            } else {
                System.out.println("  [Notice] Piper skipped (place piper.exe and model in models/ to enable offline neural TTS)");
            }

            // 3. Cloud Backends (if API keys are configured)
            String elevenKey = System.getenv("ELEVENLABS_API_KEY");
            if (elevenKey != null && !elevenKey.isBlank()) {
                tts.registerBackend(new fasttts.backends.elevenlabs.ElevenLabsBackend(elevenKey));
                System.out.println("  ElevenLabs backend registered.");
            }
            String deepgramKey = System.getenv("DEEPGRAM_API_KEY");
            if (deepgramKey != null && !deepgramKey.isBlank()) {
                tts.registerBackend(new fasttts.backends.deepgram.DeepgramBackend(deepgramKey));
                System.out.println("  Deepgram backend registered.");
            }

            System.out.println("✔ FastTTS demo completed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}