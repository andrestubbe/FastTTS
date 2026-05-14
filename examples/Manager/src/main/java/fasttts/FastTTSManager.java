package fasttts;

import fasttts.core.*;
import fasttts.backends.windows.*;
import fasttts.backends.piper.*;
import java.util.List;
import java.util.Scanner;
import java.io.*;

/**
 * FastTTS Manager - Console-based installer and configuration tool.
 */
public class FastTTSManager {

    private final FastTTS tts;
    private final Scanner scanner = new Scanner(System.in);

    public FastTTSManager(FastTTS tts) {
        this.tts = tts;
    }

    public void run() {
        while (true) {
            clearConsole();
            System.out.println("========================================");
            System.out.println("   FastTTS Manager — Native & Modular");
            System.out.println("========================================\n");

            System.out.println("  1.  [Windows]  Manage System Voices");
            System.out.println("  2.  [Piper]    Install / Manage Offline Models");
            System.out.println("  3.  [Cloud]    Configure ElevenLabs / Azure");
            System.out.println("  q.  Quit");

            System.out.print("\nChoose an option: ");
            String choice = scanner.nextLine();

            switch (choice.toLowerCase()) {
                case "1": manageWindowsVoices(); break;
                case "2": managePiper(); break;
                case "3": configureCloud(); break;
                case "q": return;
            }
        }
    }

    private void manageWindowsVoices() {
        while (true) {
            clearConsole();
            System.out.println("--- Windows System Voices ---");
            List<FastTTSVoice> voices = tts.getAllVoices();
            for (int i = 0; i < voices.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + voices.get(i).name());
            }
            System.out.println("\n  [t] Test current active");
            System.out.println("  [b] Back");

            System.out.print("\nCommand: ");
            String cmd = scanner.nextLine().toLowerCase();

            if (cmd.equals("b")) return;
            if (cmd.equals("t")) {
                System.out.println("Synthesizing test phrase...");
                try {
                    tts.speak("Fast TTS is working perfectly.");
                } catch (Exception e) {
                    System.err.println("Test failed: " + e.getMessage());
                }
            }
        }
    }

    private void managePiper() {
        clearConsole();
        System.out.println("--- Piper Offline TTS ---");
        File piperExe = new File("piper.exe");
        if (!piperExe.exists()) {
            System.out.println("Piper is NOT installed.");
            System.out.print("Do you want to download it now? (y/n): ");
            if (scanner.nextLine().equalsIgnoreCase("y")) {
                downloadPiper();
            }
        } else {
            System.out.println("Piper is installed.");
            System.out.println("Voice Models:");
            File[] models = new File(".").listFiles((d, n) -> n.endsWith(".onnx"));
            if (models != null) {
                for (File m : models) System.out.println("  - " + m.getName());
            }
        }
        System.out.println("\n[Press Enter to return]");
        scanner.nextLine();
    }

    private void downloadPiper() {
        System.out.println("\n[INFO] Downloading Piper (Windows x64)...");
        String zipUrl = "https://github.com/rhasspy/piper/releases/download/2023.11.14-2/piper_windows_amd64.zip";
        try {
            String psCommand = String.format(
                "$ProgressPreference = 'SilentlyContinue'; " +
                "Invoke-WebRequest -Uri '%s' -OutFile 'piper.zip'; " +
                "Expand-Archive -Path 'piper.zip' -DestinationPath 'piper_root' -Force; " +
                "Copy-Item -Path 'piper_root\\piper\\*' -Destination '.' -Recurse -Force; " +
                "rm -r piper_root; rm piper.zip", zipUrl
            );
            new ProcessBuilder("powershell", "-Command", psCommand).inheritIO().start().waitFor();
            
            System.out.println("[INFO] Downloading Thorsten Voice Model...");
            String modelUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/de/de_DE/thorsten/low/de_DE-thorsten-low.onnx";
            String voicePs = String.format("Invoke-WebRequest -Uri '%s' -OutFile 'thorsten.onnx'; " +
                                         "Invoke-WebRequest -Uri '%s.json' -OutFile 'thorsten.onnx.json'", modelUrl, modelUrl);
            new ProcessBuilder("powershell", "-Command", voicePs).inheritIO().start().waitFor();
            
            System.out.println("[SUCCESS] Piper and Thorsten voice model installed.");
        } catch (Exception e) {
            System.err.println("[ERROR] Installation failed: " + e.getMessage());
        }
    }

    private void configureCloud() {
        clearConsole();
        System.out.println("--- Cloud TTS Configuration ---");
        System.out.println("API Keys can be configured here.");
        System.out.println("\n[Press Enter to return]");
        scanner.nextLine();
    }

    private void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        FastTTS tts = new FastTTS();
        tts.registerBackend(new WindowsTTSBackend());
        tts.use("windows");
        
        FastTTSManager manager = new FastTTSManager(tts);
        manager.run();
    }
}
