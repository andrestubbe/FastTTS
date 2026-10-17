package fasttts.installer;

import fastaudio.FastAudioPlayer;
import fasttts.FastTTS;
import fasttts.core.FastTTSAudio;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * FastTTS Local Engine & Model Installer.
 * Manages completely self-contained local installations of all TTS backends,
 * models, and native binaries under ./bin and ./models.
 *
 * Supports CLI:
 *   --install [all|kokoro|pocket|piper|omnivoice|dia2]
 *   --deinstall [all|kokoro|pocket|piper|omnivoice|dia2]
 *   --status
 */
public class FastTTSInstaller {

    private final Scanner scanner = new Scanner(System.in);
    private final File rootDir;

    // Direct download endpoints
    private static final String SHERPA_RELEASE_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-v1.13.8-win-x64-shared-MT-Release.tar.bz2";
    private static final String KOKORO_MODEL_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/kokoro-en-v0_19.tar.bz2";
    private static final String POCKET_MODEL_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/sherpa-onnx-pocket-tts-int8-2026-01-26.tar.bz2";
    private static final String PIPER_BIN_URL =
            "https://github.com/rhasspy/piper/releases/download/2023.11.14-2/piper_windows_amd64.zip";
    private static final String PIPER_THORSTEN_ONNX =
            "https://huggingface.co/rhasspy/piper-voices/resolve/main/de/de_DE/thorsten/medium/de_DE-thorsten-medium.onnx";
    private static final String PIPER_THORSTEN_JSON =
            "https://huggingface.co/rhasspy/piper-voices/resolve/main/de/de_DE/thorsten/medium/de_DE-thorsten-medium.onnx.json";

    public FastTTSInstaller() {
        this.rootDir = findProjectRoot();
    }

    public static void main(String[] args) {
        FastTTSInstaller installer = new FastTTSInstaller();
        if (args.length > 0) {
            installer.handleCommandLine(args);
        } else {
            installer.runInteractive();
        }
    }

    private File findProjectRoot() {
        File current = new File(".").getAbsoluteFile();
        while (current != null) {
            File pom = new File(current, "pom.xml");
            File binDir = new File(current, "bin");
            File modelsDir = new File(current, "models");
            if (pom.exists() && (binDir.exists() || modelsDir.exists())) {
                try {
                    return current.getCanonicalFile();
                } catch (IOException e) {
                    return current;
                }
            }
            current = current.getParentFile();
        }
        return new File(".").getAbsoluteFile();
    }

    public void runInteractive() {
        while (true) {
            clearConsole();
            printDashboard();

            System.out.println("Actions:");
            System.out.println("  1.  [Install All]   Install all standard offline engines & models (Kokoro, Pocket, Piper)");
            System.out.println("  2.  [Deinstall All] Deinstall/Delete all local models & binaries (clean reset)");
            System.out.println("  3.  [Kokoro]        Manage Kokoro Studio (Sherpa-ONNX)");
            System.out.println("  4.  [Pocket TTS]    Manage Pocket TTS INT8 (Sherpa-ONNX)");
            System.out.println("  5.  [Piper]         Manage Piper Neural (ONNX)");
            System.out.println("  6.  [OmniVoice]     Manage OmniVoice (Diffusion Zero-Shot)");
            System.out.println("  7.  [Dia2]          Manage Dia2 Scripted Dialogue (GGUF)");
            System.out.println("  8.  [Audio Test]    Test Native Audio Playback (WASAPI)");
            System.out.println("  9.  [Benchmark]     Run Multi-Engine Benchmark Suite");
            System.out.println("  q.  Quit");
            System.out.println();
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim().toLowerCase();
            switch (choice) {
                case "1" -> installAll();
                case "2" -> deinstallAll();
                case "3" -> manageKokoro();
                case "4" -> managePocket();
                case "5" -> managePiper();
                case "6" -> manageOmniVoice();
                case "7" -> manageDia2();
                case "8" -> testAudio();
                case "9" -> runBenchmark();
                case "q" -> {
                    System.out.println("Exiting FastTTS Installer. Good bye!");
                    return;
                }
            }
        }
    }

