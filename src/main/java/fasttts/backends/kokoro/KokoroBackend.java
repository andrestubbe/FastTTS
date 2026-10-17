package fasttts.backends.kokoro;

import fasttts.core.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Kokoro TTS Backend powered by Sherpa-ONNX runtime.
 * High-performance, offline studio-grade neural text-to-speech engine.
 */
public final class KokoroBackend implements FastTTSBackend {

    private String binaryPath;
    private String modelDir;

    public KokoroBackend() {
        this(resolveBinaryPath(), resolveModelDir());
    }

    public KokoroBackend(String binaryPath, String modelDir) {
        this.binaryPath = binaryPath != null ? binaryPath : resolveBinaryPath();
        this.modelDir = modelDir != null ? modelDir : resolveModelDir();
    }

    private static String resolveBinaryPath() {
        String userHome = System.getProperty("user.home", ".");
        String[] candidates = new String[]{
                "bin/kokoro/sherpa-onnx-offline-tts.exe",
                "bin/sherpa-onnx-offline-tts.exe",
                "sherpa-onnx-offline-tts.exe",
                userHome + "/Documents/2026-08-17-Work-FastJava/FastTTS/bin/kokoro/sherpa-onnx-offline-tts.exe",
                "../2026-08-17-Work-FastJava/FastTTS/bin/kokoro/sherpa-onnx-offline-tts.exe",
                userHome + "/.fasttts/kokoro/sherpa-onnx-offline-tts.exe",
                userHome + "/.speak/bin/kokoro/sherpa-onnx-offline-tts.exe"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }

        return "bin/kokoro/sherpa-onnx-offline-tts.exe";
    }

    private static String resolveModelDir() {
        String userHome = System.getProperty("user.home", ".");
        String[] candidates = new String[]{
                "models/kokoro",
                "models/kokoro/kokoro-en-v0_19",
                "models/kokoro-en-v0_19",
                userHome + "/Documents/2026-08-17-Work-FastJava/FastTTS/models/kokoro/kokoro-en-v0_19",
                userHome + "/Documents/2026-08-17-Work-FastJava/FastTTS/models/kokoro",
                "../2026-08-17-Work-FastJava/FastTTS/models/kokoro/kokoro-en-v0_19",
                "../2026-08-17-Work-FastJava/FastTTS/models/kokoro",
                userHome + "/.fasttts/kokoro/kokoro-en-v0_19",
                userHome + "/.fasttts/kokoro"
        };
        for (String c : candidates) {
            File dir = new File(c);
            if (dir.exists() && (new File(dir, "model.onnx").exists() || new File(dir, "model.int8.onnx").exists())) {
                return dir.getAbsolutePath();
            }
        }

        return "models/kokoro";
    }

    private File resolveFile(String filename, String altFilename) {
        File dir = new File(modelDir);
        File f1 = new File(dir, filename);
        if (f1.exists()) return f1;
        if (altFilename != null) {
            File f2 = new File(dir, altFilename);
            if (f2.exists()) return f2;
        }
        // Also check nested subfolder if extracted inside kokoro-en-v0_19
        File sub = new File(dir, "kokoro-en-v0_19");
        if (sub.exists()) {
            File sub1 = new File(sub, filename);
            if (sub1.exists()) return sub1;
            if (altFilename != null) {
                File sub2 = new File(sub, altFilename);
                if (sub2.exists()) return sub2;
            }
        }
        return f1;
    }

    @Override
    public FastTTSAudio synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        File binFile = new File(binaryPath);
        if (!binFile.exists()) {
            String resolved = resolveBinaryPath();
            if (new File(resolved).exists()) {
                this.binaryPath = resolved;
                binFile = new File(resolved);
            } else {
                throw new FileNotFoundException("Kokoro binary (sherpa-onnx-offline-tts.exe) not found at: " + binFile.getAbsolutePath());
            }
        }

        File mDir = new File(modelDir);
        if (!mDir.exists()) {
            String resolved = resolveModelDir();
            if (new File(resolved).exists()) {
                this.modelDir = resolved;
            }
        }

