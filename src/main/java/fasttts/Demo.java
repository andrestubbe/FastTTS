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
        System.out.println("========================================");
        System.out.println("   FastTTS — Native Terminal Demo");
        System.out.println("========================================");

        try {
            FastTTS tts = new FastTTS();
            
            // Register Windows Backend
            WindowsTTSBackend winBackend = new WindowsTTSBackend();
            tts.registerBackend(winBackend);
            
            System.out.println("[SUCCESS] Registered Backend: " + winBackend.getName());

            // List Voices
            List<FastTTSVoice> voices = tts.getAllVoices();
            System.out.println("\nAvailable Voices:");
            for (int i = 0; i < voices.size(); i++) {
                System.out.println((i + 1) + ". " + voices.get(i));
            }

            Scanner scanner = new Scanner(System.in);
            System.out.print("\nChoose a voice number (default 1): ");
            int voiceIdx = 0;
            try {
                String input = scanner.nextLine();
                if (!input.isEmpty()) voiceIdx = Integer.parseInt(input) - 1;
            } catch (Exception e) { /* fallback to 0 */ }

            FastTTSVoice selectedVoice = voices.get(Math.max(0, Math.min(voiceIdx, voices.size() - 1)));
            System.out.println("Using: " + selectedVoice.name());

            while (true) {
                System.out.print("\nEnter text (or 'exit'): ");
                String text = scanner.nextLine();
                if (text.equalsIgnoreCase("exit")) break;

                System.out.println("Synthesizing...");
                long start = System.currentTimeMillis();
                byte[] audio = tts.speak("windows", text, selectedVoice, new FastTTSConfig());
                long end = System.currentTimeMillis();
                
                System.out.println("[INFO] Latency: " + (end - start) + "ms | Size: " + audio.length + " bytes");

                playAudio(audio);
            }

            System.out.println("Goodbye!");

        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Helper to play audio using FastAudioPlayer (Ecosystem standard).
     */
    private static void playAudio(byte[] audioData) {
        try {
            // Save to temp file as FastAudioPlayer currently loads from disk
            java.io.File temp = java.io.File.createTempFile("fasttts_output", ".wav");
            java.nio.file.Files.write(temp.toPath(), audioData);
            
            fastaudio.FastAudioPlayer player = new fastaudio.FastAudioPlayer();
            if (player.load(temp.getAbsolutePath())) {
                System.out.println("Playing via FastAudioPlayer...");
                player.play();
                while (player.isPlaying()) {
                    Thread.sleep(100);
                }
            }
            player.close();
            temp.delete();
        } catch (Exception e) {
            System.err.println("Playback error: " + e.getMessage());
        }
    }
}