    private void handleCommandLine(String[] args) {
        String cmd = args[0].toLowerCase();
        switch (cmd) {
            case "--status", "-s" -> printDashboard();
            case "--install", "-i" -> {
                String target = (args.length > 1) ? args[1].toLowerCase() : "all";
                switch (target) {
                    case "all" -> installAll();
                    case "kokoro" -> installKokoro();
                    case "pocket", "pockettts" -> installPocket();
                    case "piper" -> installPiper();
                    case "omnivoice" -> installOmniVoice();
                    case "dia2" -> installDia2();
                    default -> System.err.println("Unknown install target: " + target);
                }
            }
            case "--deinstall", "--uninstall", "-u" -> {
                String target = (args.length > 1) ? args[1].toLowerCase() : "all";
                switch (target) {
                    case "all" -> deinstallAll();
                    case "kokoro" -> deinstallKokoro();
                    case "pocket", "pockettts" -> deinstallPocket();
                    case "piper" -> deinstallPiper();
                    case "omnivoice" -> deinstallOmniVoice();
                    case "dia2" -> deinstallDia2();
                    default -> System.err.println("Unknown deinstall target: " + target);
                }
            }
            case "--download", "-d" -> {
                String target = (args.length > 1) ? args[1].toLowerCase() : "all";
                handleCommandLine(new String[]{"--install", target});
            }
            default -> {
                System.err.println("Usage:");
                System.err.println("  --status                              Print current installation status");
                System.err.println("  --install [all|kokoro|pocket|piper]   Install local engines & models");
                System.err.println("  --deinstall [all|kokoro|pocket|piper] Remove local engines & models");
            }
        }
    }

    public void printDashboard() {
        System.out.println("================================================================================");
        System.out.println("                       FastTTS Engine & Model Manager");
        System.out.println("================================================================================");
        System.out.println("Root Directory: " + rootDir.getAbsolutePath());
        System.out.println("Local Storage:  All models & binaries strictly contained in ./models and ./bin");
        System.out.println("--------------------------------------------------------------------------------");

        // Windows SAPI
        System.out.printf("  %-22s | %-12s | %s%n", "Windows SAPI/OneCore", "[INSTALLED]", "Built-in Windows native engine");

        // Kokoro Studio
        boolean kokoroBin = new File(rootDir, "bin/kokoro/sherpa-onnx-offline-tts.exe").exists();
        boolean kokoroWeights = new File(rootDir, "models/kokoro/kokoro-en-v0_19/model.onnx").exists();
        String kokoroStatus = (kokoroBin && kokoroWeights) ? "[INSTALLED]" : (kokoroBin ? "[NO WEIGHTS]" : "[MISSING]");
        System.out.printf("  %-22s | %-12s | bin/kokoro + models/kokoro/kokoro-en-v0_19%n", "Kokoro Studio (ONNX)", kokoroStatus);

        // Pocket TTS
        boolean pocketBin = new File(rootDir, "bin/pocket/sherpa-onnx-offline-tts.exe").exists();
        boolean pocketWeights = new File(rootDir, "models/pocket/sherpa-onnx-pocket-tts-int8-2026-01-26/lm_main.int8.onnx").exists();
        String pocketStatus = (pocketBin && pocketWeights) ? "[INSTALLED]" : (pocketBin ? "[NO WEIGHTS]" : "[MISSING]");
        System.out.printf("  %-22s | %-12s | bin/pocket + models/pocket/sherpa-onnx-pocket-tts-int8%n", "Pocket TTS (INT8)", pocketStatus);

        // Piper Neural
        boolean piperBin = new File(rootDir, "bin/piper/piper.exe").exists();
        File piperDir = new File(rootDir, "models/piper");
        File[] piperModels = piperDir.listFiles((d, n) -> n.endsWith(".onnx"));
        int piperCount = (piperModels != null) ? piperModels.length : 0;
        String piperStatus = (piperBin && piperCount > 0) ? "[INSTALLED: " + piperCount + "]" : (piperBin ? "[NO VOICES]" : "[MISSING]");
        System.out.printf("  %-22s | %-12s | bin/piper + " + piperCount + " voices in models/piper/%n", "Piper Neural (ONNX)", piperStatus);

        // OmniVoice
        File omniDir = new File(rootDir, "models/omnivoice");
        File omniWeights = new File(omniDir, "model.safetensors");
        String omniStatus;
        if (omniWeights.exists() && omniWeights.length() > 2_400_000_000L) {
            omniStatus = "[INSTALLED]";
        } else if (omniWeights.exists()) {
            omniStatus = "[" + (omniWeights.length() / (1024 * 1024)) + " MB / 2450 MB]";
        } else {
            omniStatus = "[MISSING]";
        }
        System.out.printf("  %-22s | %-12s | models/omnivoice (Zero-Shot Diffusion)%n", "OmniVoice", omniStatus);

        // Dia2 Dialogue
        boolean dia2Bin = new File(rootDir, "bin/dia2/audiocpp_cli.exe").exists();
        boolean dia2Weights = new File(rootDir, "models/dia2/dia2-2b-q4_k.gguf").exists();
        String dia2Status = (dia2Bin && dia2Weights) ? "[INSTALLED]" : (dia2Weights ? "[NO RUNNER]" : "[MISSING]");
        System.out.printf("  %-22s | %-12s | bin/dia2 + models/dia2 (GGUF 2B)%n", "Dia2 Dialogue", dia2Status);

        // FastAudioPlayer
        boolean fap = new File(rootDir, "bin/fastaudioplayer.dll").exists();
        System.out.printf("  %-22s | %-12s | bin/fastaudioplayer.dll (WASAPI Native)%n", "FastAudioPlayer", fap ? "[INSTALLED]" : "[MISSING]");

        System.out.println("--------------------------------------------------------------------------------\n");
    }