        File modelFile = resolveFile("model.onnx", "model.int8.onnx");
        File voicesFile = resolveFile("voices.bin", null);
        File tokensFile = resolveFile("tokens.txt", null);
        File espeakDataDir = resolveFile("espeak-ng-data", null);

        if (!modelFile.exists()) {
            throw new FileNotFoundException("Kokoro model.onnx not found at: " + modelFile.getAbsolutePath());
        }
        if (!voicesFile.exists()) {
            throw new FileNotFoundException("Kokoro voices.bin not found at: " + voicesFile.getAbsolutePath());
        }
        if (!tokensFile.exists()) {
            throw new FileNotFoundException("Kokoro tokens.txt not found at: " + tokensFile.getAbsolutePath());
        }
        if (!espeakDataDir.exists()) {
            throw new FileNotFoundException("Kokoro espeak-ng-data not found at: " + espeakDataDir.getAbsolutePath());
        }

        int sid = KokoroVoice.resolveSpeakerId(voice);

        Path tempOutput = Files.createTempFile("kokoro_out", ".wav");
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(binFile.getAbsolutePath());
            cmd.add("--kokoro-model=" + modelFile.getAbsolutePath());
            cmd.add("--kokoro-voices=" + voicesFile.getAbsolutePath());
            cmd.add("--kokoro-tokens=" + tokensFile.getAbsolutePath());
            cmd.add("--kokoro-data-dir=" + espeakDataDir.getAbsolutePath());
            cmd.add("--sid=" + sid);

            float rate = (config != null && config.getRate() > 0.0f) ? config.getRate() : 1.0f;
            // In sherpa-onnx: kokoro-length-scale: larger = slower, smaller = faster
            float lengthScale = 1.0f / Math.max(0.2f, Math.min(5.0f, rate));
            if (Math.abs(lengthScale - 1.0f) > 0.01f) {
                cmd.add(String.format(java.util.Locale.US, "--kokoro-length-scale=%.2f", lengthScale));
            }

            int threads = Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
            cmd.add("--num-threads=" + threads);
            cmd.add("--output-filename=" + tempOutput.toAbsolutePath());
            cmd.add(text);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            if (binFile.getParentFile() != null) {
                pb.directory(binFile.getParentFile());
            }
            pb.redirectErrorStream(true);

            Process p = pb.start();
            StringBuilder procOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    procOutput.append(line).append("\n");
                }
            }

            boolean finished = p.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new TimeoutException("Kokoro synthesis timed out after 60 seconds");
            }

            int exitCode = p.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("Kokoro TTS synthesis failed (exit code " + exitCode + "):\n" + procOutput);
            }

            byte[] wavData = Files.readAllBytes(tempOutput);
            // Kokoro outputs 24000 Hz studio audio
            return new FastTTSAudio(wavData, 24000);
        } finally {
            try {
                Files.deleteIfExists(tempOutput);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception {
        FastTTSAudio audio = synthesize(text, voice, config);
        chunkConsumer.accept(audio.getData());
    }

    @Override
    public List<FastTTSVoice> getVoices() {
        return KokoroVoice.all();
    }

    @Override
    public String getName() {
        return "Kokoro";
    }

    private volatile boolean prepared = false;

    @Override
    public synchronized void prepare() {
        if (prepared) return;
        if (!isAvailable()) return;
        try {
            // Execute mini-warmup to page model into OS cache and warm up ONNX runtime DLLs
            synthesize(".", KokoroVoice.AF_BELLA, null);
            prepared = true;
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean isPrepared() {
        return prepared;
    }

    @Override
    public boolean isAvailable() {
        File binFile = new File(binaryPath);
        if (!binFile.exists()) {
            String resolved = resolveBinaryPath();
            if (!new File(resolved).exists()) return false;
        }
        File modelFile = resolveFile("model.onnx", "model.int8.onnx");
        File voicesFile = resolveFile("voices.bin", null);
        File tokensFile = resolveFile("tokens.txt", null);
        File espeakDataDir = resolveFile("espeak-ng-data", null);
        return modelFile.exists() && voicesFile.exists() && tokensFile.exists() && espeakDataDir.exists();
    }

    public String getBinaryPath() {
        return binaryPath;
    }

    public String getModelDir() {
        return modelDir;
    }
}
