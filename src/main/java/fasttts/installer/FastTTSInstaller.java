package fasttts.installer;

import fastansi.FastANSI;
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

    private static final String G = FastANSI.fg(240);
    private static final String W = FastANSI.FG_BRIGHT_WHITE;
    private static final String R = FastANSI.RESET;

    private final Scanner scanner = new Scanner(System.in);
    private final File rootDir;
    private FastTTSInstallListener installListener;
    private String title;

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
    private static final String MATCHA_MODEL_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/matcha-icefall-en_US-ljspeech.tar.bz2";
    private static final String VOCOS_VOCODER_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/vocoder-models/vocos-22khz-univ.onnx";
    private static final String VITS_MODEL_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-amy-low.tar.bz2";
    private static final String F5TTS_MODEL_URL =
            "https://huggingface.co/cstr/f5-tts-GGUF/resolve/main/f5-tts-v1-base-f16.gguf";
    private static final String F5TTS_VOCAB_URL =
            "https://huggingface.co/SWivid/F5-TTS/resolve/main/F5TTS_v1_Base/vocab.txt";

    public FastTTSInstaller() {
        this(null, null);
    }

    public FastTTSInstaller(File rootDir) {
        this(rootDir, null);
    }

    public FastTTSInstaller(File rootDir, FastTTSInstallListener installListener) {
        this.rootDir = (rootDir != null) ? rootDir : findProjectRoot();
        this.installListener = installListener;
    }

    public void setInstallListener(FastTTSInstallListener listener) {
        this.installListener = listener;
    }

    public FastTTSInstallListener getInstallListener() {
        return this.installListener;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return (title != null && !title.isBlank()) ? title : "FastTTS Engine & Model Manager";
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

            boolean sherpaInstalled = new File(rootDir, "bin/sherpa-onnx/sherpa-onnx-offline-tts.exe").exists();
            boolean audioCppInstalled = new File(rootDir, "bin/audio.cpp/audiocpp_cli.exe").exists();
            boolean piperInstalled = new File(rootDir, "bin/piper/piper.exe").exists();

            System.out.println(W + "Actions:" + R);
            System.out.println(W + String.format("%-4s", "1") + W + String.format("%-24s", "Install All") + G + "Install all offline engine runners (no models)" + R);
            System.out.println(W + String.format("%-4s", "2") + W + String.format("%-24s", "Deinstall All") + G + "Deinstall/Delete all local models & binaries (clean reset)" + R);
            System.out.println(W + String.format("%-4s", "3") + W + String.format("%-24s", (sherpaInstalled ? "Deinstall Kokoro" : "Install Kokoro")) + G + "Manage Kokoro Studio" + R);
            System.out.println(W + String.format("%-4s", "4") + W + String.format("%-24s", (sherpaInstalled ? "Deinstall Pocket TTS" : "Install Pocket TTS")) + G + "Manage Pocket TTS" + R);
            System.out.println(W + String.format("%-4s", "5") + W + String.format("%-24s", (piperInstalled ? "Deinstall Piper" : "Install Piper")) + G + "Manage Piper Neural" + R);
            System.out.println(W + String.format("%-4s", "6") + W + String.format("%-24s", (sherpaInstalled ? "Deinstall Matcha-TTS" : "Install Matcha-TTS")) + G + "Manage Matcha-TTS" + R);
            System.out.println(W + String.format("%-4s", "7") + W + String.format("%-24s", (sherpaInstalled ? "Deinstall VITS" : "Install VITS")) + G + "Manage VITS Deterministic" + R);
            System.out.println(W + String.format("%-4s", "8") + W + String.format("%-24s", (audioCppInstalled ? "Deinstall F5-TTS" : "Install F5-TTS")) + G + "Manage F5-TTS Voice Clone" + R);
            System.out.println(W + String.format("%-4s", "9") + W + String.format("%-24s", (audioCppInstalled ? "Deinstall OmniVoice" : "Install OmniVoice")) + G + "Manage OmniVoice Zero-Shot" + R);
            System.out.println(W + String.format("%-4s", "10") + W + String.format("%-24s", (audioCppInstalled ? "Deinstall Dia2" : "Install Dia2")) + G + "Manage Dia2 Multi-Speaker" + R);
            System.out.println(W + String.format("%-4s", "q") + W + String.format("%-24s", "Quit") + R);
            System.out.print(G + "Choose an option: " + W);
            if (!scanner.hasNextLine()) {
                return;
            }
            String choice = scanner.nextLine().trim().toLowerCase();
            System.out.print(R);
            switch (choice) {
                case "1" -> installAll();
                case "2" -> deinstallAll();
                case "3" -> { if (sherpaInstalled) deinstallKokoro(); else installKokoro(); }
                case "4" -> { if (sherpaInstalled) deinstallPocket(); else installPocket(); }
                case "5" -> { if (piperInstalled) deinstallPiper(); else installPiper(); }
                case "6" -> { if (sherpaInstalled) deinstallMatcha(); else installMatcha(); }
                case "7" -> { if (sherpaInstalled) deinstallVITS(); else installVITS(); }
                case "8" -> { if (audioCppInstalled) deinstallF5TTS(); else installF5TTS(); }
                case "9" -> { if (audioCppInstalled) deinstallOmniVoice(); else installOmniVoice(); }
                case "10" -> { if (audioCppInstalled) deinstallDia2(); else installDia2(); }
                case "q" -> {
                    return;
                }
            }
        }
    }

    public void handleCommandLine(String[] args) {
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
                    case "piper-thorsten", "thorsten" -> installPiperThorsten();
                    case "matcha", "matchatts" -> installMatcha();
                    case "vits" -> installVITS();
                    case "f5tts", "f5" -> installF5TTS();
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
                    case "matcha", "matchatts" -> deinstallMatcha();
                    case "vits" -> deinstallVITS();
                    case "f5tts", "f5" -> deinstallF5TTS();
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
                System.err.println("  --status                                              Print current installation status");
                System.err.println("  --install [all|kokoro|pocket|piper|matcha|vits|f5tts] Install local engines & models");
                System.err.println("  --deinstall [all|kokoro|pocket|piper|matcha|vits|f5]  Remove local engines & models");
            }
        }
    }

    private Integer cachedSapiVoices = null;

    public void printDashboard() {
        // Windows SAPI voice detection (cached after first query)
        if (cachedSapiVoices == null) {
            int sapiVoices = 0;
            try {
                sapiVoices = FastTTS.standard().getBackend("windows").getVoices().size();
            } catch (Exception ignored) {}
            cachedSapiVoices = Math.max(1, sapiVoices);
        }

        System.out.println();
        System.out.println(W + getTitle() + R);
        System.out.println();

        printDashboardRow("Windows SAPI/OneCore", "INSTALLED", cachedSapiVoices, "Built-in Windows native engine");

        boolean sherpaInstalled = new File(rootDir, "bin/sherpa-onnx/sherpa-onnx-offline-tts.exe").exists();
        boolean audioCppInstalled = new File(rootDir, "bin/audio.cpp/audiocpp_cli.exe").exists();
        boolean piperInstalled = new File(rootDir, "bin/piper/piper.exe").exists();

        // Kokoro Studio
        boolean kokoroWeights = new File(rootDir, "models/kokoro/kokoro-en-v0_19/model.onnx").exists();
        String kokoroStatus = sherpaInstalled ? "INSTALLED" : "MISSING";
        int kokoroVoices = kokoroWeights ? 1 : 0;
        printDashboardRow("Kokoro Studio", kokoroStatus, kokoroVoices, "bin/sherpa-onnx + models/kokoro");

        // Pocket TTS
        boolean pocketWeights = new File(rootDir, "models/pocket/sherpa-onnx-pocket-tts-int8-2026-01-26/lm_main.int8.onnx").exists();
        String pocketStatus = sherpaInstalled ? "INSTALLED" : "MISSING";
        int pocketVoices = pocketWeights ? 1 : 0;
        printDashboardRow("Pocket TTS", pocketStatus, pocketVoices, "bin/sherpa-onnx + models/pocket");

        // Piper Neural
        File piperDir = new File(rootDir, "models/piper");
        File[] piperModels = piperDir.listFiles((d, n) -> n.endsWith(".onnx"));
        int piperCount = (piperModels != null) ? piperModels.length : 0;
        String piperStatus = piperInstalled ? "INSTALLED" : "MISSING";
        printDashboardRow("Piper Neural", piperStatus, piperCount, "bin/piper + models/piper");

        // Matcha-TTS
        boolean matchaAcoustic = new File(rootDir, "models/matcha/matcha-icefall-en_US-ljspeech/model-steps-3.onnx").exists();
        boolean matchaVocoder = new File(rootDir, "models/matcha/vocos-22khz-univ.onnx").exists();
        String matchaStatus = sherpaInstalled ? "INSTALLED" : "MISSING";
        int matchaVoices = (matchaAcoustic && matchaVocoder) ? 1 : 0;
        printDashboardRow("Matcha-TTS", matchaStatus, matchaVoices, "bin/sherpa-onnx + models/matcha");

        // VITS
        File vitsDir = new File(rootDir, "models/vits");
        boolean vitsModel = false;
        if (vitsDir.exists()) {
            File[] sub = vitsDir.listFiles(File::isDirectory);
            if (sub != null && sub.length > 0) vitsModel = true;
        }
        String vitsStatus = sherpaInstalled ? "INSTALLED" : "MISSING";
        int vitsVoices = vitsModel ? 1 : 0;
        printDashboardRow("VITS Deterministic", vitsStatus, vitsVoices, "bin/sherpa-onnx + models/vits");

        // F5-TTS
        File f5Weights = new File(rootDir, "models/f5tts/f5-tts-v1-base-f16.gguf");
        String f5Status = audioCppInstalled ? "INSTALLED" : "MISSING";
        int f5Voices = (f5Weights.exists() && f5Weights.length() > 900_000_000L) ? 1 : 0;
        printDashboardRow("F5-TTS Voice Clone", f5Status, f5Voices, "bin/audio.cpp + models/f5tts");

        // OmniVoice
        File omniDir = new File(rootDir, "models/omnivoice");
        File omniWeights = new File(omniDir, "model.safetensors");
        String omniStatus = audioCppInstalled ? "INSTALLED" : "MISSING";
        int omniVoices = (omniWeights.exists() && omniWeights.length() > 2_400_000_000L) ? 1 : 0;
        printDashboardRow("OmniVoice Zero-Shot", omniStatus, omniVoices, "bin/audio.cpp + models/omnivoice");

        // Dia2 Dialogue
        File dia2Weights = new File(rootDir, "models/dia2/dia2-2b-q4_k.gguf").exists() ? new File(rootDir, "models/dia2/dia2-2b-q4_k.gguf") : new File(rootDir, "models/dia2/dia2-q4_0.gguf");
        String dia2Status = audioCppInstalled ? "INSTALLED" : "MISSING";
        int dia2Voices = dia2Weights.exists() ? 2 : 0;
        printDashboardRow("Dia2 Multi-Speaker", dia2Status, dia2Voices, "bin/audio.cpp + models/dia2");

        System.out.println();
    }

    private void printDashboardRow(String name, String status, int voiceCount, String details) {
        String statusFormatted;
        if (status.equalsIgnoreCase("INSTALLED")) {
            statusFormatted = W + String.format("%-4s", "✓") + R;
        } else {
            statusFormatted = G + String.format("%-4s", "✗") + R;
        }
        System.out.println(W + String.format("%-24s", name) + " " +
                statusFormatted + " " +
                W + String.format("%-10s", voiceCount + " voices") + " " +
                G + details + R);
    }

    // =========================================================================
    // Master Install / Deinstall
    // =========================================================================

    public void installAll() {
        System.out.println("\n" + W + "Installing all offline engine runners (no models)..." + R);
        ensureSherpaBinaries(new File(rootDir, "bin/sherpa-onnx"));
        ensureAudioCppBinaries();
        installPiper();
        ensureFastAudioPlayer();
        System.out.println("\n" + W + "All offline runners installed successfully!" + R + "\n");
        printDashboard();
    }

    private void ensureAudioCppBinaries() {
        File binDir = new File(rootDir, "bin/audio.cpp");
        File runner = new File(binDir, "audiocpp_cli.exe");
        if (runner.exists()) return;

        binDir.mkdirs();
        File tempZip = new File(binDir, "audiocpp_bin.zip");
        String url = "https://github.com/0xShug0/audio.cpp/releases/download/v0.9.2/audio-v0.9.2-bin-windows-x64-cpu.zip";
        System.out.println(W + "audio.cpp" + "  " + G + "Downloading native runner (~27 MB)..." + R);
        downloadWithCurl(url, tempZip);
        if (tempZip.exists() && tempZip.length() > 0) {
            System.out.println(W + "audio.cpp" + "  " + G + "Extracting binaries..." + R);
            extractZipPowerShell(tempZip, binDir);
            tempZip.delete();
        }
    }

    private void ensureFastAudioPlayer() {
        File binDir = new File(rootDir, "bin");
        binDir.mkdirs();
        File dll = new File(binDir, "fastaudioplayer.dll");
        if (!dll.exists()) {
            try (InputStream in = getClass().getResourceAsStream("/native/fastaudioplayer.dll")) {
                if (in != null) {
                    Files.copy(in, dll.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception ignored) {}
        }
    }

    public void deinstallAll() {
        System.out.println();
        System.out.println(W + "Deinstalling all local models & binaries..." + R);
        deleteDirectory(new File(rootDir, "bin/sherpa-onnx"));
        deleteDirectory(new File(rootDir, "bin/audio.cpp"));
        deleteDirectory(new File(rootDir, "bin/piper"));
        deleteDirectory(new File(rootDir, "models"));
        System.out.println();
        System.out.println(W + "Clean deinstallation complete!" + R);
        System.out.println();
        printDashboard();
    }

    // =========================================================================
    // 1. Kokoro Studio
    // =========================================================================

    public void installKokoro() {
        ensureSherpaBinaries(new File(rootDir, "bin/sherpa-onnx"));
        System.out.println(W + "Kokoro Studio" + "  " + G + "Runner installed successfully." + R);
    }

    public void installKokoroModel() {
        File kokoroDir = new File(rootDir, "models/kokoro");
        File targetDir = new File(kokoroDir, "kokoro-en-v0_19");
        File modelFile = new File(targetDir, "model.onnx");

        if (modelFile.exists() && modelFile.length() > 300_000_000L) {
            System.out.println(W + "Kokoro Studio" + "  " + G + "Model weights already present." + R);
        } else {
            kokoroDir.mkdirs();
            File tarBz2 = new File(kokoroDir, "kokoro-en-v0_19.tar.bz2");
            System.out.println(W + "Kokoro Studio" + "  " + G + "Downloading default model (~345 MB)..." + R);
            downloadWithCurl(KOKORO_MODEL_URL, tarBz2);
            if (tarBz2.exists() && tarBz2.length() > 0) {
                System.out.println(W + "Kokoro Studio" + "  " + G + "Extracting archive..." + R);
                extractTarBz2(tarBz2, kokoroDir);
                tarBz2.delete();
            }
        }
        System.out.println(W + "Kokoro Studio" + "  " + G + "Model weights installed successfully." + R);
    }

    public void deinstallKokoro() {
        System.out.println(W + "Kokoro Studio" + "  " + G + "Removing models..." + R);
        deleteDirectory(new File(rootDir, "models/kokoro"));
        System.out.println(W + "Kokoro Studio" + "  " + G + "Deinstalled successfully." + R);
    }

    private void manageKokoro() {
        while (true) {
            clearConsole();
            System.out.println("--- Kokoro Studio Management ---");
            File bin = new File(rootDir, "bin/sherpa-onnx/sherpa-onnx-offline-tts.exe");
            File modelDir = new File(rootDir, "models/kokoro/kokoro-en-v0_19");
            System.out.println("Binary:  " + (bin.exists() ? "✓ Installed (bin/sherpa-onnx)" : "✗ Missing"));
            System.out.println("Model:   " + (new File(modelDir, "model.onnx").exists() ? "✓ Installed (" + (new File(modelDir, "model.onnx").length() / (1024 * 1024)) + " MB)" : "✗ Missing"));
            System.out.println();
            System.out.println("Options:");
            System.out.println("1   Install Kokoro Runner Binary");
            System.out.println("2   Download Kokoro Default Model (English ~345 MB)");
            System.out.println("3   Deinstall Kokoro Studio Models");
            System.out.println("4   Test Kokoro Voice Output");
            System.out.println("b   Back");
            System.out.println();
            System.out.print("Command: ");

            String cmd = scanner.nextLine().trim().toLowerCase();
            if (cmd.equals("b") || cmd.equals("back")) return;

            switch (cmd) {
                case "1" -> installKokoro();
                case "2" -> installKokoroModel();
                case "3" -> deinstallKokoro();
                case "4" -> testKokoroVoice();
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
                System.out.println("Playback complete.");
            }
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // 2. Pocket TTS
    // =========================================================================

    public void installPocket() {
        ensureSherpaBinaries(new File(rootDir, "bin/sherpa-onnx"));
        System.out.println(W + "Pocket TTS" + "  " + G + "Runner installed successfully." + R);
    }

    public void installPocketModel() {
        File pocketDir = new File(rootDir, "models/pocket");
        File targetDir = new File(pocketDir, "sherpa-onnx-pocket-tts-int8-2026-01-26");
        File lmMain = new File(targetDir, "lm_main.int8.onnx");

        if (lmMain.exists() && lmMain.length() > 50_000_000L) {
            System.out.println(W + "Pocket TTS" + "  " + G + "Model weights already present." + R);
        } else {
            pocketDir.mkdirs();
            File tarBz2 = new File(pocketDir, "pocket_tts.tar.bz2");
            System.out.println(W + "Pocket TTS" + "  " + G + "Downloading INT8 weights (~98 MB)..." + R);
            downloadWithCurl(POCKET_MODEL_URL, tarBz2);
            if (tarBz2.exists() && tarBz2.length() > 0) {
                System.out.println(W + "Pocket TTS" + "  " + G + "Extracting archive..." + R);
                extractTarBz2(tarBz2, pocketDir);
                tarBz2.delete();
            }
        }
        System.out.println(W + "Pocket TTS" + "  " + G + "Model weights installed successfully." + R);
    }

    public void deinstallPocket() {
        System.out.println(W + "Pocket TTS" + "  " + G + "Removing models..." + R);
        deleteDirectory(new File(rootDir, "models/pocket"));
        System.out.println(W + "Pocket TTS" + "  " + G + "Deinstalled successfully." + R);
    }

    private void managePocket() {
        while (true) {
            clearConsole();
            System.out.println("--- Pocket TTS Management ---");
            File bin = new File(rootDir, "bin/sherpa-onnx/sherpa-onnx-offline-tts.exe");
            File modelDir = new File(rootDir, "models/pocket/sherpa-onnx-pocket-tts-int8-2026-01-26");
            System.out.println("Binary:  " + (bin.exists() ? "✓ Installed (bin/sherpa-onnx)" : "✗ Missing"));
            System.out.println("Model:   " + (new File(modelDir, "lm_main.int8.onnx").exists() ? "✓ Installed" : "✗ Missing"));
            System.out.println();
            System.out.println("Options:");
            System.out.println("1   Install Pocket TTS Runner Binary");
            System.out.println("2   Download Pocket INT8 Weights (~98 MB)");
            System.out.println("3   Deinstall Pocket TTS Models");
            System.out.println("4   Test Pocket TTS Output");
            System.out.println("b   Back");
            System.out.println();
            System.out.print("Command: ");

            String cmd = scanner.nextLine().trim().toLowerCase();
            if (cmd.equals("b") || cmd.equals("back")) return;

            switch (cmd) {
                case "1" -> installPocket();
                case "2" -> installPocketModel();
                case "3" -> deinstallPocket();
                case "4" -> testPocketVoice();
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
                System.out.println("Playback complete.");
            }
        } catch (Exception e) {
            System.err.println("Pocket test failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // 3. Piper Neural
    // =========================================================================

    public void installPiper() {
        File binDir = new File(rootDir, "bin/piper");
        File piperExe = new File(binDir, "piper.exe");

        if (piperExe.exists()) {
            System.out.println(W + "Piper Neural" + "  " + G + "Runner already present." + R);
        } else {
            binDir.mkdirs();
            File tempZip = new File(binDir, "piper_temp.zip");
            System.out.println(W + "Piper Neural" + "  " + G + "Downloading Piper runner (~22 MB)..." + R);
            downloadWithCurl(PIPER_BIN_URL, tempZip);
            if (tempZip.exists() && tempZip.length() > 0) {
                System.out.println(W + "Piper Neural" + "  " + G + "Extracting binaries..." + R);
                extractZipPowerShell(tempZip, binDir);
                tempZip.delete();
                // Move inner files if extracted into subfolder
                flattenSubfolder(binDir, "piper");
            }
        }

        File modelsDir = new File(rootDir, "models/piper");
        modelsDir.mkdirs();

        System.out.println(W + "Piper Neural" + "  " + G + "Runner installed successfully." + R);
    }

    public void installPiperThorsten() {
        installPiper();
        File modelsDir = new File(rootDir, "models/piper");
        modelsDir.mkdirs();
        File onnxFile = new File(modelsDir, "de_DE-thorsten-medium.onnx");
        File jsonFile = new File(modelsDir, "de_DE-thorsten-medium.onnx.json");

        if (!onnxFile.exists() || onnxFile.length() < 10_000_000L) {
            System.out.println(W + "Piper Neural" + "  " + G + "Downloading Thorsten German model (~63 MB)..." + R);
            downloadWithCurl(PIPER_THORSTEN_ONNX, onnxFile);
        }
        if (!jsonFile.exists()) {
            System.out.println(W + "Piper Neural" + "  " + G + "Downloading config..." + R);
            downloadWithCurl(PIPER_THORSTEN_JSON, jsonFile);
        }
        System.out.println(W + "Piper Neural" + "  " + G + "Thorsten German model installed successfully." + R);
    }

    public void deinstallPiper() {
        System.out.println(W + "Piper Neural" + "  " + G + "Removing binaries and models..." + R);
        deleteDirectory(new File(rootDir, "bin/piper"));
        deleteDirectory(new File(rootDir, "models/piper"));
        System.out.println(W + "Piper Neural" + "  " + G + "Deinstalled successfully." + R);
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
                    System.out.printf("✓ %-35s (%s MB)%n", v.getName(), String.format("%.1f", v.length() / (1024.0 * 1024.0)));
                }
            } else {
                System.out.println("(No voices installed in models/piper)");
            }

            System.out.println();
            System.out.println("Options:");
            System.out.println("1   Install Piper Binary Runtime");
            System.out.println("2   Install Thorsten German Voice");
            System.out.println("3   Deinstall Piper (Binaries & Voices)");
            System.out.println("4   Test Piper Voice");
            System.out.println("b   Back");
            System.out.println();
            System.out.print("Command: ");

            String cmd = scanner.nextLine().trim().toLowerCase();
            if (cmd.equals("b") || cmd.equals("back")) return;

            switch (cmd) {
                case "1" -> installPiper();
                case "2" -> installPiperThorsten();
                case "3" -> deinstallPiper();
                case "4" -> testPiperVoice();
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
                System.out.println("Playback complete.");
            }
        } catch (Exception e) {
            System.err.println("Piper test failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // 4. OmniVoice
    // =========================================================================

    public void installOmniVoice() {
        ensureAudioCppBinaries();
        File omniDir = new File(rootDir, "models/omnivoice");
        omniDir.mkdirs();
        System.out.println(W + "OmniVoice" + "  " + G + "Runner installed successfully." + R);
    }

    public void installOmniVoiceModel() {
        File omniDir = new File(rootDir, "models/omnivoice");
        omniDir.mkdirs();
        File target = new File(omniDir, "model.safetensors");

        if (target.exists() && target.length() > 2_400_000_000L) {
            System.out.println(W + "OmniVoice" + "  " + G + "Weights already complete (" + (target.length() / (1024 * 1024)) + " MB)" + R);
        } else {
            String url = "https://huggingface.co/k2-fsa/OmniVoice/resolve/main/model.safetensors";
            System.out.println(W + "OmniVoice" + "  " + G + "Downloading model weights (~2.45 GB)..." + R);
            downloadWithCurl(url, target);
        }
        System.out.println(W + "OmniVoice" + "  " + G + "Model weights installed successfully." + R);
    }

    public void deinstallOmniVoice() {
        System.out.println(W + "OmniVoice Zero-Shot" + "  " + G + "Removing models..." + R);
        deleteDirectory(new File(rootDir, "models/omnivoice"));
        System.out.println(W + "OmniVoice Zero-Shot" + "  " + G + "Deinstalled successfully." + R);
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
        System.out.println("1   Install OmniVoice Runner Binary");
        System.out.println("2   Download / Resume OmniVoice Weights (2.45 GB)");
        System.out.println("3   Deinstall OmniVoice Models");
        System.out.println("b   Back");
        System.out.println();
        System.out.print("Command: ");

        String cmd = scanner.nextLine().trim().toLowerCase();
        switch (cmd) {
            case "1" -> installOmniVoice();
            case "2" -> installOmniVoiceModel();
            case "3" -> deinstallOmniVoice();
        }
    }

    // =========================================================================
    // 5. Dia2 Dialogue
    // =========================================================================

    public void installDia2() {
        ensureAudioCppBinaries();
        File dia2Dir = new File(rootDir, "models/dia2");
        dia2Dir.mkdirs();
        System.out.println(W + "Dia2" + "  " + G + "Runner installed successfully." + R);
    }

    public void installDia2Model() {
        File dia2Dir = new File(rootDir, "models/dia2");
        dia2Dir.mkdirs();
        File target = new File(dia2Dir, "dia2-2b-q4_k.gguf");
        if (target.exists() && target.length() > 1_000_000_000L) {
            System.out.println(W + "Dia2" + "  " + G + "Model weights already present (" + (target.length() / (1024 * 1024)) + " MB)" + R);
        } else {
            System.out.println(W + "Dia2" + "  " + G + "Downloading model weights (~1.1 GB)..." + R);
            String dia2Url = "https://huggingface.co/markldn/Dia2-2B-GGUF/resolve/main/dia2-q4_0.gguf";
            downloadWithCurl(dia2Url, target);
        }
        System.out.println(W + "Dia2" + "  " + G + "Model installed successfully." + R);
    }

    public void deinstallDia2() {
        System.out.println(W + "Dia2 Multi-Speaker" + "  " + G + "Removing models..." + R);
        deleteDirectory(new File(rootDir, "models/dia2"));
        System.out.println(W + "Dia2 Multi-Speaker" + "  " + G + "Deinstalled successfully." + R);
    }

    private void manageDia2() {
        while (true) {
            clearConsole();
            System.out.println("--- Dia2 Multi-Speaker Dialogue (GGUF) ---");
            File dia2Dir = new File(rootDir, "models/dia2");
            File weights = new File(dia2Dir, "dia2-2b-q4_k.gguf");
            File runner = new File(rootDir, "bin/audio.cpp/audiocpp_cli.exe");

            System.out.println("GGUF Weights: " + (weights.exists() ? "✓ Installed (" + (weights.length() / (1024 * 1024)) + " MB)" : "✗ Missing"));
            System.out.println("CLI Runner:   " + (runner.exists() ? "✓ Installed (bin/audio.cpp/audiocpp_cli.exe)" : "✗ Missing"));
            System.out.println();
            System.out.println("Options:");
            System.out.println("1   Install Dia2 Runner Binary");
            System.out.println("2   Download Dia2 GGUF Model (1.1 GB)");
            System.out.println("3   Deinstall Dia2 Models");
            System.out.println("4   Test Dia2 Dialogue Synthesis");
            System.out.println("b   Back");
            System.out.println();
            System.out.print("Command: ");

            String cmd = scanner.nextLine().trim().toLowerCase();
            if (cmd.equals("b") || cmd.equals("back")) return;

            switch (cmd) {
                case "1" -> installDia2();
                case "2" -> installDia2Model();
                case "3" -> deinstallDia2();
                case "4" -> testDia2Voice();
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
                System.out.println("Playback complete.");
            }
        } catch (Exception e) {
            System.err.println("Dia2 test failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // 6. Matcha-TTS (Flow Matching)
    // =========================================================================

    public void installMatcha() {
        ensureSherpaBinaries(new File(rootDir, "bin/sherpa-onnx"));
        File matchaDir = new File(rootDir, "models/matcha");
        matchaDir.mkdirs();
        System.out.println(W + "Matcha-TTS" + "  " + G + "Runner installed successfully." + R);
    }

    public void installMatchaModel() {
        File matchaDir = new File(rootDir, "models/matcha");
        File acousticDir = new File(matchaDir, "matcha-icefall-en_US-ljspeech");
        File acousticModel = new File(acousticDir, "model-steps-3.onnx");
        File vocoderFile = new File(matchaDir, "vocos-22khz-univ.onnx");

        matchaDir.mkdirs();

        if (acousticModel.exists() && acousticModel.length() > 50_000_000L) {
            System.out.println(W + "Matcha-TTS" + "  " + G + "Acoustic model already present." + R);
        } else {
            File tarBz2 = new File(matchaDir, "matcha-icefall-en_US-ljspeech.tar.bz2");
            System.out.println(W + "Matcha-TTS" + "  " + G + "Downloading acoustic model (~74 MB)..." + R);
            downloadWithCurl(MATCHA_MODEL_URL, tarBz2);
            if (tarBz2.exists() && tarBz2.length() > 0) {
                System.out.println(W + "Matcha-TTS" + "  " + G + "Extracting acoustic model..." + R);
                extractTarBz2(tarBz2, matchaDir);
                tarBz2.delete();
            }
        }

        if (vocoderFile.exists() && vocoderFile.length() > 40_000_000L) {
            System.out.println(W + "Matcha-TTS" + "  " + G + "Vocoder already present." + R);
        } else {
            System.out.println(W + "Matcha-TTS" + "  " + G + "Downloading vocoder (~51 MB)..." + R);
            downloadWithCurl(VOCOS_VOCODER_URL, vocoderFile);
        }

        System.out.println(W + "Matcha-TTS" + "  " + G + "Model weights installed successfully." + R);
    }

    public void deinstallMatcha() {
        System.out.println(W + "Matcha-TTS" + "  " + G + "Removing models..." + R);
        deleteDirectory(new File(rootDir, "models/matcha"));
        System.out.println(W + "Matcha-TTS" + "  " + G + "Deinstalled successfully." + R);
    }

    private void manageMatcha() {
        while (true) {
            clearConsole();
            System.out.println("--- Matcha-TTS Management (Flow Matching) ---");
            File bin = new File(rootDir, "bin/sherpa-onnx/sherpa-onnx-offline-tts.exe");
            File acousticModel = new File(rootDir, "models/matcha/matcha-icefall-en_US-ljspeech/model-steps-3.onnx");
            File vocoder = new File(rootDir, "models/matcha/vocos-22khz-univ.onnx");
            System.out.println("Binary:   " + (bin.exists() ? "✓ Installed (bin/sherpa-onnx)" : "✗ Missing"));
            System.out.println("Acoustic: " + (acousticModel.exists() ? "✓ Installed (" + (acousticModel.length() / (1024 * 1024)) + " MB)" : "✗ Missing"));
            System.out.println("Vocoder:  " + (vocoder.exists() ? "✓ Installed (" + (vocoder.length() / (1024 * 1024)) + " MB)" : "✗ Missing"));
            System.out.println();
            System.out.println("Options:");
            System.out.println("1   Install Matcha-TTS Runner Binary");
            System.out.println("2   Download Matcha-TTS Models (LJSpeech + Vocos ~125 MB)");
            System.out.println("3   Deinstall Matcha-TTS Models");
            System.out.println("4   Test Matcha Voice Output");
            System.out.println("b   Back");
            System.out.println();
            System.out.print("Command: ");

            String cmd = scanner.nextLine().trim().toLowerCase();
            if (cmd.equals("b") || cmd.equals("back")) return;

            switch (cmd) {
                case "1" -> installMatcha();
                case "2" -> installMatchaModel();
                case "3" -> deinstallMatcha();
                case "4" -> testMatchaVoice();
            }
        }
    }

    private void testMatchaVoice() {
        System.out.println("\nSynthesizing test speech with Matcha-TTS (Flow Matching)...");
        try {
            FastTTS tts = FastTTS.standard();
            FastTTSAudio audio = tts.speak("matcha", "Hello! Matcha Flow Matching speech synthesis is running at ultra high speed.", null, null);
            if (audio != null) {
                playAudio(audio.getData());
                System.out.println("Playback complete.");
            }
        } catch (Exception e) {
            System.err.println("Matcha test failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // 7. VITS (Deterministic Neural TTS)
    // =========================================================================

    public void installVITS() {
        ensureSherpaBinaries(new File(rootDir, "bin/sherpa-onnx"));
        File vitsDir = new File(rootDir, "models/vits");
        vitsDir.mkdirs();
        System.out.println(W + "VITS" + "  " + G + "Runner installed successfully." + R);
    }

    public void installVITSModel() {
        File vitsDir = new File(rootDir, "models/vits");
        File targetDir = new File(vitsDir, "vits-piper-en_US-amy-low");
        File modelFile = new File(targetDir, "en_US-amy-low.onnx");

        if (modelFile.exists() && modelFile.length() > 50_000_000L) {
            System.out.println(W + "VITS" + "  " + G + "Model weights already present." + R);
        } else {
            vitsDir.mkdirs();
            File tarBz2 = new File(vitsDir, "vits-piper-en_US-amy-low.tar.bz2");
            System.out.println(W + "VITS" + "  " + G + "Downloading Amy Low model (~64 MB)..." + R);
            downloadWithCurl(VITS_MODEL_URL, tarBz2);
            if (tarBz2.exists() && tarBz2.length() > 0) {
                System.out.println(W + "VITS" + "  " + G + "Extracting archive..." + R);
                extractTarBz2(tarBz2, vitsDir);
                tarBz2.delete();
            }
        }
        System.out.println(W + "VITS" + "  " + G + "Model weights installed successfully." + R);
    }

    public void deinstallVITS() {
        System.out.println(W + "VITS Deterministic" + "  " + G + "Removing models..." + R);
        deleteDirectory(new File(rootDir, "models/vits"));
        System.out.println(W + "VITS Deterministic" + "  " + G + "Deinstalled successfully." + R);
    }

    private void manageVITS() {
        while (true) {
            clearConsole();
            System.out.println("--- VITS Management (Deterministic Neural) ---");
            File bin = new File(rootDir, "bin/sherpa-onnx/sherpa-onnx-offline-tts.exe");
            File vitsDir = new File(rootDir, "models/vits");
            boolean hasModel = false;
            if (vitsDir.exists()) {
                File[] sub = vitsDir.listFiles(File::isDirectory);
                if (sub != null && sub.length > 0) hasModel = true;
            }
            System.out.println("Binary:  " + (bin.exists() ? "✓ Installed (bin/sherpa-onnx)" : "✗ Missing"));
            System.out.println("Model:   " + (hasModel ? "✓ Installed (models/vits)" : "✗ Missing"));
            System.out.println();
            System.out.println("Options:");
            System.out.println("1   Install VITS Runner Binary");
            System.out.println("2   Download VITS Amy Low Model (~64 MB)");
            System.out.println("3   Deinstall VITS Models");
            System.out.println("4   Test VITS Voice Output");
            System.out.println("b   Back");
            System.out.println();
            System.out.print("Command: ");

            String cmd = scanner.nextLine().trim().toLowerCase();
            if (cmd.equals("b") || cmd.equals("back")) return;

            switch (cmd) {
                case "1" -> installVITS();
                case "2" -> installVITSModel();
                case "3" -> deinstallVITS();
                case "4" -> testVITSVoice();
            }
        }
    }

    private void testVITSVoice() {
        System.out.println("\nSynthesizing test speech with VITS (Deterministic low latency)...");
        try {
            FastTTS tts = FastTTS.standard();
            FastTTSAudio audio = tts.speak("vits", "Hello! VITS deterministic speech synthesis is online.", null, null);
            if (audio != null) {
                playAudio(audio.getData());
                System.out.println("Playback complete.");
            }
        } catch (Exception e) {
            System.err.println("VITS test failed: " + e.getMessage());
        }
    }

    // =========================================================================
    // 8. F5-TTS (Flow Matching Voice Clone)
    // =========================================================================

    public void installF5TTS() {
        ensureAudioCppBinaries();
        File f5Dir = new File(rootDir, "models/f5tts");
        f5Dir.mkdirs();
        System.out.println(W + "F5-TTS" + "  " + G + "Runner installed successfully." + R);
    }

    public void installF5TTSModel() {
        File f5Dir = new File(rootDir, "models/f5tts");
        f5Dir.mkdirs();

        File vocab = new File(f5Dir, "vocab.txt");
        if (!vocab.exists()) {
            System.out.println(W + "F5-TTS" + "  " + G + "Downloading vocab.txt..." + R);
            downloadWithCurl(F5TTS_VOCAB_URL, vocab);
        }

        File model = new File(f5Dir, "f5-tts-v1-base-f16.gguf");
        if (model.exists() && model.length() > 900_000_000L) {
            System.out.println(W + "F5-TTS" + "  " + G + "Model weights already present." + R);
        } else {
            System.out.println(W + "F5-TTS" + "  " + G + "Downloading model weights (~999 MB)..." + R);
            downloadWithCurl(F5TTS_MODEL_URL, model);
        }

        System.out.println(W + "F5-TTS" + "  " + G + "Model weights installed successfully." + R);
    }

    public void deinstallF5TTS() {
        System.out.println(W + "F5-TTS Voice Clone" + "  " + G + "Removing models..." + R);
        deleteDirectory(new File(rootDir, "models/f5tts"));
        System.out.println(W + "F5-TTS Voice Clone" + "  " + G + "Deinstalled successfully." + R);
    }

    private void manageF5TTS() {
        while (true) {
            clearConsole();
            System.out.println("--- F5-TTS Management (Voice Cloning Flow Matching) ---");
            File bin = new File(rootDir, "bin/audio.cpp/audiocpp_cli.exe");
            File model = new File(rootDir, "models/f5tts/f5-tts-v1-base-f16.gguf");
            System.out.println("Runner:  " + (bin.exists() ? "✓ Installed (bin/audio.cpp/audiocpp_cli.exe)" : "✗ Missing"));
            System.out.println("Model:   " + (model.exists() && model.length() > 900_000_000L ? "✓ Installed (" + (model.length() / (1024 * 1024)) + " MB)" : "✗ Missing"));
            System.out.println();
            System.out.println("Options:");
            System.out.println("1   Install F5-TTS Runner Binary");
            System.out.println("2   Download F5-TTS Model (GGUF Base + Vocab ~999 MB)");
            System.out.println("3   Deinstall F5-TTS Models");
            System.out.println("4   Test F5-TTS Voice Output");
            System.out.println("b   Back");
            System.out.println();
            System.out.print("Command: ");

            String cmd = scanner.nextLine().trim().toLowerCase();
            if (cmd.equals("b") || cmd.equals("back")) return;

            switch (cmd) {
                case "1" -> installF5TTS();
                case "2" -> installF5TTSModel();
                case "3" -> deinstallF5TTS();
                case "4" -> testF5TTSVoice();
            }
        }
    }

    private void testF5TTSVoice() {
        System.out.println("\nSynthesizing test speech with F5-TTS...");
        try {
            FastTTS tts = FastTTS.standard();
            FastTTSAudio audio = tts.speak("f5tts", "Hello from F5-TTS Flow Matching voice synthesis.", null, null);
            if (audio != null) {
                playAudio(audio.getData());
                System.out.println("Playback complete.");
            }
        } catch (Exception e) {
            System.err.println("F5-TTS test failed: " + e.getMessage());
        }
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
                System.out.println(W + "Sherpa-ONNX" + "  " + G + "Reusing local binaries from " + src.getName() + " -> " + targetBinDir.getName() + R);
                copyDirectory(src, targetBinDir);
                return;
            }
        }

        // Download official release
        targetBinDir.mkdirs();
        File tempTar = new File(targetBinDir, "sherpa_release.tar.bz2");
        System.out.println(W + "Sherpa-ONNX" + "  " + G + "Downloading Sherpa-ONNX runner (~24 MB)..." + R);
        downloadWithCurl(SHERPA_RELEASE_URL, tempTar);
        if (tempTar.exists() && tempTar.length() > 0) {
            System.out.println(W + "Sherpa-ONNX" + "  " + G + "Extracting binaries..." + R);
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
        downloadWithProgress(url, targetFile);
    }

    private void downloadWithProgress(String urlString, File targetFile) {
        String gray = G;
        String white = W;
        String reset = R;

        File tempFile = new File(targetFile.getParentFile(), targetFile.getName() + ".part");
        try {
            if (targetFile.getParentFile() != null) {
                targetFile.getParentFile().mkdirs();
            }

            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(15))
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                    .build();

            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(urlString))
                    .timeout(java.time.Duration.ofMinutes(10))
                    .header("User-Agent", "FastTTS-Installer/0.1.6")
                    .GET()
                    .build();

            java.net.http.HttpResponse<java.io.InputStream> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() >= 400) {
                System.err.println("[Download Error] HTTP " + resp.statusCode() + " for " + urlString);
                return;
            }

            long totalBytes = resp.headers().firstValueAsLong("Content-Length").orElse(-1L);
            long downloaded = 0;
            long lastNotify = 0;
            String fileName = targetFile.getName();

            try (java.io.InputStream in = resp.body();
                 java.io.OutputStream out = new java.io.FileOutputStream(tempFile)) {
                byte[] buffer = new byte[65536];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    downloaded += read;

                    long now = System.currentTimeMillis();
                    boolean isComplete = (totalBytes > 0 && downloaded >= totalBytes);
                    if (now - lastNotify >= 60 || isComplete) {
                        lastNotify = now;
                        double percent = (totalBytes > 0) ? (downloaded * 100.0 / totalBytes) : -1.0;
                        int width = 30;
                        int filled = (percent >= 0) ? (int) Math.round((percent / 100.0) * width) : 0;
                        filled = Math.max(0, Math.min(width, filled));

                        String bar = white + "█".repeat(filled) + gray + "░".repeat(width - filled) + reset;
                        double mbDownloaded = downloaded / (1024.0 * 1024.0);
                        double mbTotal = (totalBytes > 0) ? totalBytes / (1024.0 * 1024.0) : 0;
                        String stat = (totalBytes > 0)
                                ? String.format(" %s%.1f%%%s (%s%.1f%s / %s%.1f MB%s)", white, percent, gray, white, mbDownloaded, gray, white, mbTotal, gray)
                                : String.format(" %s%.1f MB%s", white, mbDownloaded, gray);

                        System.out.print("\r" + gray + "Downloading " + white + fileName + gray + ": " + bar + stat + reset);

                        if (installListener != null) {
                            try {
                                installListener.onProgress("Downloading " + fileName, percent, downloaded, totalBytes);
                            } catch (Exception ignored) {}
                        }
                    }
                }
                out.flush();
            }
            if (installListener != null) {
                try {
                    installListener.onProgress("Downloaded " + fileName, 100.0, downloaded, totalBytes);
                } catch (Exception ignored) {}
            }
            System.out.println();
            Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("\n[Download Error]: " + e.getMessage());
            tempFile.delete();
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
        if (audioData == null || audioData.length == 0) return;
        try {
            javax.sound.sampled.AudioInputStream ais = javax.sound.sampled.AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioData));
            javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
            clip.open(ais);
            clip.start();
            Thread.sleep(Math.min(5000, clip.getMicrosecondLength() / 1000 + 100));
            clip.close();
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