    // =========================================================================
    // Master Install / Deinstall
    // =========================================================================

    public void installAll() {
        System.out.println("\n>>> [FastTTS] Installing all standard offline engines locally...");
        installKokoro();
        installPocket();
        installPiper();
        System.out.println("\n>>> [FastTTS] Installation of standard offline engines complete!\n");
        printDashboard();
    }

    public void deinstallAll() {
        System.out.println("\n>>> [FastTTS] Deinstalling all local models & binaries...");
        deinstallKokoro();
        deinstallPocket();
        deinstallPiper();
        deinstallOmniVoice();
        deinstallDia2();
        System.out.println("\n>>> [FastTTS] Clean deinstallation complete!\n");
        printDashboard();
    }

    // =========================================================================
    // 1. Kokoro Studio
    // =========================================================================

    public void installKokoro() {
        System.out.println("\n--- [Kokoro Studio] Installing Binaries & Model Weights ---");
        ensureSherpaBinaries(new File(rootDir, "bin/kokoro"));

        File kokoroDir = new File(rootDir, "models/kokoro");
        File targetDir = new File(kokoroDir, "kokoro-en-v0_19");
        File modelFile = new File(targetDir, "model.onnx");

        if (modelFile.exists() && modelFile.length() > 300_000_000L) {
            System.out.println("[Kokoro] Model weights already present: " + modelFile.getAbsolutePath());
        } else {
            kokoroDir.mkdirs();
            File tarBz2 = new File(kokoroDir, "kokoro-en-v0_19.tar.bz2");
            System.out.println("[Kokoro] Downloading kokoro-en-v0_19.tar.bz2 (~345 MB)...");
            downloadWithCurl(KOKORO_MODEL_URL, tarBz2);
            if (tarBz2.exists() && tarBz2.length() > 0) {
                System.out.println("[Kokoro] Extracting archive...");
                extractTarBz2(tarBz2, kokoroDir);
                tarBz2.delete();
            }
        }
        System.out.println("[SUCCESS] Kokoro Studio installed.");
    }

    public void deinstallKokoro() {
        System.out.println("[Deinstall] Removing Kokoro binaries and models...");
        deleteDirectory(new File(rootDir, "bin/kokoro"));
        deleteDirectory(new File(rootDir, "models/kokoro"));
        System.out.println("[SUCCESS] Kokoro deinstalled.");
    }

