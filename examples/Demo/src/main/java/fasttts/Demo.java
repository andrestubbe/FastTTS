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

        Scanner scanner = new Scanner(System.in);
        while (true) {
            clearConsole();
            System.out.println("========================================");
            System.out.println("   FastTTS Multi-Engine Demo v0.2.2");
            System.out.println("========================================\n");
            
            List<FastTTSVoice> voices = tts.getAllVoices();
            for (int i = 0; i < voices.size(); i++) {
                FastTTSVoice v = voices.get(i);
                System.out.println("  " + (i + 1) + ". [" + v.backendId() + "] " + v.name());
            }
            System.out.println("  q. Quit");

            System.out.print("\nSelect voice (1-" + voices.size() + ") or 'q': ");
            String choice = scanner.nextLine();
            if (choice.equalsIgnoreCase("q")) break;

            int index;
            try {
                index = Integer.parseInt(choice) - 1;
                if (index < 0 || index >= voices.size()) throw new Exception();
            } catch (Exception e) {
                System.out.println("Invalid selection.");
                continue;
            }

            FastTTSVoice selected = voices.get(index);
            while (true) {
                clearConsole();
                System.out.println("========================================");
                System.out.println("   SPEAKING: " + selected.name().toUpperCase());
                System.out.println("   BACKEND : " + selected.backendId().toUpperCase());
                System.out.println("========================================\n");
                System.out.println("(Type 'm' for menu, 'q' to quit)");

                System.out.print("\nEnter text: ");
                String text = scanner.nextLine();
                if (text.equalsIgnoreCase("m") || text.equalsIgnoreCase("b")) break;
                if (text.equalsIgnoreCase("q")) return;

                if (text.isEmpty()) continue;

                System.out.println("\n[Synthesizing...]");
                try {
                    byte[] audio = tts.speak(selected.backendId(), text, selected, null);
                    if (audio != null && audio.length > 0) {
                        playAudio(audio);
                    } else {
                        System.err.println("No audio generated.");
                    }
                } catch (Exception e) {
                    System.err.println("\n[ERROR] Synthesis failed: " + e.getMessage());
                    System.out.println("Press Enter to continue...");
                    scanner.nextLine();
                }
            }
        }
        System.out.println("Goodbye!");
    }

    private static void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception ignored) {}
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
                System.out.println("No WAV header found. Treating as raw PCM (16kHz, 16-bit, Mono)...");
                AudioFormat rawFormat = new AudioFormat(16000, 16, 1, true, false);
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
