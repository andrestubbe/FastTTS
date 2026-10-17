package fasttts.backends.dia2;

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
 * Dia2 Dialogue TTS Backend using quantized GGUF models (Nari Labs Dia2-2B).
 * Supports multi-speaker scripted dialogue ([S1], [S2]) and zero-copy streaming audio.
 */
public final class Dia2Backend implements FastTTSBackend {

    private final String modelPath;
    private final String runnerPath;
    private volatile boolean prepared = false;

    public Dia2Backend() {
        this(resolveModelPath(), resolveRunnerPath());
    }

    public Dia2Backend(String modelPath) {
        this(modelPath, resolveRunnerPath());
    }

    public Dia2Backend(String modelPath, String runnerPath) {
        this.modelPath = modelPath != null ? modelPath : resolveModelPath();
        this.runnerPath = runnerPath != null ? runnerPath : resolveRunnerPath();
    }

    private static String resolveModelPath() {
        String[] candidates = {
                "models/dia2-2b-q4_k.gguf",
                "models/dia2/dia2-2b-q4_k.gguf",
                "../2026-08-17-Work-FastJava/FastTTS/models/dia2-2b-q4_k.gguf",
                System.getProperty("user.home", ".") + "/Documents/2026-08-17-Work-FastJava/FastTTS/models/dia2-2b-q4_k.gguf"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        return "models/dia2-2b-q4_k.gguf";
    }

    private static String resolveRunnerPath() {
        String[] candidates = {
                "bin/dia2/audiocpp_cli.exe",
                "bin/dia2/dia2_cli.exe",
                "../2026-08-17-Work-FastJava/FastTTS/bin/dia2/audiocpp_cli.exe",
                System.getProperty("user.home", ".") + "/Documents/2026-08-17-Work-FastJava/FastTTS/bin/dia2/audiocpp_cli.exe"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        return "bin/dia2/audiocpp_cli.exe";
    }

    @Override
    public String getName() {
        return "Dia2";
    }

    @Override
    public boolean isAvailable() {
        return new File(modelPath).exists() && new File(runnerPath).exists();
    }

    public boolean hasModel() {
        return new File(modelPath).exists();
    }

    @Override
    public synchronized void prepare() {
        if (prepared) return;
        if (!hasModel()) return;

        // Page the GGUF model into OS page cache
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
        if (!hasModel()) {
            throw new FileNotFoundException("Dia2 GGUF model file not found at: " + modelPath);
        }
        if (!new File(runnerPath).exists()) {
            throw new FileNotFoundException("Dia2 runtime (audio.cpp-dia2) not found at: " + runnerPath
                    + ". The GGUF model is present at '" + modelPath + "', but requires audio.cpp-dia2 build for native inference.");
        }

        Path tempOut = Files.createTempFile("dia2_out", ".wav");
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(new File(runnerPath).getAbsolutePath());
            cmd.add("--task");
            cmd.add("tts");
            cmd.add("--family");
            cmd.add("dia2");
            cmd.add("--model");
            cmd.add(new File(modelPath).getAbsolutePath());
            cmd.add("--backend");
            cmd.add("cpu");
            cmd.add("--text");

            // Format multi-speaker prefix if voice is speaker specific
            String formattedText = text;
            if (voice != null && voice.id() != null) {
                if (voice.id().equalsIgnoreCase("dia2_s1") && !text.contains("[S1]")) {
                    formattedText = "[S1] " + text;
                } else if (voice.id().equalsIgnoreCase("dia2_s2") && !text.contains("[S2]")) {
                    formattedText = "[S2] " + text;
                }
            }
            cmd.add(formattedText);

            cmd.add("--out");
            cmd.add(tempOut.toAbsolutePath().toString());

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder errOut = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errOut.append(line).append("\n");
                }
            }

            boolean finished = p.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new TimeoutException("Dia2 synthesis timed out after 60 seconds");
            }

            int exit = p.exitValue();
            if (exit != 0 || !Files.exists(tempOut) || Files.size(tempOut) == 0) {
                throw new RuntimeException("Dia2 synthesis failed (exit code " + exit + "):\n" + errOut);
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
        return List.of(
                new FastTTSVoice("dia2_s1", "Dia2-Speaker1", "en-US", "Dialogue Speaker 1", "dia2"),
                new FastTTSVoice("dia2_s2", "Dia2-Speaker2", "en-US", "Dialogue Speaker 2", "dia2"),
                new FastTTSVoice("dia2_duet", "Dia2-Duet", "en-US", "Multi-Speaker Dialogue [S1/S2]", "dia2")
        );
    }
}