    private void manageKokoro() {
        while (true) {
            clearConsole();
            System.out.println("--- Kokoro Studio Management ---");
            File bin = new File(rootDir, "bin/kokoro/sherpa-onnx-offline-tts.exe");
            File modelDir = new File(rootDir, "models/kokoro/kokoro-en-v0_19");
            System.out.println("Binary:  " + (bin.exists() ? "✓ Installed (bin/kokoro)" : "✗ Missing"));
            System.out.println("Model:   " + (new File(modelDir, "model.onnx").exists() ? "✓ Installed (" + (new File(modelDir, "model.onnx").length() / (1024 * 1024)) + " MB)" : "✗ Missing"));
            System.out.println();
            System.out.println("Options:");
            System.out.println("  1. Install Kokoro Studio");
            System.out.println("  2. Deinstall Kokoro Studio");
            System.out.println("  3. Test Kokoro Voice Output");
            System.out.println("  b. Back");
            System.out.println();
            System.out.print("Command: ");

            String cmd = scanner.nextLine().trim().toLowerCase();
            if (cmd.equals("b") || cmd.equals("back")) return;

            switch (cmd) {
                case "1" -> { installKokoro(); pause(); }
                case "2" -> { deinstallKokoro(); pause(); }
                case "3" -> testKokoroVoice();
            }
        }
    }

    private void testKokoroVoice() {
        System.out.println("\nSynthesizing test speech with Kokoro (af_bella)...");
        try {
            FastTTS tts = FastTTS.standard();
            FastTTSAudio audio = tts.speak("kokoro", "Hello! Kokoro studio neural speech is working locally.", null, null);
            if (audio != null) {
                playAudio(audio.getData());
                System.out.println("[SUCCESS] Playback complete.");
            }
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
        }
        pause();
    }

    // =========================================================================
    // 2. Pocket TTS
    // =========================================================================

    public void installPocket() {
        System.out.println("\n--- [Pocket TTS] Installing Binaries & INT8 Weights ---");
        ensureSherpaBinaries(new File(rootDir, "bin/pocket"));

        File pocketDir = new File(rootDir, "models/pocket");
        File targetDir = new File(pocketDir, "sherpa-onnx-pocket-tts-int8-2026-01-26");
        File lmMain = new File(targetDir, "lm_main.int8.onnx");

        if (lmMain.exists() && lmMain.length() > 50_000_000L) {
            System.out.println("[Pocket TTS] Model weights already present: " + lmMain.getAbsolutePath());
        } else {
            pocketDir.mkdirs();
            File tarBz2 = new File(pocketDir, "pocket_tts.tar.bz2");
            System.out.println("[Pocket TTS] Downloading sherpa-onnx-pocket-tts-int8-2026-01-26.tar.bz2 (~98 MB)...");
            downloadWithCurl(POCKET_MODEL_URL, tarBz2);
            if (tarBz2.exists() && tarBz2.length() > 0) {
                System.out.println("[Pocket TTS] Extracting archive...");
                extractTarBz2(tarBz2, pocketDir);
                tarBz2.delete();
            }
        }
        System.out.println("[SUCCESS] Pocket TTS installed.");
    }

    public void deinstallPocket() {
        System.out.println("[Deinstall] Removing Pocket TTS binaries and models...");
        deleteDirectory(new File(rootDir, "bin/pocket"));
        deleteDirectory(new File(rootDir, "models/pocket"));
        System.out.println("[SUCCESS] Pocket TTS deinstalled.");
    }

    private void managePocket() {
        while (true) {
            clearConsole();
            System.out.println("--- Pocket TTS Management ---");
            File bin = new File(rootDir, "bin/pocket/sherpa-onnx-offline-tts.exe");
            File modelDir = new File(rootDir, "models/pocket/sherpa-onnx-pocket-tts-int8-2026-01-26");
            System.out.println("Binary:  " + (bin.exists() ? "✓ Installed (bin/pocket)" : "✗ Missing"));
            System.out.println("Model:   " + (new File(modelDir, "lm_main.int8.onnx").exists() ? "✓ Installed" : "✗ Missing"));
            System.out.println();
            System.out.println("Options:");
            System.out.println("  1. Install Pocket TTS");
            System.out.println("  2. Deinstall Pocket TTS");
            System.out.println("  3. Test Pocket TTS Output");
            System.out.println("  b. Back");
            System.out.println();
            System.out.print("Command: ");

            String cmd = scanner.nextLine().trim().toLowerCase();
            if (cmd.equals("b") || cmd.equals("back")) return;

            switch (cmd) {
                case "1" -> { installPocket(); pause(); }
                case "2" -> { deinstallPocket(); pause(); }
                case "3" -> testPocketVoice();
            }
        }
    }

