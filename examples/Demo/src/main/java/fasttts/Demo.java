package fasttts;

import fasttts.core.*;
import fasttts.backends.windows.*;
import fasttts.backends.piper.*;
import fasttts.backends.elevenlabs.*;
import java.util.List;
import java.util.Scanner;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Terminal Demo for FastTTS.
 * Demonstrates backend registration, voice listing, and audio playback.
 */
public class Demo {

    public static void main(String[] args) {
        FastTTS tts = new FastTTS();
        tts.registerBackend(new WindowsTTSBackend());
        
        // Register Piper if available
        File piperExe = new File("../../piper.exe");
        if (piperExe.exists()) {
            tts.registerBackend(new PiperBackend("../../piper.exe", "../../thorsten.onnx"));
        }

        // Register ElevenLabs if API key exists
        java.util.Properties props = new java.util.Properties();
        try (InputStream is = new FileInputStream("../../fasttts.properties")) {
            props.load(is);
            String elKey = props.getProperty("elevenlabs.api.key");
            if (elKey != null && !elKey.isEmpty()) {
                String defVoice = props.getProperty("elevenlabs.default.voice", "21m00Tcm4TlvDq8ikWAM");
                float stability = Float.parseFloat(props.getProperty("elevenlabs.default.stability", "0.5"));
                float similarity = Float.parseFloat(props.getProperty("elevenlabs.default.similarity", "0.75"));
                tts.registerBackend(new ElevenLabsBackend(elKey, defVoice, stability, similarity));
            }
        } catch (Exception ignored) {}

        System.out.println("=== FastTTS Multi-Engine Demo ===");
        List<FastTTSVoice> voices = tts.getAllVoices();
        for (int i = 0; i < voices.size(); i++) {
            FastTTSVoice v = voices.get(i);
            System.out.println((i + 1) + ". [" + v.backendId() + "] " + v.name());
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
                byte[] audio = tts.speak(selected.backendId(), text, selected, null);
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
