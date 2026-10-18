package fasttts.backends.matcha;

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
 * Matcha-TTS Backend powered by Sherpa-ONNX runtime.
 * Ultra-fast non-autoregressive neural speech synthesis based on optimal-transport Flow Matching.
 * Delivers studio-level prosody and audio quality with extreme CPU real-time speeds (< 0.1x RTF).
 */
public final class MatchaBackend implements FastTTSBackend {

    private String binaryPath;
    private String acousticModelPath;
    private String vocoderPath;
    private String tokensPath;
    private String dataDir;
    private volatile boolean prepared = false;

    public MatchaBackend() {
        this(resolveBinaryPath(), resolveAcousticModel(), resolveVocoder(), resolveTokens(), resolveDataDir());
    }

    public MatchaBackend(String binaryPath, String acousticModelPath, String vocoderPath, String tokensPath, String dataDir) {
        this.binaryPath = binaryPath != null ? binaryPath : resolveBinaryPath();
        this.acousticModelPath = acousticModelPath != null ? acousticModelPath : resolveAcousticModel();
        this.vocoderPath = vocoderPath != null ? vocoderPath : resolveVocoder();
        this.tokensPath = tokensPath != null ? tokensPath : resolveTokens();
        this.dataDir = dataDir != null ? dataDir : resolveDataDir();
    }

    private static String resolveBinaryPath() {
        String[] candidates = {
                "bin/sherpa-onnx/sherpa-onnx-offline-tts.exe",
                "bin/kokoro/sherpa-onnx-offline-tts.exe",
                "bin/pocket/sherpa-onnx-offline-tts.exe",
                "bin/matcha/sherpa-onnx-offline-tts.exe",
                "bin/sherpa-onnx-offline-tts.exe",
                "sherpa-onnx-offline-tts.exe"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        String userHome = System.getProperty("user.home", ".");
        File homeBin = new File(userHome, ".speak/bin/sherpa-onnx/sherpa-onnx-offline-tts.exe");
        if (homeBin.exists()) return homeBin.getAbsolutePath();
        File homeBinLegacy = new File(userHome, ".speak/bin/sherpa-onnx-offline-tts.exe");
        if (homeBinLegacy.exists()) return homeBinLegacy.getAbsolutePath();
        return "bin/sherpa-onnx/sherpa-onnx-offline-tts.exe";
    }

    private static String resolveAcousticModel() {
        String[] candidates = {
                "models/matcha/matcha-icefall-en_US-ljspeech/model-steps-3.onnx",
                "models/matcha/model-steps-3.onnx",
                "models/matcha/model.onnx"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        return "models/matcha/matcha-icefall-en_US-ljspeech/model-steps-3.onnx";
    }

    private static String resolveVocoder() {
        String[] candidates = {
                "models/matcha/vocos-22khz-univ.onnx",
                "models/matcha/hifigan_v2.onnx",
                "models/vocos-22khz-univ.onnx"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        return "models/matcha/vocos-22khz-univ.onnx";
    }

    private static String resolveTokens() {
        String[] candidates = {
                "models/matcha/matcha-icefall-en_US-ljspeech/tokens.txt",
                "models/matcha/tokens.txt"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        return "models/matcha/matcha-icefall-en_US-ljspeech/tokens.txt";
    }

    private static String resolveDataDir() {
        String[] candidates = {
                "models/matcha/matcha-icefall-en_US-ljspeech/espeak-ng-data",
                "models/matcha/espeak-ng-data",
                "models/kokoro/kokoro-en-v0_19/espeak-ng-data"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.isDirectory()) return f.getAbsolutePath();
        }
        return "models/matcha/matcha-icefall-en_US-ljspeech/espeak-ng-data";
    }

    @Override
    public String getName() {
        return "Matcha";
    }

    @Override
    public boolean isAvailable() {
        return new File(binaryPath).exists()
                && new File(acousticModelPath).exists()
                && new File(vocoderPath).exists()
                && new File(tokensPath).exists();
    }

    @Override
    public synchronized void prepare() {
        if (prepared) return;
        if (!isAvailable()) return;
        try (RandomAccessFile raf = new RandomAccessFile(acousticModelPath, "r")) {
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
            throw new FileNotFoundException("Matcha-TTS binary or model files not found. Acoustic: "
                    + acousticModelPath + ", Vocoder: " + vocoderPath);
        }

        Path tempOut = Files.createTempFile("matcha_out", ".wav");
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(new File(binaryPath).getAbsolutePath());
            cmd.add("--matcha-acoustic-model=" + new File(acousticModelPath).getAbsolutePath());
            cmd.add("--matcha-vocoder=" + new File(vocoderPath).getAbsolutePath());
            cmd.add("--matcha-tokens=" + new File(tokensPath).getAbsolutePath());
            if (new File(dataDir).exists()) {
                cmd.add("--matcha-data-dir=" + new File(dataDir).getAbsolutePath());
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
                // length-scale is inverse of speed
                cmd.add("--matcha-length-scale=" + (1.0f / speed));
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
                throw new TimeoutException("Matcha-TTS synthesis timed out after 30 seconds");
            }

            if (p.exitValue() != 0) {
                throw new RuntimeException("Matcha-TTS synthesis failed with exit code " + p.exitValue());
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
        return List.of(
                new FastTTSVoice("matcha-ljspeech", "Matcha-LJSpeech", "en-US", "Flow Matching Female", "matcha")
        );
    }
}
