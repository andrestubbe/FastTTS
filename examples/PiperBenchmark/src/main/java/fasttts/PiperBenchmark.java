package fasttts;

import fasttts.backends.piper.PiperBackend;
import fasttts.backends.piper.PiperONNXBackend;
import fasttts.core.FastTTSAudio;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Benchmark comparing PiperONNX (direct ONNX) vs PiperBackend (piper.exe).
 */
public class PiperBenchmark {
    
    public static void main(String[] args) {
        try {
            // Configuration
            String modelPath = "de_DE-thorsten-medium.onnx";
            String piperPath = getPiperPath();
            String espeakPath = getEspeakPath();
            
            // Test texts
            String[] testTexts = {
                "Hallo Welt!",
                "Dies ist ein kurzer Test.",
                "FastTTS PiperONNX Backend ist sehr schnell.",
                "Wir vergleichen die Performance von ONNX versus piper.exe.",
                "Die Benchmark-Testreihe umfasst verschiedene Textlängen und Komplexitäten."
            };
            
            // Check prerequisites
            if (!new File(modelPath).exists()) {
                System.err.println("Model file not found: " + new File(modelPath).getAbsolutePath());
                System.err.println("Please download using: download-models.bat");
                return;
            }
            
            if (piperPath == null || !new File(piperPath).exists()) {
                System.err.println("piper.exe not found!");
                System.err.println("Please install Piper and copy piper.exe to this directory.");
                System.err.println("Download from: https://github.com/rhasspy/piper/releases");
                return;
            }
            
            if (espeakPath == null || !new File(espeakPath).exists()) {
                System.err.println("espeak-ng.exe not found!");
                System.err.println("Please install eSpeak-NG from: https://github.com/espeak-ng/espeak-ng/releases");
                System.err.println("Download the Windows MSI installer and run it.");
                return;
            }
            
            System.out.println("=== Piper Benchmark: ONNX vs piper.exe ===");
            System.out.println("Model: " + modelPath);
            System.out.println("Piper: " + piperPath);
            System.out.println("eSpeak-NG: " + espeakPath);
            System.out.println("Test texts: " + testTexts.length);
            System.out.println();
            
            // Initialize backends
            System.out.println("Initializing backends...");
            
            FastTTS ttsONNX = new FastTTS();
            PiperONNXBackend onnxBackend = new PiperONNXBackend(modelPath, espeakPath);
            onnxBackend.initialize();
            ttsONNX.registerBackend(onnxBackend);
            
            FastTTS ttsPiper = new FastTTS();
            PiperBackend piperBackend = new PiperBackend(piperPath, modelPath);
            ttsPiper.registerBackend(piperBackend);
            
            System.out.println("Backends initialized.");
            System.out.println();
            
            // Warm-up runs
            System.out.println("Warm-up runs (2 iterations)...");
            for (int i = 0; i < 2; i++) {
                ttsONNX.speak("Warm-up");
                ttsPiper.speak("Warm-up");
            }
            System.out.println("Warm-up complete.");
            System.out.println();
            
            // Benchmark runs
            int iterations = 5;
            List<Long> onnxTimes = new ArrayList<>();
            List<Long> piperTimes = new ArrayList<>();
            
            System.out.println("Running benchmark (" + iterations + " iterations)...");
            
            for (int i = 0; i < iterations; i++) {
                System.out.println("Iteration " + (i + 1) + "/" + iterations);
                
                for (String text : testTexts) {
                    // Benchmark ONNX
                    long onnxStart = System.nanoTime();
                    FastTTSAudio onnxAudio = ttsONNX.speak(text);
                    long onnxEnd = System.nanoTime();
                    onnxTimes.add(onnxEnd - onnxStart);
                    
                    // Benchmark piper.exe
                    long piperStart = System.nanoTime();
                    FastTTSAudio piperAudio = ttsPiper.speak(text);
                    long piperEnd = System.nanoTime();
                    piperTimes.add(piperEnd - piperStart);
                }
            }
            
            System.out.println("Benchmark complete.");
            System.out.println();
            
            // Calculate statistics
            double onnxAvg = onnxTimes.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
            double piperAvg = piperTimes.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;
            
            long onnxMin = onnxTimes.stream().mapToLong(Long::longValue).min().orElse(0) / 1_000_000;
            long piperMin = piperTimes.stream().mapToLong(Long::longValue).min().orElse(0) / 1_000_000;
            
            long onnxMax = onnxTimes.stream().mapToLong(Long::longValue).max().orElse(0) / 1_000_000;
            long piperMax = piperTimes.stream().mapToLong(Long::longValue).max().orElse(0) / 1_000_000;
            
            // Print results
            System.out.println("=== Results ===");
            System.out.println();
            System.out.println("PiperONNX (direct ONNX):");
            System.out.println("  Average: " + String.format("%.2f", onnxAvg) + " ms");
            System.out.println("  Min:     " + onnxMin + " ms");
            System.out.println("  Max:     " + onnxMax + " ms");
            System.out.println();
            System.out.println("PiperBackend (piper.exe):");
            System.out.println("  Average: " + String.format("%.2f", piperAvg) + " ms");
            System.out.println("  Min:     " + piperMin + " ms");
            System.out.println("  Max:     " + piperMax + " ms");
            System.out.println();
            
            // Calculate speedup
            double speedup = piperAvg / onnxAvg;
            System.out.println("=== Performance Comparison ===");
            System.out.println("Speedup: " + String.format("%.2f", speedup) + "x");
            System.out.println("Time saved per synthesis: " + String.format("%.2f", piperAvg - onnxAvg) + " ms");
            System.out.println();
            
            if (speedup > 1.0) {
                System.out.println("✅ PiperONNX is " + String.format("%.0f", (speedup - 1) * 100) + "% faster!");
            } else {
                System.out.println("❌ PiperONNX is " + String.format("%.0f", (1 - speedup) * 100) + "% slower.");
            }
            
            // Cleanup
            onnxBackend.close();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Try to find eSpeak-NG in common locations.
     */
    private static String getEspeakPath() {
        // First check current directory
        if (new File("espeak-ng.exe").exists()) {
            return "espeak-ng.exe";
        }
        
        // Check common installation paths
        String[] possiblePaths = {
            "C:\\Program Files\\eSpeak NG\\espeak-ng.exe",
            "C:\\Program Files (x86)\\eSpeak NG\\espeak-ng.exe",
            "C:\\espeak-ng\\espeak-ng.exe"
        };
        
        for (String path : possiblePaths) {
            if (new File(path).exists()) {
                return path;
            }
        }
        
        return null;
    }
    
    /**
     * Try to find piper.exe in common locations.
     */
    private static String getPiperPath() {
        // First check current directory
        if (new File("piper.exe").exists()) {
            return "piper.exe";
        }
        
        // Check common installation paths
        String[] possiblePaths = {
            "C:\\Program Files\\Piper\\piper.exe",
            "C:\\Program Files (x86)\\Piper\\piper.exe",
            "C:\\piper\\piper.exe"
        };
        
        for (String path : possiblePaths) {
            if (new File(path).exists()) {
                return path;
            }
        }
        
        return null;
    }
}
