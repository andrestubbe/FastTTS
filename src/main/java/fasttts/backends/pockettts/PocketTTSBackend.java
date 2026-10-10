package fasttts.backends.pockettts;

import fasttts.core.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pocket TTS Backend powered by Sherpa-ONNX runtime.
 * Ultra-compact, fast, offline CPU neural text-to-speech engine.
 */
public final class PocketTTSBackend implements FastTTSBackend {

    private String binaryPath;
    private String modelDir;
    private String referenceAudio;

    public PocketTTSBackend() {
        this(resolveBinaryPath(), resolveModelDir(), resolveReferenceAudio(resolveModelDir(), null));
    }

    public PocketTTSBackend(String binaryPath, String modelDir, String referenceAudio) {
        this.binaryPath = binaryPath != null ? binaryPath : resolveBinaryPath();
        this.modelDir = modelDir != null ? modelDir : resolveModelDir();
        this.referenceAudio = referenceAudio != null ? referenceAudio : resolveReferenceAudio(this.modelDir, null);
    }

    private static String resolveBinaryPath() {
        String[] candidates = new String[]{
                "bin/pocket/sherpa-onnx-offline-tts.exe",
                "bin/sherpa-onnx-offline-tts.exe",
                "sherpa-onnx-offline-tts.exe"
        };
        for (String c : candidates) {
            if (new File(c).exists()) return new File(c).getAbsolutePath();
        }
        String userHome = System.getProperty("user.home", ".");
        File homeBin = new File(userHome, ".speak/bin/pocket/sherpa-onnx-offline-tts.exe");
        if (homeBin.exists()) return homeBin.getAbsolutePath();
        return "bin/pocket/sherpa-onnx-offline-tts.exe";
    }

    private static String resolveModelDir() {
        String[] candidates = new String[]{
                "models/pocket/sherpa-onnx-pocket-tts-int8-2026-01-26",
                "models/pocket",
                "models/sherpa-onnx-pocket-tts-int8-2026-01-26"
        };
        for (String c : candidates) {
            if (new File(c).exists() && new File(c, "lm_main.int8.onnx").exists()) {
                return new File(c).getAbsolutePath();
            }
        }
        String userHome = System.getProperty("user.home", ".");
        File homeModel = new File(userHome, ".speak/models/pocket/sherpa-onnx-pocket-tts-int8-2026-01-26");
        if (homeModel.exists()) return homeModel.getAbsolutePath();
        return "models/pocket/sherpa-onnx-pocket-tts-int8-2026-01-26";
    }

    private static String resolveReferenceAudio(String modelDir, String voiceHint) {
        if (modelDir == null) return null;
        File wavsDir = new File(modelDir, "test_wavs");
        if (wavsDir.exists()) {
            if (voiceHint != null && !voiceHint.isBlank()) {
                File target = new File(wavsDir, voiceHint.replace("pocket-", "") + ".wav");
                if (target.exists()) return target.getAbsolutePath();
            }
            File bria = new File(wavsDir, "bria.wav");
            if (bria.exists()) return bria.getAbsolutePath();
            File loona = new File(wavsDir, "loona.wav");
            if (loona.exists()) return loona.getAbsolutePath();
            File[] files = wavsDir.listFiles((dir, name) -> name.endsWith(".wav"));
            if (files != null && files.length > 0) return files[0].getAbsolutePath();
        }
        return null;
    }

    @Override
    public FastTTSAudio synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        File binFile = new File(binaryPath);
        if (!binFile.exists()) {
            String resolved = resolveBinaryPath();
            if (new File(resolved).exists()) {
                this.binaryPath = resolved;
            } else {
                throw new FileNotFoundException("Pocket TTS binary not found at [" + binFile.getAbsolutePath() + "]");
            }
        }

        File mDir = new File(modelDir);
        if (!mDir.exists()) {
            String resolved = resolveModelDir();
            if (new File(resolved).exists()) {
                this.modelDir = resolved;
            } else {
                throw new FileNotFoundException("Pocket TTS model dir not found at [" + mDir.getAbsolutePath() + "]");
            }
        }

        String voiceAudio = referenceAudio;
        if (voice != null && voice.id() != null && !voice.id().isBlank()) {
            if (new File(voice.id()).exists()) {
                voiceAudio = voice.id();
            } else {
                String byHint = resolveReferenceAudio(modelDir, voice.id());
                if (byHint != null) voiceAudio = byHint;
            }
        }

        Path tempOutput = Files.createTempFile("pocket_out", ".wav");

        List<String> cmd = new ArrayList<>();
        cmd.add(binaryPath);
        cmd.add("--pocket-lm-flow=" + new File(modelDir, "lm_flow.int8.onnx").getAbsolutePath());
        cmd.add("--pocket-lm-main=" + new File(modelDir, "lm_main.int8.onnx").getAbsolutePath());
        cmd.add("--pocket-encoder=" + new File(modelDir, "encoder.onnx").getAbsolutePath());
        cmd.add("--pocket-decoder=" + new File(modelDir, "decoder.int8.onnx").getAbsolutePath());
        cmd.add("--pocket-text-conditioner=" + new File(modelDir, "text_conditioner.onnx").getAbsolutePath());
        cmd.add("--pocket-vocab-json=" + new File(modelDir, "vocab.json").getAbsolutePath());
        cmd.add("--pocket-token-scores-json=" + new File(modelDir, "token_scores.json").getAbsolutePath());

        if (voiceAudio != null && new File(voiceAudio).exists()) {
            cmd.add("--reference-audio=" + voiceAudio);
        }

        int threads = Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
        cmd.add("--num-threads=" + threads);

        cmd.add("--output-filename=" + tempOutput.toAbsolutePath());
        cmd.add(text);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        if (binFile.getParentFile() != null) {
            pb.directory(binFile.getParentFile());
        }
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);

        Process p = pb.start();
        if (p.waitFor() != 0) {
            Files.deleteIfExists(tempOutput);
            throw new RuntimeException("Pocket TTS synthesis process failed.");
        }

        byte[] wavData = Files.readAllBytes(tempOutput);
        Files.deleteIfExists(tempOutput);

        return new FastTTSAudio(wavData, 24000);
    }

    @Override
    public void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception {
        FastTTSAudio audio = synthesize(text, voice, config);
        chunkConsumer.accept(audio.getData());
    }

    @Override
    public List<FastTTSVoice> getVoices() {
        List<FastTTSVoice> list = new ArrayList<>();
        File wavsDir = new File(modelDir, "test_wavs");
        if (wavsDir.exists()) {
            File[] files = wavsDir.listFiles((dir, name) -> name.endsWith(".wav"));
            if (files != null) {
                for (File f : files) {
                    String baseName = f.getName().replace(".wav", "");
                    list.add(new FastTTSVoice(f.getAbsolutePath(), "pocket-" + baseName, "en/multilingual", "Neural", "pocket"));
                }
            }
        }
        if (list.isEmpty()) {
            list.add(new FastTTSVoice("bria", "pocket-bria", "en/multilingual", "Female (Neural)", "pocket"));
            list.add(new FastTTSVoice("loona", "pocket-loona", "en/multilingual", "Female (Neural)", "pocket"));
        }
        return list;
    }

    @Override
    public String getName() {
        return "PocketTTS";
    }
}