    private void testPocketVoice() {
        System.out.println("\nSynthesizing test speech with Pocket TTS...");
        try {
            FastTTS tts = FastTTS.standard();
            FastTTSAudio audio = tts.speak("pocket", "Pocket TTS compact neural voice is working locally.", null, null);
            if (audio != null) {
                playAudio(audio.getData());
                System.out.println("[SUCCESS] Playback complete.");
            }
        } catch (Exception e) {
            System.err.println("Pocket test failed: " + e.getMessage());
        }
        pause();
    }

    // =========================================================================
    // 3. Piper Neural
    // =========================================================================

    public void installPiper() {
        System.out.println("\n--- [Piper Neural] Installing Binaries & Thorsten Model ---");
        File binDir = new File(rootDir, "bin/piper");
        File piperExe = new File(binDir, "piper.exe");

        if (piperExe.exists()) {
            System.out.println("[Piper] Binary already present: " + piperExe.getAbsolutePath());
        } else {
            binDir.mkdirs();
            File tempZip = new File(binDir, "piper_temp.zip");
            System.out.println("[Piper] Downloading Piper Windows amd64 Release (~22 MB)...");
            downloadWithCurl(PIPER_BIN_URL, tempZip);
            if (tempZip.exists() && tempZip.length() > 0) {
                System.out.println("[Piper] Extracting piper binaries...");
                extractZipPowerShell(tempZip, binDir);
                tempZip.delete();
                // Move inner files if extracted into subfolder
                flattenSubfolder(binDir, "piper");
            }
        }

        File modelsDir = new File(rootDir, "models/piper");
        modelsDir.mkdirs();
        File onnxFile = new File(modelsDir, "de_DE-thorsten-medium.onnx");
        File jsonFile = new File(modelsDir, "de_DE-thorsten-medium.onnx.json");

        if (!onnxFile.exists() || onnxFile.length() < 10_000_000L) {
            System.out.println("[Piper] Downloading de_DE-thorsten-medium.onnx (~63 MB)...");
            downloadWithCurl(PIPER_THORSTEN_ONNX, onnxFile);
        }
        if (!jsonFile.exists()) {
            System.out.println("[Piper] Downloading de_DE-thorsten-medium.onnx.json...");
            downloadWithCurl(PIPER_THORSTEN_JSON, jsonFile);
        }

        System.out.println("[SUCCESS] Piper Neural installed.");
    }

    public void deinstallPiper() {
        System.out.println("[Deinstall] Removing Piper binaries and models...");
        deleteDirectory(new File(rootDir, "bin/piper"));
        deleteDirectory(new File(rootDir, "models/piper"));
        System.out.println("[SUCCESS] Piper deinstalled.");
    }

    private void managePiper() {
        while (true) {
            clearConsole();
            System.out.println("--- Piper Neural Management ---");
            File bin = new File(rootDir, "bin/piper/piper.exe");
            File modelsDir = new File(rootDir, "models/piper");
            File[] voices = modelsDir.listFiles((d, n) -> n.endsWith(".onnx"));

            System.out.println("Piper Binary: " + (bin.exists() ? "✓ Installed (bin/piper/piper.exe)" : "✗ Missing"));
            System.out.println("Installed Voices (models/piper):");
            if (voices != null && voices.length > 0) {
                for (File v : voices) {
                    System.out.printf("  ✓ %-35s (%s MB)%n", v.getName(), String.format("%.1f", v.length() / (1024.0 * 1024.0)));
                }
            } else {
                System.out.println("  (No voices installed in models/piper)");
            }

            System.out.println();
            System.out.println("Options:");
            System.out.println("  1. Install Piper & Thorsten German Voice");
            System.out.println("  2. Deinstall Piper");
            System.out.println("  3. Test Piper Voice");
            System.out.println("  b. Back");
            System.out.println();
            System.out.print("Command: ");

            String cmd = scanner.nextLine().trim().toLowerCase();
            if (cmd.equals("b") || cmd.equals("back")) return;

            switch (cmd) {
                case "1" -> { installPiper(); pause(); }
                case "2" -> { deinstallPiper(); pause(); }
                case "3" -> testPiperVoice();
            }
        }
    }

