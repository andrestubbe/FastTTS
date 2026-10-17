package fasttts.backends.vits;

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
 * VITS (Variational Inference for Text-to-Speech) Backend powered by Sherpa-ONNX.
 * End-to-end neural TTS system offering deterministic, ultra-low latency audio synthesis.
 */
public final class VITSBackend implements FastTTSBackend {

    private String binaryPath;
    private String modelPath;
    private String tokensPath;
    private String dataDir;
    private volatile boolean prepared = false;

    public VITSBackend() {
        this(resolveBinaryPath(), resolveModelPath(), resolveTokensPath(), resolveDataDir());
    }

    public VITSBackend(String binaryPath, String modelPath, String tokensPath, String dataDir) {
        this.binaryPath = binaryPath != null ? binaryPath : resolveBinaryPath();
        this.modelPath = modelPath != null ? modelPath : resolveModelPath();
        this.tokensPath = tokensPath != null ? tokensPath : resolveTokensPath();
        this.dataDir = dataDir != null ? dataDir : resolveDataDir();
    }

    private static String resolveBinaryPath() {
        String[] candidates = {
                "bin/kokoro/sherpa-onnx-offline-tts.exe",
                "bin/pocket/sherpa-onnx-offline-tts.exe",
                "bin/vits/sherpa-onnx-offline-tts.exe",
                "bin/sherpa-onnx-offline-tts.exe",
                "sherpa-onnx-offline-tts.exe"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        return "bin/kokoro/sherpa-onnx-offline-tts.exe";
    }

    private static String resolveModelPath() {
        File vitsDir = new File("models/vits");
        if (vitsDir.exists()) {
            File[] subdirs = vitsDir.listFiles(File::isDirectory);
            if (subdirs != null) {
                for (File dir : subdirs) {
                    File[] onnxFiles = dir.listFiles((d, name) -> name.endsWith(".onnx") && !name.endsWith(".json"));
                    if (onnxFiles != null && onnxFiles.length > 0) {
                        return onnxFiles[0].getAbsolutePath();
                    }
                }
            }
            File[] directOnnx = vitsDir.listFiles((d, name) -> name.endsWith(".onnx") && !name.endsWith(".json"));
            if (directOnnx != null && directOnnx.length > 0) {
                return directOnnx[0].getAbsolutePath();
            }
        }
        return "models/vits/vits-piper-en_US-amy-low/en_US-amy-low.onnx";
    }

    private static String resolveTokensPath() {
        File modelFile = new File(resolveModelPath());
        if (modelFile.getParentFile() != null) {
            File tokens = new File(modelFile.getParentFile(), "tokens.txt");
            if (tokens.exists()) return tokens.getAbsolutePath();
        }
        File defaultTokens = new File("models/vits/tokens.txt");
        if (defaultTokens.exists()) return defaultTokens.getAbsolutePath();
        return "models/vits/vits-piper-en_US-amy-low/tokens.txt";
    }

    private static String resolveDataDir() {
        File modelFile = new File(resolveModelPath());
        if (modelFile.getParentFile() != null) {
            File espeak = new File(modelFile.getParentFile(), "espeak-ng-data");
            if (espeak.exists() && espeak.isDirectory()) return espeak.getAbsolutePath();
        }
        String[] fallbackDirs = {
                "models/vits/espeak-ng-data",
                "models/matcha/matcha-icefall-en_US-ljspeech/espeak-ng-data",
                "models/kokoro/kokoro-en-v0_19/espeak-ng-data"
        };
        for (String fb : fallbackDirs) {
            File f = new File(fb);
            if (f.exists() && f.isDirectory()) return f.getAbsolutePath();
        }
        return "models/vits/vits-piper-en_US-amy-low/espeak-ng-data";
    }

    @Override
    public String getName() {
        return "VITS";
    }

    @Override
    public boolean isAvailable() {
        return new File(binaryPath).exists()
                && new File(modelPath).exists()
                && new File(tokensPath).exists();
    }

    @Override
    public synchronized void prepare() {
        if (prepared) return;
        if (!isAvailable()) return;
        try (RandomAccessFile raf = new RandomAccessFile(modelPath, "r")) {
            byte[] buf = new byte[65536];
            raf.read(buf);
            prepared = true;
        } catch (Exception ignored) {}
    }

    @Override
    public boolean isPrepared() {
        return prepared;
    }

    @Override
    public FastTTSAudio synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        if (!isAvailable()) {
            throw new FileNotFoundException("VITS binary or model file not found at: " + modelPath);
        }

        Path tempOut = Files.createTempFile("vits_out", ".wav");
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(new File(binaryPath).getAbsolutePath());
            cmd.add("--vits-model=" + new File(modelPath).getAbsolutePath());
            cmd.add("--vits-tokens=" + new File(tokensPath).getAbsolutePath());
            if (new File(dataDir).exists()) {
                cmd.add("--vits-data-dir=" + new File(dataDir).getAbsolutePath());
            }

            int threads = Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
            cmd.add("--num-threads=" + threads);

            float speed = 1.0f;
            if (config != null && config.getProperty("speed") != null) {
                try {
                    speed = Float.parseFloat(config.getProperty("speed"));
                } catch (NumberFormatException ignored) {}
            }
            if (speed != 1.0f) {
                cmd.add("--vits-length-scale=" + (1.0f / speed));
            }

            cmd.add("--output-filename=" + tempOut.toAbsolutePath());
            cmd.add(text);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            File binParent = new File(binaryPath).getParentFile();
            if (binParent != null) {
                pb.directory(binParent);
            }
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);

            Process p = pb.start();
            boolean finished = p.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new TimeoutException("VITS synthesis timed out after 30 seconds");
            }

            if (p.exitValue() != 0) {
                throw new RuntimeException("VITS synthesis failed with exit code " + p.exitValue());
            }

            byte[] wavData = Files.readAllBytes(tempOut);
            return new FastTTSAudio(wavData, 22050);
        } finally {
            try {
                Files.deleteIfExists(tempOut);
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
        File mFile = new File(modelPath);
        String voiceName = mFile.exists() ? mFile.getName().replace(".onnx", "") : "vits-amy";
        return List.of(
                new FastTTSVoice(voiceName, "VITS-" + voiceName, "en-US", "Deterministic VITS Neural", "vits")
        );
    }
}
