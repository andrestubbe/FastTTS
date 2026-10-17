package fasttts.backends.omnivoice;

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
 * OmniVoice TTS Backend supporting 600+ languages and Zero-Shot Voice Cloning.
 * Runs in isolated environment (bin/omnivoice) via diffusion language model.
 */
public final class OmniVoiceBackend implements FastTTSBackend {

    private String pythonPath;
    private String runnerScript;
    private String defaultReferenceAudio;
    private volatile boolean prepared = false;

    public OmniVoiceBackend() {
        this(resolvePythonPath(), resolveRunnerScript(), resolveReferenceAudio());
    }

    public OmniVoiceBackend(String pythonPath, String runnerScript, String defaultReferenceAudio) {
        this.pythonPath = pythonPath != null ? pythonPath : resolvePythonPath();
        this.runnerScript = runnerScript != null ? runnerScript : resolveRunnerScript();
        this.defaultReferenceAudio = defaultReferenceAudio != null ? defaultReferenceAudio : resolveReferenceAudio();
    }

    private static String resolvePythonPath() {
        String[] candidates = new String[]{
                "bin/omnivoice/venv/Scripts/python.exe",
                "../2026-08-17-Work-FastJava/FastTTS/bin/omnivoice/venv/Scripts/python.exe",
                System.getProperty("user.home", ".") + "/Documents/2026-08-17-Work-FastJava/FastTTS/bin/omnivoice/venv/Scripts/python.exe"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        return "bin/omnivoice/venv/Scripts/python.exe";
    }

    private static String resolveRunnerScript() {
        String[] candidates = new String[]{
                "bin/omnivoice/run_omnivoice.py",
                "../2026-08-17-Work-FastJava/FastTTS/bin/omnivoice/run_omnivoice.py",
                System.getProperty("user.home", ".") + "/Documents/2026-08-17-Work-FastJava/FastTTS/bin/omnivoice/run_omnivoice.py"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        return "bin/omnivoice/run_omnivoice.py";
    }

    private static String resolveReferenceAudio() {
        String[] candidates = new String[]{
                "models/omnivoice/sample_reference.wav",
                "../2026-08-17-Work-FastJava/FastTTS/models/omnivoice/sample_reference.wav",
                System.getProperty("user.home", ".") + "/Documents/2026-08-17-Work-FastJava/FastTTS/models/omnivoice/sample_reference.wav"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        return "models/omnivoice/sample_reference.wav";
    }

    @Override
    public synchronized void prepare() {
        if (prepared) return;
        if (!isAvailable()) return;
        try {
            // Prepare / warm up the model
            ProcessBuilder pb = new ProcessBuilder(pythonPath, runnerScript, "--prepare");
            File py = new File(pythonPath);
            if (py.getParentFile() != null) pb.directory(py.getParentFile());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                while (reader.readLine() != null) {}
            }
            boolean finished = p.waitFor(120, TimeUnit.SECONDS);
            if (finished && p.exitValue() == 0) {
                prepared = true;
            } else {
                p.destroyForcibly();
            }
        } catch (Exception ignored) {}
    }

    @Override
    public boolean isPrepared() {
        return prepared;
    }

    @Override
    public boolean isAvailable() {
        return new File(pythonPath).exists() && new File(runnerScript).exists();
    }

    @Override
    public FastTTSAudio synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        if (!isAvailable()) {
            throw new FileNotFoundException("OmniVoice environment not found at: " + pythonPath);
        }

        Path tempOutput = Files.createTempFile("omnivoice_out", ".wav");
        try {
            String refAudio = null;
            if (voice != null && voice.id() != null && new File(voice.id()).exists()) {
                refAudio = voice.id();
            }

            int steps = 16; // Balanced default for CPU benchmark (can be overridden via config)
            if (config != null) {
                String stepsProp = config.getProperty("omnivoice.steps", null);
                if (stepsProp != null) {
                    try { steps = Integer.parseInt(stepsProp); } catch (NumberFormatException ignored) {}
                }
            }

            List<String> cmd = new ArrayList<>();
            cmd.add(new File(pythonPath).getAbsolutePath());
            cmd.add(new File(runnerScript).getAbsolutePath());
            cmd.add("--text");
            cmd.add(text);
            if (refAudio != null && new File(refAudio).exists()) {
                cmd.add("--reference");
                cmd.add(new File(refAudio).getAbsolutePath());
            }
            cmd.add("--steps");
            cmd.add(String.valueOf(steps));
            cmd.add("--output");
            cmd.add(tempOutput.toAbsolutePath().toString());

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);

            Process p = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = p.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new TimeoutException("OmniVoice synthesis timed out after 300 seconds");
            }

            int exit = p.exitValue();
            if (exit != 0 || !Files.exists(tempOutput) || Files.size(tempOutput) == 0) {
                throw new RuntimeException("OmniVoice synthesis failed (code " + exit + "):\n" + output);
            }

            byte[] wavData = Files.readAllBytes(tempOutput);
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
        return OmniVoiceVoice.all();
    }

    @Override
    public String getName() {
        return "OmniVoice";
    }
}