    private void testPiperVoice() {
        System.out.println("\nSynthesizing test speech with Piper...");
        try {
            FastTTS tts = FastTTS.standard();
            FastTTSAudio audio = tts.speak("piper", "Das ist ein Sprachtest mit Piper Neural lokal in FastTTS.", null, null);
            if (audio != null) {
                playAudio(audio.getData());
                System.out.println("[SUCCESS] Playback complete.");
            }
        } catch (Exception e) {
            System.err.println("Piper test failed: " + e.getMessage());
        }
        pause();
    }

    // =========================================================================
    // 4. OmniVoice
    // =========================================================================

    public void installOmniVoice() {
        System.out.println("\n--- [OmniVoice] Installing Model Weights (2.45 GB) ---");
        File omniDir = new File(rootDir, "models/omnivoice");
        omniDir.mkdirs();
        File target = new File(omniDir, "model.safetensors");

        if (target.exists() && target.length() > 2_400_000_000L) {
            System.out.println("[OmniVoice] Weights already complete (" + (target.length() / (1024 * 1024)) + " MB)");
        } else {
            String url = "https://huggingface.co/k2-fsa/OmniVoice/resolve/main/model.safetensors";
            downloadWithCurl(url, target);
        }
        System.out.println("[SUCCESS] OmniVoice installed.");
    }

    public void deinstallOmniVoice() {
        System.out.println("[Deinstall] Removing OmniVoice models...");
        deleteDirectory(new File(rootDir, "models/omnivoice"));
        System.out.println("[SUCCESS] OmniVoice deinstalled.");
    }

    private void manageOmniVoice() {
        clearConsole();
        System.out.println("--- OmniVoice Zero-Shot Voice Cloning ---");
        File omniDir = new File(rootDir, "models/omnivoice");
        File weights = new File(omniDir, "model.safetensors");

        System.out.println("Directory:     " + omniDir.getAbsolutePath());
        System.out.println("Model Weights: " + (weights.exists() ? (weights.length() > 2_400_000_000L ? "✓ Complete (" + (weights.length() / (1024 * 1024)) + " MB)" : "⏳ Incomplete (" + (weights.length() / (1024 * 1024)) + " / 2450 MB)") : "✗ Missing"));
        System.out.println();
        System.out.println("Options:");
        System.out.println("  1. Install / Resume OmniVoice Weights (model.safetensors)");
        System.out.println("  2. Deinstall OmniVoice");
        System.out.println("  b. Back");
        System.out.println();
        System.out.print("Command: ");

        String cmd = scanner.nextLine().trim().toLowerCase();
        switch (cmd) {
            case "1" -> { installOmniVoice(); pause(); }
            case "2" -> { deinstallOmniVoice(); pause(); }
        }
    }

    // =========================================================================
    // 5. Dia2 Dialogue
    // =========================================================================

    public void installDia2() {
        System.out.println("\n--- [Dia2 Dialogue] Installing Runner & Model Weights ---");
        File binDir = new File(rootDir, "bin/dia2");
        File runner = new File(binDir, "audiocpp_cli.exe");

        if (runner.exists()) {
            System.out.println("[Dia2] audio.cpp runner already present: " + runner.getAbsolutePath());
        } else {
            binDir.mkdirs();
            File tempZip = new File(binDir, "audiocpp_bin.zip");
            String url = "https://github.com/0xShug0/audio.cpp/releases/download/v0.9.2/audio-v0.9.2-bin-windows-x64-cpu.zip";
            System.out.println("[Dia2] Downloading audio.cpp Windows x64 runner (~27 MB)...");
            downloadWithCurl(url, tempZip);
            if (tempZip.exists() && tempZip.length() > 0) {
                System.out.println("[Dia2] Extracting binaries...");
                extractZipPowerShell(tempZip, binDir);
                tempZip.delete();
            }
        }

        File dia2Dir = new File(rootDir, "models/dia2");
        dia2Dir.mkdirs();
        File target = new File(dia2Dir, "dia2-2b-q4_k.gguf");
        if (target.exists() && target.length() > 1_000_000_000L) {
            System.out.println("[Dia2] GGUF Model weights already present (" + (target.length() / (1024 * 1024)) + " MB)");
        } else {
            System.out.println("[Dia2] Downloading Dia2 GGUF weights (1.1 GB)...");
            String dia2Url = "https://huggingface.co/markldn/Dia2-2B-GGUF/resolve/main/dia2-q4_0.gguf";
            downloadWithCurl(dia2Url, target);
        }
        System.out.println("[SUCCESS] Dia2 installed.");
    }

