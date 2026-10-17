package fasttts.installer;

import fastaudio.FastAudioPlayer;
import fasttts.FastTTS;
import fasttts.core.FastTTSAudio;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * FastTTS Local Engine & Model Installer.
 * Manages completely self-contained local installations of all TTS backends,
 * models, and native binaries under ./bin and ./models.
 */
public class FastTTSInstaller {

    private final Scanner scanner = new Scanner(System.in);
    private final File rootDir;

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
            System.out.println("  1.  [Kokoro]    Install / Update Kokoro Studio & Sherpa-ONNX");
            System.out.println("  2.  [Piper]     Install / Update Piper Neural & Voices");
            System.out.println("  3.  [OmniVoice] Manage OmniVoice Zero-Shot Cloning");
            System.out.println("  4.  [Dia2]      Manage Dia2 Scripted Dialogue (GGUF)");
            System.out.println("  5.  [Audio]     Test Native Audio Playback (WASAPI)");
            System.out.println("  6.  [Benchmark] Run Multi-Engine Benchmark Suite");
            System.out.println("  q.  Quit");
            System.out.println();
            System.out.print("Choose an option: ");

            String choice = scanner.nextLine().trim().toLowerCase();
            switch (choice) {
                case "1" -> manageKokoro();
                case "2" -> managePiper();
                case "3" -> manageOmniVoice();
                case "4" -> manageDia2();
                case "5" -> testAudio();
                case "6" -> runBenchmark();
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
            case "--download", "-d" -> {
                if (args.length < 2) {
                    System.err.println("Usage: run-installer --download <kokoro|piper|omnivoice|dia2>");
                    return;
                }
                String target = args[1].toLowerCase();
                switch (target) {
                    case "kokoro" -> installKokoro();
                    case "piper" -> {
                        installPiperBinaries();
                        downloadPiperVoice("de_DE-thorsten-medium");
                    }
                    case "omnivoice" -> downloadOmniVoiceWeights();
                    case "dia2" -> downloadDia2Weights();
                    default -> System.err.println("Unknown target: " + target);
                }
            }
            case "--all" -> {
                System.out.println("Installing all offline engines locally...");
                installKokoro();
                installPiperBinaries();
                downloadPiperVoice("de_DE-thorsten-medium");
                downloadPiperVoice("en_US-lessac-medium");
            }
            default -> System.err.println("Unknown command: " + cmd);
        }
    }

    private void printDashboard() {
        System.out.println("================================================================================");
        System.out.println("                       FastTTS Engine & Model Manager");
        System.out.println("================================================================================");
        System.out.println("Root Directory: " + rootDir.getAbsolutePath());
        System.out.println("Local Storage:  All models & binaries strictly contained in ./models and ./bin");
        System.out.println("--------------------------------------------------------------------------------");

        // Windows SAPI
        System.out.printf("  %-22s | %-12s | %s%n", "Windows SAPI/OneCore", "[INSTALLED]", "Built-in Windows native engine");

        // Kokoro Studio
        boolean kokoroBin = new File(rootDir, "bin/kokoro/sherpa-onnx-offline-tts.exe").exists()
                || new File(rootDir, "bin/sherpa-onnx/sherpa-onnx-offline-tts.exe").exists();
        boolean kokoroWeights = new File(rootDir, "models/kokoro/kokoro-en-v0_19/model.onnx").exists()
                || new File(rootDir, "models/kokoro/model.onnx").exists();
        String kokoroStatus = (kokoroBin && kokoroWeights) ? "[INSTALLED]" : (kokoroBin ? "[NO WEIGHTS]" : "[MISSING]");
        System.out.printf("  %-22s | %-12s | bin/kokoro + models/kokoro%n", "Kokoro Studio (ONNX)", kokoroStatus);

        // Piper Neural
        boolean piperBin = new File(rootDir, "bin/piper/piper.exe").exists();
        File piperDir = new File(rootDir, "models/piper");
        File[] piperModels = piperDir.listFiles((d, n) -> n.endsWith(".onnx"));
        if (piperModels == null || piperModels.length == 0) {
            piperModels = new File(rootDir, "models").listFiles((d, n) -> n.endsWith(".onnx") && !n.equals("model.onnx"));
        }
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
    // 1. Kokoro Studio
    // =========================================================================

    private void manageKokoro() {
        while (true) {
            clearConsole();
            System.out.println("--- Kokoro Studio Management ---");
            File bin = new File(rootDir, "bin/sherpa-onnx/sherpa-onnx-offline-tts.exe");
            File modelDir = new File(rootDir, "models/kokoro");
            System.out.println("Binary:  " + (bin.exists() ? "✓ Installed" : "✗ Missing (bin/sherpa-onnx/sherpa-onnx-offline-tts.exe)"));
            System.out.println("Model:   " + (new File(modelDir, "model.onnx").exists() ? "✓ Installed (models/kokoro/model.onnx)" : "✗ Missing"));
            System.out.println("Voices:  " + (new File(modelDir, "voices.bin").exists() ? "✓ Installed (models/kokoro/voices.bin)" : "✗ Missing"));
            System.out.println("Tokens:  " + (new File(modelDir, "tokens.txt").exists() ? "✓ Installed (models/kokoro/tokens.txt)" : "✗ Missing"));
            System.out.println();
            System.out.println("Options:");
            System.out.println("  1. Download & Install Kokoro Weights (model.onnx, voices.bin, tokens.txt)");
            System.out.println("  2. Download Sherpa-ONNX Windows x64 Runner");
            System.out.println("  3. Test Kokoro Voice Output");
            System.out.println("  b. Back");
            System.out.println();
            System.out.print("Command: ");

            String cmd = scanner.nextLine().trim().toLowerCase();
            if (cmd.equals("b") || cmd.equals("back")) return;

            switch (cmd) {
                case "1" -> installKokoroWeights();
                case "2" -> installSherpaOnnxBinary();
                case "3" -> testKokoroVoice();
            }
        }
    }

    private void installKokoro() {
        installSherpaOnnxBinary();
        installKokoroWeights();
    }

    private void installSherpaOnnxBinary() {
        System.out.println("\n[Kokoro] Installing Sherpa-ONNX Runner...");
        File binDir = new File(rootDir, "bin/sherpa-onnx");
        binDir.mkdirs();

        String url = "https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.10.45/sherpa-onnx-v1.10.45-win-x64.zip";
        File tempZip = new File(binDir, "sherpa_temp.zip");
        downloadFileWithProgress(url, tempZip);

        if (tempZip.exists() && tempZip.length() > 0) {
            System.out.println("[Kokoro] Extracting sherpa-onnx binaries...");
            extractZipPowerShell(tempZip, binDir);
            tempZip.delete();
            System.out.println("[SUCCESS] Sherpa-ONNX runner installed locally in " + binDir.getAbsolutePath());
        }
    }

    private void installKokoroWeights() {
        System.out.println("\n[Kokoro] Installing Kokoro Model Weights...");
        File modelDir = new File(rootDir, "models/kokoro");
        modelDir.mkdirs();

        String baseHf = "https://huggingface.co/hexgrad/Kokoro-82M/resolve/main/";
        downloadFileWithProgress(baseHf + "kokoro-v0_19.onnx", new File(modelDir, "model.onnx"));
        downloadFileWithProgress(baseHf + "voices.bin", new File(modelDir, "voices.bin"));
        downloadFileWithProgress(baseHf + "tokens.txt", new File(modelDir, "tokens.txt"));
        System.out.println("[SUCCESS] Kokoro model weights installed locally in " + modelDir.getAbsolutePath());
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
        pause();
    }

    // =========================================================================
    // 2. Piper Neural
    // =========================================================================

    private void managePiper() {
        while (true) {
            clearConsole();
            System.out.println("--- Piper Neural Management ---");
            File bin = new File(rootDir, "bin/piper/piper.exe");
            File modelsDir = new File(rootDir, "models/piper");
            File[] voices = modelsDir.listFiles((d, n) -> n.endsWith(".onnx"));
            if (voices == null || voices.length == 0) {
                voices = new File(rootDir, "models").listFiles((d, n) -> n.endsWith(".onnx") && !n.equals("model.onnx"));
            }

            System.out.println("Piper Binary: " + (bin.exists() ? "✓ Installed (bin/piper/piper.exe)" : "✗ Missing"));
            System.out.println("Installed Voices (models/piper):");
            if (voices != null && voices.length > 0) {
                for (File v : voices) {
                    System.out.printf("  ✓ %-35s (%s MB)%n", v.getName(), String.format("%.1f", v.length() / (1024.0 * 1024.0)));
                }
            } else {
                System.out.println("  (No voices installed in models/)");
            }

            System.out.println();
            System.out.println("Available Voices to Download:");
            System.out.println("  [1] de_DE-thorsten-medium  (German Male, ~75 MB)");
            System.out.println("  [2] de_DE-kerstin-medium   (German Female, ~75 MB)");
            System.out.println("  [3] en_US-lessac-medium    (US English Female, ~75 MB)");
            System.out.println("  [4] en_US-ryan-medium      (US English Male, ~75 MB)");
            System.out.println("  [b] Download Piper Binaries (Windows x64)");
            System.out.println("  [t] Test Piper Voice");
            System.out.println("  [back] Back");
            System.out.println();
            System.out.print("Command: ");

            String cmd = scanner.nextLine().trim().toLowerCase();
            if (cmd.equals("back")) return;

            switch (cmd) {
                case "1" -> downloadPiperVoice("de_DE-thorsten-medium");
                case "2" -> downloadPiperVoice("de_DE-kerstin-medium");
                case "3" -> downloadPiperVoice("en_US-lessac-medium");
                case "4" -> downloadPiperVoice("en_US-ryan-medium");
                case "b" -> installPiperBinaries();
                case "t" -> testPiperVoice();
            }
        }
    }

    private void installPiperBinaries() {
        System.out.println("\n[Piper] Downloading Piper Windows x64 Release...");
        File binDir = new File(rootDir, "bin/piper");
        binDir.mkdirs();

        String url = "https://github.com/rhasspy/piper/releases/download/v1.2.0/piper_windows_amd64.zip";
        File tempZip = new File(binDir, "piper_temp.zip");
        downloadFileWithProgress(url, tempZip);

        if (tempZip.exists() && tempZip.length() > 0) {
            System.out.println("[Piper] Extracting binaries to bin/piper/...");
            extractZipPowerShell(tempZip, binDir);
            tempZip.delete();
            System.out.println("[SUCCESS] Piper installed locally in " + binDir.getAbsolutePath());
        }
        pause();
    }

    private void downloadPiperVoice(String voiceKey) {
        System.out.println("\n[Piper] Downloading voice: " + voiceKey + "...");
        File modelsDir = new File(rootDir, "models/piper");
        modelsDir.mkdirs();

        String basePath;
        if (voiceKey.startsWith("de_DE-thorsten")) {
            basePath = "https://huggingface.co/rhasspy/piper-voices/resolve/main/de/de_DE/thorsten/medium/de_DE-thorsten-medium";
        } else if (voiceKey.startsWith("de_DE-kerstin")) {
            basePath = "https://huggingface.co/rhasspy/piper-voices/resolve/main/de/de_DE/kerstin/medium/de_DE-kerstin-medium";
        } else if (voiceKey.startsWith("en_US-lessac")) {
            basePath = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium/en_US-lessac-medium";
        } else if (voiceKey.startsWith("en_US-ryan")) {
            basePath = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/ryan/medium/en_US-ryan-medium";
        } else {
            System.err.println("Unknown voice: " + voiceKey);
            return;
        }

        downloadFileWithProgress(basePath + ".onnx", new File(modelsDir, voiceKey + ".onnx"));
        downloadFileWithProgress(basePath + ".onnx.json", new File(modelsDir, voiceKey + ".onnx.json"));
        System.out.println("[SUCCESS] Voice " + voiceKey + " installed in models/piper/");
        pause();
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
        pause();
    }

    // =========================================================================
    // 3. OmniVoice
    // =========================================================================

    private void manageOmniVoice() {
        clearConsole();
        System.out.println("--- OmniVoice Zero-Shot Voice Cloning ---");
        File omniDir = new File(rootDir, "models/omnivoice");
        File weights = new File(omniDir, "model.safetensors");
        File tok = new File(omniDir, "audio_tokenizer/model.safetensors");

        System.out.println("Directory:        " + omniDir.getAbsolutePath());
        System.out.println("Audio Tokenizer:  " + (tok.exists() ? "✓ Installed (" + (tok.length() / (1024 * 1024)) + " MB)" : "✗ Missing"));
        System.out.println("Model Weights:    " + (weights.exists() ? (weights.length() > 2_400_000_000L ? "✓ Complete (" + (weights.length() / (1024 * 1024)) + " MB)" : "⏳ Incomplete (" + (weights.length() / (1024 * 1024)) + " / 2450 MB)") : "✗ Missing"));
        System.out.println();
        System.out.println("Options:");
        System.out.println("  1. Download / Resume OmniVoice Weights (model.safetensors)");
        System.out.println("  2. Download Audio Tokenizer (audio_tokenizer/model.safetensors)");
        System.out.println("  3. Verify Python Environment & PyTorch");
        System.out.println("  b. Back");
        System.out.println();
        System.out.print("Command: ");

        String cmd = scanner.nextLine().trim().toLowerCase();
        switch (cmd) {
            case "1" -> downloadOmniVoiceWeights();
            case "2" -> downloadOmniVoiceTokenizer();
            case "3" -> verifyPythonEnvironment();
        }
    }

    private void downloadOmniVoiceWeights() {
        File omniDir = new File(rootDir, "models/omnivoice");
        omniDir.mkdirs();
        File target = new File(omniDir, "model.safetensors");
        String url = "https://huggingface.co/k2-fsa/OmniVoice/resolve/main/model.safetensors";
        System.out.println("\n[OmniVoice] Downloading 2.45 GB weights (with resume support)...");
        downloadWithCurlResume(url, target);
        pause();
    }

    private void downloadOmniVoiceTokenizer() {
        File tokDir = new File(rootDir, "models/omnivoice/audio_tokenizer");
        tokDir.mkdirs();
        File target = new File(tokDir, "model.safetensors");
        String url = "https://huggingface.co/k2-fsa/OmniVoice/resolve/main/audio_tokenizer/model.safetensors";
        System.out.println("\n[OmniVoice] Downloading Audio Tokenizer (805 MB)...");
        downloadWithCurlResume(url, target);
        pause();
    }

    private void verifyPythonEnvironment() {
        System.out.println("\nChecking Python & PyTorch...");
        try {
            Process p = new ProcessBuilder("python", "-c", "import torch, torchaudio; print('PyTorch: ' + torch.__version__ + ' | CUDA: ' + str(torch.cuda.is_available()))")
                    .inheritIO().start();
            p.waitFor();
        } catch (Exception e) {
            System.err.println("Python verification failed: " + e.getMessage());
        }
        pause();
    }

    // =========================================================================
    // 4. Dia2 Dialogue
    // =========================================================================

    private void manageDia2() {
        clearConsole();
        System.out.println("--- Dia2 Multi-Speaker Dialogue (GGUF) ---");
        File dia2Dir = new File(rootDir, "models/dia2");
        File weights = new File(dia2Dir, "dia2-2b-q4_k.gguf");
        File runner = new File(rootDir, "bin/dia2/audiocpp_cli.exe");

        System.out.println("GGUF Weights: " + (weights.exists() ? "✓ Installed (" + (weights.length() / (1024 * 1024)) + " MB)" : "✗ Missing"));
        System.out.println("CLI Runner:   " + (runner.exists() ? "✓ Installed (bin/dia2/audiocpp_cli.exe)" : "✗ Pending build"));
        System.out.println();
        System.out.println("Options:");
        System.out.println("  1. Download Dia2 2B Q4_K Weights (2.1 GB)");
        System.out.println("  b. Back");
        System.out.println();
        System.out.print("Command: ");

        String cmd = scanner.nextLine().trim().toLowerCase();
        if (cmd.equals("1")) {
            downloadDia2Weights();
        }
    }

    private void downloadDia2Weights() {
        File dia2Dir = new File(rootDir, "models/dia2");
        dia2Dir.mkdirs();
        File target = new File(dia2Dir, "dia2-2b-q4_k.gguf");
        String url = "https://huggingface.co/nari-labs/Dia2-2B-GGUF/resolve/main/dia2-2b-q4_k.gguf";
        System.out.println("\n[Dia2] Downloading GGUF 2B weights (2.1 GB)...");
        downloadWithCurlResume(url, target);
        pause();
    }

    // =========================================================================
    // 5. Audio Playback Test
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
    // Utilities
    // =========================================================================

    private void downloadFileWithProgress(String urlString, File outputFile) {
        System.out.println("[Download] Fetching " + outputFile.getName() + "...");
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "FastTTS-Installer/0.1.3");
            long total = conn.getContentLengthLong();

            try (InputStream in = new BufferedInputStream(conn.getInputStream());
                 OutputStream out = new FileOutputStream(outputFile)) {
                byte[] buf = new byte[8192];
                int n;
                long read = 0;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    read += n;
                    if (total > 0) {
                        drawProgressBar(read, total);
                    }
                }
            }
            System.out.println("\n[Download Complete] " + outputFile.getName());
        } catch (Exception e) {
            System.err.println("\n[Download Error]: " + e.getMessage());
            if (outputFile.exists() && outputFile.length() == 0) outputFile.delete();
        }
    }

    private void downloadWithCurlResume(String url, File targetFile) {
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

    private void extractZipPowerShell(File zipFile, File destinationDir) {
        try {
            String script = "Expand-Archive -Path '" + zipFile.getAbsolutePath() + "' -DestinationPath '" + destinationDir.getAbsolutePath() + "' -Force";
            Process p = new ProcessBuilder("powershell", "-NoProfile", "-Command", script).inheritIO().start();
            p.waitFor();
        } catch (Exception e) {
            System.err.println("[Extract Error]: " + e.getMessage());
        }
    }

    private void drawProgressBar(long current, long total) {
        int percent = (int) (current * 100 / total);
        int barLength = 30;
        int completed = (int) (current * barLength / total);

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < completed) bar.append("█");
            else bar.append("░");
        }
        bar.append("] ").append(percent).append("% (")
                .append(current / (1024 * 1024)).append("/")
                .append(total / (1024 * 1024)).append(" MB)\r");

        System.out.print(bar);
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
