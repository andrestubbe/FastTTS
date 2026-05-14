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
        tts.registerBackend(new WindowsTTSBackend());

        System.out.println("=== FastTTS Windows Voice Test ===");
        List<FastTTSVoice> voices = tts.getAllVoices();
        for (int i = 0; i < voices.size(); i++) {
            System.out.println((i + 1) + ". " + voices.get(i).name());
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("\nSelect voice (1-" + voices.size() + "): ");
        int idx = scanner.nextInt();
        scanner.nextLine(); // consume newline

        FastTTSVoice selected = voices.get(idx - 1);
        System.out.println("Using: " + selected.name());

        while (true) {
            System.out.print("\nEnter text (q to quit): ");
            String text = scanner.nextLine();
            if (text.equalsIgnoreCase("q")) break;

            System.out.println("Speaking...");
            try {
                byte[] audio = tts.synthesize("windows", text, selected, null);
                System.out.println("Synthesis complete. Buffer size: " + (audio != null ? audio.length : 0) + " bytes");
                if (audio != null && audio.length > 0) {
                    playAudio(audio);
                } else {
                    System.err.println("Warning: Received empty audio buffer.");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Helper to play audio using standard Java Sound API.
     */
    private static void playAudio(byte[] audioData) {
        try {
            System.out.println("Processing audio buffer (" + audioData.length + " bytes)...");
            
            AudioInputStream ais;
            try {
                ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioData));
                System.out.println("Detected WAV format: " + ais.getFormat());
            } catch (UnsupportedAudioFileException e) {
                System.out.println("No WAV header found. Treating as raw PCM (44.1kHz, 16-bit, Mono)...");
                AudioFormat rawFormat = new AudioFormat(44100, 16, 1, true, false);
                ais = new AudioInputStream(new ByteArrayInputStream(audioData), rawFormat, audioData.length / 2);
            }

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, ais.getFormat());
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(ais.getFormat());
            line.start();

            byte[] buffer = new byte[4096];
            int read;
            while ((read = ais.read(buffer)) != -1) {
                line.write(buffer, 0, read);
            }
            
            line.drain();
            line.close();
            System.out.println("Playback finished.");
        } catch (Exception e) {
            System.err.println("Playback error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
