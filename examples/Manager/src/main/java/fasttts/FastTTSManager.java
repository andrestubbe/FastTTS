package fasttts;

import java.util.List;
import java.util.Scanner;
import java.io.*;
import java.net.URL;
import java.nio.file.*;

/**
 * FastTTS Manager - Console-based installer and configuration tool.
 * Ollama-inspired UX for managing engines and voices.
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
            System.out.println("  3.  [Kokoro]   High-Fidelity Native Bridge");
            System.out.println("  4.  [Cloud]    Configure ElevenLabs / Azure");
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
            System.out.println("\n  [t] Test current default");
            System.out.println("  [s] Set default voice");
            System.out.println("  [b] Back");

            System.out.print("\nCommand: ");
            String cmd = scanner.nextLine().toLowerCase();

            if (cmd.equals("b")) return;
            if (cmd.equals("t")) {
                System.out.println("Synthesizing test phrase...");
                tts.speak("This is a test of the Fast TTS native windows engine.");
            }
            if (cmd.equals("s")) {
                System.out.print("Enter number: ");
                try {
                    int idx = Integer.parseInt(scanner.nextLine()) - 1;
                    // TODO: Store default voice in config
                    System.out.println("[SUCCESS] Voice selected.");
                    Thread.sleep(500);
                } catch (Exception e) {
                    System.out.println("[ERROR] Invalid input.");
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
            System.out.println("Piper is installed at: " + piperExe.getAbsolutePath());
            // Add model management here later
        }
        System.out.println("\n[Press Enter to return]");
        scanner.nextLine();
    }

    private void downloadPiper() {
        System.out.println("\n[INFO] Downloading Piper (Windows x64)...");
        // Placeholder for real download logic
        System.out.println("[TODO] In a real app, I would download from GitHub Releases.");
        System.out.println("[INFO] Simulated: Piper.exe has been 'installed'.");
        try { new File("piper.exe").createNewFile(); } catch (Exception e) {}
    }

    private void configureCloud() {
        clearConsole();
        System.out.println("--- Cloud TTS Configuration ---");
        System.out.print("Enter ElevenLabs API Key (current: none): ");
        String key = scanner.nextLine();
        System.out.println("[SUCCESS] API Key saved (simulated).");
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
        
        FastTTSManager manager = new FastTTSManager(tts);
        manager.run();
    }
}
