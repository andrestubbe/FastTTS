package fasttts.backends.orpheus;

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
 * Orpheus TTS Backend powered by CrispASR native runtime.
 * Llama-3b-based speech-LLM with zero-shot voice cloning, emotion control, and streaming capabilities.
 * Apache-2.0 licensed, CPU-only inference via GGUF quantized models.
 */
public final class OrpheusBackend implements FastTTSBackend {

    private String runnerPath;
    private String modelPath;
    private String codecPath;
    private volatile boolean prepared = false;

    public OrpheusBackend() {
        this(resolveRunnerPath(), resolveModelPath(), resolveCodecPath());
    }

    public OrpheusBackend(String runnerPath, String modelPath, String codecPath) {
        this.runnerPath = runnerPath != null ? runnerPath : resolveRunnerPath();
        this.modelPath = modelPath != null ? modelPath : resolveModelPath();
        this.codecPath = codecPath != null ? codecPath : resolveCodecPath();
    }

    private static String resolveRunnerPath() {
        String[] candidates = {
                "bin/orpheus/crispasr.exe",
                "bin/crispasr.exe",
                "crispasr.exe"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        String userHome = System.getProperty("user.home", ".");
        File homeBin = new File(userHome, ".fasttts/orpheus/crispasr.exe");
        if (homeBin.exists()) return homeBin.getAbsolutePath();
        return "bin/orpheus/crispasr.exe";
    }

    private static String resolveModelPath() {
        String[] candidates = {
                "models/orpheus/orpheus-3b-0.1-ft-q8_0.gguf",
                "models/orpheus/orpheus-3b-0.1-ft-f16.gguf",
                "models/orpheus/orpheus-3b-0.1-ft-q4_k.gguf",
                "models/orpheus/orpheus-3b-0.1-ft.gguf"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.length() > 100_000_000L) return f.getAbsolutePath();
        }
        String userHome = System.getProperty("user.home", ".");
        File homeModel = new File(userHome, ".fasttts/orpheus/orpheus-3b-0.1-ft-q8_0.gguf");
        if (homeModel.exists()) return homeModel.getAbsolutePath();
        return "models/orpheus/orpheus-3b-0.1-ft-q8_0.gguf";
    }

    private static String resolveCodecPath() {
        String[] candidates = {
                "models/orpheus/snac-24khz.gguf",
                "models/orpheus/snac.gguf"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.length() > 10_000_000L) return f.getAbsolutePath();
        }
        String userHome = System.getProperty("user.home", ".");
        File homeCodec = new File(userHome, ".fasttts/orpheus/snac-24khz.gguf");
        if (homeCodec.exists()) return homeCodec.getAbsolutePath();
        return "models/orpheus/snac-24khz.gguf";
    }

    @Override
    public String getName() {
        return "Orpheus";
    }

    @Override
    public boolean isAvailable() {
        File r = new File(runnerPath);
        File m = new File(modelPath);
        File c = new File(codecPath);
        return r.exists() && m.exists() && m.length() > 100_000_000L && c.exists() && c.length() > 10_000_000L;
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
            throw new FileNotFoundException("Orpheus CrispASR runner or model not found. Runner: "
                    + runnerPath + ", Model: " + modelPath + ", Codec: " + codecPath);
        }

        // Resolve voice ID (default: tara)
        String voiceId = (voice != null && voice.id() != null && !voice.id().isBlank())
                ? voice.id()
                : "tara";

        Path tempOut = Files.createTempFile("orpheus_out", ".wav");
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(new File(runnerPath).getAbsolutePath());
            cmd.add("--backend");
            cmd.add("orpheus");
            cmd.add("-m");
            cmd.add(new File(modelPath).getAbsolutePath());
            cmd.add("--codec-model");
            cmd.add(new File(codecPath).getAbsolutePath());
            cmd.add("--voice");
            cmd.add(voiceId);
            cmd.add("--prompt");
            cmd.add(text);
            cmd.add("--save-path");
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
                throw new TimeoutException("Orpheus synthesis timed out after 120 seconds");
            }

            int exit = p.exitValue();
            if (exit != 0 || !Files.exists(tempOut) || Files.size(tempOut) == 0) {
                throw new RuntimeException("Orpheus synthesis failed (exit code " + exit + "):\n" + out);
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
        return OrpheusVoice.all();
    }
}
