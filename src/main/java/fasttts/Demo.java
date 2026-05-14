package fasttts;

import java.util.List;
import java.util.Scanner;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;

/**
 * Terminal Demo for FastTTS.
 * Demonstrates backend registration, voice listing, and audio playback.
 */
public class Demo {

    public static void main(String[] args) {
        FastTTS tts = new FastTTS();
        
        // Register backends
        tts.registerBackend(new WindowsTTSBackend());
        tts.registerBackend(new PiperBackend("piper.exe", "en_US-lessac-medium.onnx"));

        System.out.println("FastTTS Ready. Default: Windows");
        
        // Zero Bullshit Speak
        tts.speak("Hello Andre! FastTTS is running.");
        
        // Change engine on the fly
        System.out.println("Switching to Piper...");
        tts.setDefaultBackend("piper");
        tts.speak("I am Piper, a fast offline voice.");
    }

    /**
     * Helper to play audio using standard Java Sound API.
     */
    private static void playAudio(byte[] audioData) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioData));
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.start();
            while (clip.isRunning()) {
                Thread.sleep(100);
            }
        } catch (Exception e) {
            System.err.println("Playback error: " + e.getMessage());
        }
    }
}