    public void deinstallDia2() {
        System.out.println("[Deinstall] Removing Dia2 models and binaries...");
        deleteDirectory(new File(rootDir, "models/dia2"));
        deleteDirectory(new File(rootDir, "bin/dia2"));
        System.out.println("[SUCCESS] Dia2 deinstalled.");
    }

    private void manageDia2() {
        while (true) {
            clearConsole();
            System.out.println("--- Dia2 Multi-Speaker Dialogue (GGUF) ---");
            File dia2Dir = new File(rootDir, "models/dia2");
            File weights = new File(dia2Dir, "dia2-2b-q4_k.gguf");
            File runner = new File(rootDir, "bin/dia2/audiocpp_cli.exe");

            System.out.println("GGUF Weights: " + (weights.exists() ? "✓ Installed (" + (weights.length() / (1024 * 1024)) + " MB)" : "✗ Missing"));
            System.out.println("CLI Runner:   " + (runner.exists() ? "✓ Installed (bin/dia2/audiocpp_cli.exe)" : "✗ Missing"));
            System.out.println();
            System.out.println("Options:");
            System.out.println("  1. Install Dia2 Runner & GGUF Model");
            System.out.println("  2. Deinstall Dia2");
            System.out.println("  3. Test Dia2 Dialogue Synthesis");
            System.out.println("  b. Back");
            System.out.println();
            System.out.print("Command: ");

            String cmd = scanner.nextLine().trim().toLowerCase();
            if (cmd.equals("b") || cmd.equals("back")) return;

            switch (cmd) {
                case "1" -> { installDia2(); pause(); }
                case "2" -> { deinstallDia2(); pause(); }
                case "3" -> testDia2Voice();
            }
        }
    }

    private void testDia2Voice() {
        System.out.println("\nSynthesizing dialogue speech with Dia2...");
        try {
            FastTTS tts = FastTTS.standard();
            FastTTSAudio audio = tts.speak("dia2", "[S1] Welcome to FastTTS! [S2] Dia2 multi-speaker dialogue is active.", null, null);
            if (audio != null) {
                playAudio(audio.getData());
                System.out.println("[SUCCESS] Playback complete.");
            }
        } catch (Exception e) {
            System.err.println("Dia2 test failed: " + e.getMessage());
        }
        pause();
    }

    // =========================================================================
    // Shared Sherpa-ONNX Runner Setup
    // =========================================================================

    private void ensureSherpaBinaries(File targetBinDir) {
        File exe = new File(targetBinDir, "sherpa-onnx-offline-tts.exe");
        if (exe.exists() && exe.length() > 1_000_000L) {
            return;
        }

        // Check if already in sister folder or backup
        File[] sources = new File[]{
                new File(rootDir, "bin/sherpa-onnx"),
                new File(rootDir, "bin/kokoro"),
                new File(rootDir, "bin/pocket"),
                new File(rootDir, "target/backup_bin/kokoro"),
                new File(rootDir, "target/backup_bin/pocket")
        };
        for (File src : sources) {
            if (src.exists() && new File(src, "sherpa-onnx-offline-tts.exe").exists()) {
                System.out.println("[Sherpa-ONNX] Reusing local binaries from " + src.getName() + " -> " + targetBinDir.getName());
                copyDirectory(src, targetBinDir);
                return;
            }
        }

        // Download official release
        targetBinDir.mkdirs();
        File tempTar = new File(targetBinDir, "sherpa_release.tar.bz2");
        System.out.println("[Sherpa-ONNX] Downloading Sherpa-ONNX v1.13.8 Windows x64 binaries (~24 MB)...");
        downloadWithCurl(SHERPA_RELEASE_URL, tempTar);
        if (tempTar.exists() && tempTar.length() > 0) {
            System.out.println("[Sherpa-ONNX] Extracting binaries...");
            extractTarBz2(tempTar, targetBinDir);
            tempTar.delete();

            // Locate bin and move dll/exe up
            flattenSherpaBin(targetBinDir);
        }
    }

