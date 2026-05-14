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

    public PiperBackend() {}

    public PiperBackend(String piperPath, String modelPath) {
        this.piperPath = piperPath;
        this.modelPath = modelPath;
    }

    @Override
    public FastTTSAudio synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        if (!new File(piperPath).exists()) {
            throw new FileNotFoundException("piper.exe not found at: " + piperPath);
        }

        String currentModel = (voice != null && voice.id() != null) ? voice.id() : modelPath;
        Path tempOutput = Files.createTempFile("piper_out", ".wav");
        
        ProcessBuilder pb = new ProcessBuilder(
            piperPath,
            "--model", currentModel,
            "--output_file", tempOutput.toAbsolutePath().toString()
        );
        
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
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
        
        // Piper outputs WAV by default, but we treat it as 22050 if raw. 
        // Actually Piper WAVs are usually 22050Hz.
        return new FastTTSAudio(data, 22050);
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
