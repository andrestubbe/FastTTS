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
                tts.speak("Fast TTS is working perfectly.");
            }
        }
    }

    private void managePiper() {
        clearConsole();
        System.out.println("--- Piper Offline TTS ---");
        System.out.println("Piper integration is ready for model management.");
        System.out.println("\n[Press Enter to return]");
        scanner.nextLine();
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
