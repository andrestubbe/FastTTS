package fasttts.backends.f5tts;

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
 * F5-TTS Backend powered by audio.cpp native runtime.
 * Non-autoregressive Flow Matching voice cloning & text-to-speech engine.
 */
public final class F5TTSBackend implements FastTTSBackend {

    private String runnerPath;
    private String modelPath;
    private String vocabPath;
    private volatile boolean prepared = false;

    public F5TTSBackend() {
        this(resolveRunnerPath(), resolveModelPath(), resolveVocabPath());
    }

    public F5TTSBackend(String runnerPath, String modelPath, String vocabPath) {
        this.runnerPath = runnerPath != null ? runnerPath : resolveRunnerPath();
        this.modelPath = modelPath != null ? modelPath : resolveModelPath();
        this.vocabPath = vocabPath != null ? vocabPath : resolveVocabPath();
    }

    private static String resolveRunnerPath() {
        String[] candidates = {
                "bin/dia2/audiocpp_cli.exe",
                "bin/audiocpp_cli.exe",
                "audiocpp_cli.exe"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        return "bin/dia2/audiocpp_cli.exe";
    }

    private static String resolveModelPath() {
        String[] candidates = {
                "models/f5tts/f5-tts-v1-base-f16.gguf",
                "models/f5tts/f5-tts-v1-base.gguf",
                "models/f5-tts-v1-base-f16.gguf"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.length() > 10_000_000L) return f.getAbsolutePath();
        }
        return "models/f5tts/f5-tts-v1-base-f16.gguf";
    }

    private static String resolveVocabPath() {
        String[] candidates = {
                "models/f5tts/vocab.txt",
                "models/vocab.txt"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        return "models/f5tts/vocab.txt";
    }

    @Override
    public String getName() {
        return "F5TTS";
    }

    @Override
    public boolean isAvailable() {
        File r = new File(runnerPath);
        File m = new File(modelPath);
        return r.exists() && m.exists() && m.length() > 10_000_000L;
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
            throw new FileNotFoundException("F5-TTS runner or model not found. Runner: "
                    + runnerPath + ", Model: " + modelPath);
        }

        Path tempOut = Files.createTempFile("f5tts_out", ".wav");
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(new File(runnerPath).getAbsolutePath());
            cmd.add("--task");
            cmd.add("tts");
            cmd.add("--family");
            cmd.add("f5_tts");
            cmd.add("--model");
            cmd.add(new File(modelPath).getAbsolutePath());
            cmd.add("--backend");
            cmd.add("cpu");
            cmd.add("--text");
            cmd.add(text);
            cmd.add("--out");
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

            boolean finished = p.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new TimeoutException("F5-TTS synthesis timed out after 60 seconds");
            }

            int exit = p.exitValue();
            if (exit != 0 || !Files.exists(tempOut) || Files.size(tempOut) == 0) {
                throw new RuntimeException("F5-TTS synthesis failed (exit code " + exit + "):\n" + out);
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
                new FastTTSVoice("f5-base", "F5-TTS Base", "en-US", "Flow Matching Voice Clone", "f5tts")
        );
    }
}
