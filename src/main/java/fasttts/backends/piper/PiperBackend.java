package fasttts.backends.piper;

import fasttts.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Piper TTS Backend (CLI-based).
 * Fast, offline, and reliable.
 */
public final class PiperBackend implements FastTTSBackend {

    private String piperPath = "piper.exe";
    private String modelPath = "model.onnx";

    public PiperBackend() {
        this(resolvePiperBinary(), "model.onnx");
    }

    public PiperBackend(String piperPath, String modelPath) {
        this.piperPath = piperPath != null ? piperPath : resolvePiperBinary();
        this.modelPath = modelPath;
    }

    private static String resolvePiperBinary() {
        // 1. Current working dir
        if (new File("piper.exe").exists()) {
            return new File("piper.exe").getAbsolutePath();
        }
        // 2. Relative bin/piper/piper.exe
        if (new File("bin/piper/piper.exe").exists()) {
            return new File("bin/piper/piper.exe").getAbsolutePath();
        }
        // 3. ~/.speak/bin/piper/piper.exe
        String userHome = System.getProperty("user.home");
        File homePiper = new File(userHome, ".speak/bin/piper/piper.exe");
        if (homePiper.exists()) {
            return homePiper.getAbsolutePath();
        }
        // 4. ~/.speak/bin/piper.exe
        File homePiperDirect = new File(userHome, ".speak/bin/piper.exe");
        if (homePiperDirect.exists()) {
            return homePiperDirect.getAbsolutePath();
        }
        // 5. Default fallback
        return "bin/piper/piper.exe";
    }

    private static File resolveModelFile(String model) {
        if (model == null) model = "models/piper/de_DE-thorsten-medium.onnx";
        File direct = new File(model);
        if (direct.exists()) return direct.getAbsoluteFile();

        // 1. Check models/piper/<model>
        File inModelsPiper = new File("models/piper", model);
        if (inModelsPiper.exists()) return inModelsPiper.getAbsoluteFile();

        // 2. Check models/<model>
        File inModels = new File("models", model);
        if (inModels.exists()) return inModels.getAbsoluteFile();

        // 3. Basename only in models/piper
        File baseNameInPiper = new File("models/piper", direct.getName());
        if (baseNameInPiper.exists()) return baseNameInPiper.getAbsoluteFile();

        // 4. Basename only in models
        File baseNameInModels = new File("models", direct.getName());
        if (baseNameInModels.exists()) return baseNameInModels.getAbsoluteFile();

        return direct.getAbsoluteFile();
    }

    @Override
    public FastTTSAudio synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        if (!new File(piperPath).exists()) {
            // Re-check dynamic paths in case it was installed after startup
            String resolved = resolvePiperBinary();
            if (new File(resolved).exists()) {
                this.piperPath = resolved;
            } else {
                throw new FileNotFoundException("piper.exe not found at [" + new File(piperPath).getAbsolutePath() + "]. Please ensure Piper is installed in ~/.speak/bin/piper/piper.exe or in current dir.");
            }
        }

        String currentModel = (voice != null && voice.id() != null) ? voice.id() : modelPath;
        Path tempOutput = Files.createTempFile("piper_out", ".wav");
        try {
            File piperExe = new File(piperPath).getAbsoluteFile();
            File modelFile = resolveModelFile(currentModel);

            List<String> cmd = new ArrayList<>();
            cmd.add(piperExe.getAbsolutePath());
            cmd.add("--model");
            cmd.add(modelFile.getAbsolutePath());
            cmd.add("--output_file");
            cmd.add(tempOutput.toAbsolutePath().toString());

            File espeakData = new File(piperExe.getParentFile(), "espeak-ng-data");
            if (espeakData.exists()) {
                cmd.add("--espeak_data");
                cmd.add(espeakData.getAbsolutePath());
            }

            ProcessBuilder pb = new ProcessBuilder(cmd);
            if (piperExe.getParentFile() != null) {
                pb.directory(piperExe.getParentFile());
            }
            
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();
            
            try (OutputStream os = p.getOutputStream(); 
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {
                writer.write(text);
                writer.flush();
            }
            
            boolean finished = p.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new TimeoutException("Piper synthesis timed out after 60 seconds");
            }

            int exit = p.exitValue();
            if (exit != 0) {
                throw new RuntimeException("Piper execution failed (exit code " + exit + ").");
            }
            
            byte[] data = Files.readAllBytes(tempOutput);
            
            // Dynamically parse sample rate from .onnx.json config
            int sampleRate = 22050; // Fallback
            try {
                String jsonPath = currentModel + ".json";
                if (new File(jsonPath).exists()) {
                    String content = Files.readString(Paths.get(jsonPath));
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"sample_rate\"\\s*:\\s*(\\d+)").matcher(content);
                    if (m.find()) {
                        sampleRate = Integer.parseInt(m.group(1));
                    }
                }
            } catch (Throwable ignored) {}
            
            return new FastTTSAudio(data, sampleRate);
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
        List<FastTTSVoice> voices = new java.util.ArrayList<>();
        File[] searchDirs = {
                new File("models/piper"),
                new File("models"),
                new File(piperPath).getParentFile()
        };
        for (File dir : searchDirs) {
            if (dir != null && dir.exists()) {
                File[] models = dir.listFiles((d, name) -> name.endsWith(".onnx"));
                if (models != null) {
                    for (File m : models) {
                        String name = m.getName().replace(".onnx", "");
                        String locale = name.toLowerCase().startsWith("de") ? "de-DE" : "en-US";
                        voices.add(new FastTTSVoice(m.getAbsolutePath(), name, locale, "Neural", "piper"));
                    }
                }
            }
        }
        return voices;
    }

    @Override
    public String getName() {
        return "Piper";
    }
}
