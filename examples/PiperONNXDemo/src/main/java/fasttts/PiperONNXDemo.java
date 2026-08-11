package fasttts;

import fasttts.backends.piper.PiperONNXBackend;
import fasttts.core.FastTTSAudio;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Demo for FastTTS PiperONNX Backend.
 * Demonstrates direct ONNX inference using FastAIModel.
 */
public class PiperONNXDemo {
    
    public static void main(String[] args) {
        try {
            // Configuration - adjust paths to your setup
            String modelPath = "de_DE-thorsten-medium.onnx";  // Path to your Piper ONNX model
            String espeakPath = getEspeakPath();              // Path to eSpeak-NG executable
            String outputPath = "output.wav";                 // Output WAV file
            
            // Check if model exists
            if (!new File(modelPath).exists()) {
                System.err.println("Model file not found: " + new File(modelPath).getAbsolutePath());
                System.err.println("Please download a Piper model from: https://github.com/rhasspy/piper/releases");
                System.err.println("Or use: download-models.bat");
                return;
            }
            
            // Check if eSpeak-NG exists
            if (espeakPath == null || !new File(espeakPath).exists()) {
                System.err.println("eSpeak-NG not found!");
                System.err.println("Please install eSpeak-NG from: https://github.com/espeak-ng/espeak-ng/releases");
                System.err.println("Download the Windows MSI installer and run it.");
                System.err.println("Then copy espeak-ng.exe to this directory or modify the path in this demo.");
                System.err.println("Common installation paths:");
                System.err.println("  - C:\\Program Files\\eSpeak NG\\espeak-ng.exe");
                System.err.println("  - C:\\Program Files (x86)\\eSpeak NG\\espeak-ng.exe");
                return;
            }
            
            // Initialize FastTTS with PiperONNX backend
            FastTTS tts = new FastTTS();
            PiperONNXBackend backend = new PiperONNXBackend(modelPath, espeakPath);
            backend.initialize();
            tts.registerBackend(backend);
            
            // Text to synthesize
            String text = "Hallo Welt! Dies ist eine Demonstration des FastTTS PiperONNX Backends.";
            
            System.out.println("=== FastTTS PiperONNX Demo ===");
            System.out.println("Synthesizing: " + text);
            System.out.println("Using model: " + modelPath);
            System.out.println("Using eSpeak-NG: " + espeakPath);
            System.out.println();
            
            // Synthesize speech
            System.out.println("Starting synthesis...");
            long startTime = System.currentTimeMillis();
            FastTTSAudio audio = tts.speak(text);
            long endTime = System.currentTimeMillis();
            
            // Save to WAV file
            Files.write(Paths.get(outputPath), audio.getData());
            
            System.out.println("Success! Audio saved to: " + new File(outputPath).getAbsolutePath());
            System.out.println("Sample rate: " + audio.getSampleRate() + " Hz");
            System.out.println("Audio size: " + audio.getData().length + " bytes");
            System.out.println("Synthesis time: " + (endTime - startTime) + " ms");
            System.out.println();
            
            // Play the audio
            System.out.println("Playing audio...");
            audio.play();
            System.out.println("Playback finished.");
            
            // List available voices
            System.out.println("\nAvailable voices:");
            tts.getAllVoices().forEach(voice -> 
                System.out.println(" - " + voice.name() + " (" + voice.id() + ")")
            );
            
            // Cleanup
            backend.close();
            
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
}
