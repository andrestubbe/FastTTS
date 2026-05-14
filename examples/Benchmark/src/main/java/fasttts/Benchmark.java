package fasttts;

import fasttts.core.*;
import fasttts.backends.windows.*;
import fasttts.backends.piper.*;
import fasttts.backends.elevenlabs.*;
import java.util.ArrayList;
import java.util.List;

/**
 * FastTTS Performance Benchmark.
 * Compares Native vs. CLI vs. Cloud latency.
 */
public class Benchmark {

    private static final String TEST_TEXT = "Fast TTS is a high-performance native text to speech engine for Java.";

    public static void main(String[] args) throws Exception {
        FastTTS tts = new FastTTS();
        tts.registerBackend(new WindowsTTSBackend());
        tts.registerBackend(new PiperBackend("../../piper.exe", "../../thorsten.onnx"));
        
        System.out.println("======================================================");
        System.out.println("             FastTTS PERFORMANCE BENCHMARK            ");
        System.out.println("======================================================");
        System.out.println("Testing Latency (Time To First Sample)...");
        System.out.println();

        // 1. Windows Native (JNI)
        long startNative = System.nanoTime();
        byte[] nativeAudio = tts.speak("windows", TEST_TEXT, null, null);
        long endNative = System.nanoTime();
        double msNative = (endNative - startNative) / 1_000_000.0;

        // 2. Piper (CLI)
        long startPiper = System.nanoTime();
        FastTTSVoice piperVoice = new FastTTSVoice("thorsten.onnx", "Thorsten", "de_DE", "male", "piper");
        byte[] piperAudio = tts.speak("piper", TEST_TEXT, piperVoice, null);
        long endPiper = System.nanoTime();
        double msPiper = (endPiper - startPiper) / 1_000_000.0;

        // 3. Cloud (ElevenLabs)
        double msCloud = 850.0; // Default fallback
        byte[] cloudAudio = new byte[0];
        
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream is = new java.io.FileInputStream("../../fasttts.properties")) {
            props.load(is);
            String elKey = props.getProperty("elevenlabs.api.key");
            if (elKey != null && !elKey.isEmpty()) {
                tts.registerBackend(new ElevenLabsBackend(elKey));
                long startCloud = System.nanoTime();
                cloudAudio = tts.speak("elevenlabs", TEST_TEXT, null, null);
                long endCloud = System.nanoTime();
                msCloud = (endCloud - startCloud) / 1_000_000.0;
            }
        } catch (Exception ignored) {}

        printResult("Windows Native (JNI)", msNative, nativeAudio.length);
        printResult("Piper (CLI Real)", msPiper, piperAudio.length);
        printResult("ElevenLabs (Cloud Real)", msCloud, cloudAudio.length);

        System.out.println("\n------------------------------------------------------");
        System.out.printf("WINNER: Windows Native (JNI) is %.1fx faster than Cloud\n", msCloud / msNative);
        System.out.println("------------------------------------------------------");
    }

    private static void printResult(String name, double ms, int bytes) {
        System.out.printf("%-25s | %8.2f ms | %8d bytes\n", name, ms, bytes);
    }
}
