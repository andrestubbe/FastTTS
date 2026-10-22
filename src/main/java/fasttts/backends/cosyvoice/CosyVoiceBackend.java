package fasttts.backends.cosyvoice;

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
 * CosyVoice3 TTS Backend powered by cosyvoice.cpp native runtime.
 * Multi-lingual zero-shot voice cloning TTS system from Alibaba's FunAudioLLM team.
 * Apache-2.0 licensed, supports 9 languages plus 18 Chinese dialects via GGUF quantized models.
 */
public final class CosyVoiceBackend implements FastTTSBackend {

    private String runnerPath;
    private String modelPath;
    private volatile boolean prepared = false;

    public CosyVoiceBackend() {
        this(resolveRunnerPath(), resolveModelPath());
    }

    public CosyVoiceBackend(String runnerPath, String modelPath) {
        this.runnerPath = runnerPath != null ? runnerPath : resolveRunnerPath();
        this.modelPath = modelPath != null ? modelPath : resolveModelPath();
    }

    private static String resolveRunnerPath() {
        String[] candidates = {
                "bin/cosyvoice/cosyvoice-cli.exe",
                "bin/cosyvoice-cli.exe",
                "cosyvoice-cli.exe"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        String userHome = System.getProperty("user.home", ".");
        File homeBin = new File(userHome, ".fasttts/cosyvoice/cosyvoice-cli.exe");
        if (homeBin.exists()) return homeBin.getAbsolutePath();
        return "bin/cosyvoice/cosyvoice-cli.exe";
    }

    private static String resolveModelPath() {
        String[] candidates = {
                "models/cosyvoice/cosyvoice3-llm-q4_k.gguf",
                "models/cosyvoice/cosyvoice3-llm-f16.gguf",
                "models/cosyvoice/cosyvoice3-llm.gguf"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.length() > 100_000_000L) return f.getAbsolutePath();
        }
        String userHome = System.getProperty("user.home", ".");
        File homeModel = new File(userHome, ".fasttts/cosyvoice/cosyvoice3-llm-q4_k.gguf");
        if (homeModel.exists()) return homeModel.getAbsolutePath();
        return "models/cosyvoice/cosyvoice3-llm-q4_k.gguf";
    }

    @Override
    public String getName() {
        return "CosyVoice";
    }

    @Override
    public boolean isAvailable() {
        File r = new File(runnerPath);
        File m = new File(modelPath);
        return r.exists() && m.exists() && m.length() > 100_000_000L;
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
            throw new FileNotFoundException("CosyVoice runner or model not found. Runner: "
                    + runnerPath + ", Model: " + modelPath);
        }

        // Resolve voice ID (default: zero_shot)
        String voiceId = (voice != null && voice.id() != null && !voice.id().isBlank())
                ? voice.id()
                : "zero_shot";

        Path tempOut = Files.createTempFile("cosyvoice_out", ".wav");
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(new File(runnerPath).getAbsolutePath());
            cmd.add("--backend");
            cmd.add("cpu");
            cmd.add("--model");
            cmd.add(new File(modelPath).getAbsolutePath());
            cmd.add("--speaker");
            cmd.add(voiceId);
            cmd.add("--text");
            cmd.add(text);
            cmd.add("--output");
            cmd.add(tempOut.toAbsolutePath().toString());

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder out = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append("\n");
                }
            }

            boolean finished = p.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new TimeoutException("CosyVoice synthesis timed out after 120 seconds");
            }

            int exit = p.exitValue();
            if (exit != 0 || !Files.exists(tempOut) || Files.size(tempOut) == 0) {
                throw new RuntimeException("CosyVoice synthesis failed (exit code " + exit + "):\n" + out);
            }

            byte[] wavData = Files.readAllBytes(tempOut);
            return new FastTTSAudio(wavData, 24000);
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
        return CosyVoiceVoice.all();
    }
}
