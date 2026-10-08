package fasttts.backends.piper;

import fasttts.core.*;
import java.io.*;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
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
        
        ProcessBuilder pb = new ProcessBuilder(
            piperPath,
            "--model", currentModel,
            "--output_file", tempOutput.toAbsolutePath().toString()
        );
        
        File piperExe = new File(piperPath).getAbsoluteFile();
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
        
        if (p.waitFor() != 0) {
            throw new RuntimeException("Piper execution failed.");
        }
        
        byte[] data = Files.readAllBytes(tempOutput);
        Files.deleteIfExists(tempOutput);
        
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
    }

    @Override
    public void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception {
        FastTTSAudio audio = synthesize(text, voice, config);
        chunkConsumer.accept(audio.getData());
    }

    @Override
    public List<FastTTSVoice> getVoices() {
        List<FastTTSVoice> voices = new java.util.ArrayList<>();
        File dir = new File(piperPath).getParentFile();
        if (dir == null) dir = new File(".");
        
        File[] models = dir.listFiles((d, name) -> name.endsWith(".onnx"));
        if (models != null) {
            for (File m : models) {
                String name = m.getName().replace(".onnx", "");
                voices.add(new FastTTSVoice(m.getAbsolutePath(), name, "unknown", "unknown", "piper"));
            }
        }
        return voices;
    }

    @Override
    public String getName() {
        return "Piper";
    }
}