    private void flattenSherpaBin(File dir) {
        File[] found = dir.listFiles((d, n) -> n.startsWith("sherpa-onnx-v1.13.8"));
        if (found != null && found.length > 0) {
            File inner = found[0];
            File innerBin = new File(inner, "bin");
            File innerLib = new File(inner, "lib");
            if (innerBin.exists()) copyDirectory(innerBin, dir);
            if (innerLib.exists()) copyDirectory(innerLib, dir);
            deleteDirectory(inner);
        }
    }

    private void flattenSubfolder(File parentDir, String subname) {
        File sub = new File(parentDir, subname);
        if (sub.exists() && sub.isDirectory()) {
            copyDirectory(sub, parentDir);
            deleteDirectory(sub);
        }
    }

    // =========================================================================
    // Diagnostics & Audio
    // =========================================================================

    private void testAudio() {
        clearConsole();
        System.out.println("--- Native Audio Playback Test (WASAPI) ---");
        System.out.println("Testing fastaudio.FastAudioPlayer with a live synthesis...");

        try {
            FastTTS tts = FastTTS.standard();
            FastTTSAudio audio = tts.speak("windows", "Audio playback test for FastTTS native WASAPI subsystem.", null, null);
            if (audio != null) {
                System.out.println("Playing audio out loud...");
                playAudio(audio.getData());
                System.out.println("[SUCCESS] Playback complete.");
            }
        } catch (Exception e) {
            System.err.println("[Playback Error]: " + e.getMessage());
        }
        pause();
    }

    private void runBenchmark() {
        clearConsole();
        System.out.println("Starting FastTTS Multi-Engine Benchmark Suite...");
        try {
            new ProcessBuilder("cmd", "/c", "run-demo.bat", "benchmark").inheritIO().start().waitFor();
        } catch (Exception e) {
            System.err.println("Benchmark error: " + e.getMessage());
        }
        pause();
    }

    // =========================================================================
    // Utilities (Curl, Tar, Zip, Audio)
    // =========================================================================

    private void downloadWithCurl(String url, File targetFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "curl.exe", "-L", "-C", "-", "--retry", "5", "--progress-bar",
                    "-o", targetFile.getAbsolutePath(), url
            );
            pb.inheritIO();
            Process p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            System.err.println("[Curl Error]: " + e.getMessage());
        }
    }

    private void extractTarBz2(File archive, File destinationDir) {
        try {
            destinationDir.mkdirs();
            ProcessBuilder pb = new ProcessBuilder(
                    "tar.exe", "-xvf", archive.getAbsolutePath(), "-C", destinationDir.getAbsolutePath()
            );
            pb.inheritIO();
            Process p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            System.err.println("[Tar Error]: " + e.getMessage());
        }
    }

    private void extractZipPowerShell(File zipFile, File destinationDir) {
        try {
            destinationDir.mkdirs();
            String script = "Expand-Archive -Path '" + zipFile.getAbsolutePath() + "' -DestinationPath '" + destinationDir.getAbsolutePath() + "' -Force";
            Process p = new ProcessBuilder("powershell", "-NoProfile", "-Command", script).inheritIO().start();
            p.waitFor();
        } catch (Exception e) {
            System.err.println("[Extract Error]: " + e.getMessage());
        }
    }

    private void copyDirectory(File source, File destination) {
        try {
            Files.walk(source.toPath()).forEach(sourcePath -> {
                try {
                    Path targetPath = destination.toPath().resolve(source.toPath().relativize(sourcePath));
                    if (Files.isDirectory(sourcePath)) {
                        if (!Files.exists(targetPath)) Files.createDirectories(targetPath);
                    } else {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (Exception ignored) {}
    }

    private void deleteDirectory(File dir) {
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteDirectory(f);
                else f.delete();
            }
        }
        dir.delete();
    }

    private void playAudio(byte[] audioData) {
        try {
            FastAudioPlayer player = new FastAudioPlayer();
            if (player.load(audioData)) {
                player.play();
                long dur = player.getDuration();
                long t0 = System.currentTimeMillis();
                while (player.isPlaying()) {
                    Thread.sleep(10);
                    if (dur > 0 && (System.currentTimeMillis() - t0) > (dur + 200)) break;
                }
                player.close();
            }
        } catch (Exception ignored) {}
    }

    private void clearConsole() {
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (Exception e) {
            System.out.print("\033[H\033[2J");
            System.out.flush();
        }
    }

    private void pause() {
        System.out.println("\n[Press Enter to continue]");
        scanner.nextLine();
    }
}
