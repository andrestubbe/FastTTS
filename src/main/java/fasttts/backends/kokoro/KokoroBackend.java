package fasttts.backends.kokoro;

import ai.onnxruntime.*;
import fasttts.core.*;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Native Kokoro TTS Backend using ONNX Runtime Java API.
 */
public final class KokoroBackend implements FastTTSBackend {

    private final OrtEnvironment env;
    private final OrtSession session;
    private final float[] defaultStyle = new float[256];
    private final Map<String, float[]> voiceStyles = new java.util.HashMap<>();

    public KokoroBackend(String modelPath) throws OrtException, java.io.IOException {
        this.env = OrtEnvironment.getEnvironment();
        this.session = env.createSession(modelPath, new OrtSession.SessionOptions());
        
        // Initialize fallback style (all zeros is often safer/neutral)
        for (int i = 0; i < 256; i++) defaultStyle[i] = 0.0f;

        // Load voices.bin from same directory
        java.io.File voicesFile = new java.io.File(new java.io.File(modelPath).getParent(), "voices.bin");
        if (voicesFile.exists()) {
            loadVoices(voicesFile);
        }
    }

    private void loadVoices(java.io.File file) {
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new java.io.FileInputStream(file))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".npy")) {
                    String voiceId = entry.getName().replace(".npy", "");
                    byte[] data = zis.readAllBytes();
                    
                    // Skip NumPy header (usually starts with \x93NUMPY and has a fixed-length size field)
                    // For Kokoro v0.19, the data usually starts at offset 80-128.
                    if (data.length > 10 && data[0] == (byte)0x93 && data[1] == 'N') {
                        int headerLen = (data[8] & 0xFF) | ((data[9] & 0xFF) << 8);
                        int offset = 10 + headerLen;
                        
                        // The shape is (511, 1, 256). Let's average them for a stable voice.
                        int vectors = 511;
                        float[] meanStyle = new float[256];
                        java.nio.FloatBuffer fb = java.nio.ByteBuffer.wrap(data, offset, data.length - offset)
                            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
                            .asFloatBuffer();
                        
                        for (int v = 0; v < vectors && fb.remaining() >= 256; v++) {
                            for (int i = 0; i < 256; i++) {
                                meanStyle[i] += fb.get();
                            }
                        }
                        for (int i = 0; i < 256; i++) meanStyle[i] /= vectors;

                        voiceStyles.put(voiceId.equals("af_bella") || voiceId.equals("af") ? "af" : voiceId, meanStyle);
                        voiceStyles.put(voiceId, meanStyle);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[WARN] Failed to load Kokoro voices from ZIP: " + e.getMessage());
        }
    }

    @Override
    public FastTTSAudio synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        long[] tokens = KokoroPhonemes.tokenize(text);
        
        long[] shape = {1, tokens.length};
        OnnxTensor tokenTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(tokens), shape);
        
        float[] style = voiceStyles.getOrDefault(voice != null ? voice.id() : "af", defaultStyle);
        long[] styleShape = {1, 256};
        OnnxTensor styleTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(style), styleShape);
        
        float speed = config != null ? config.getRate() : 1.0f;
        long[] speedShape = {1};
        OnnxTensor speedTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(new float[]{speed}), speedShape);

        Map<String, OnnxTensor> inputs = Map.of(
            "tokens", tokenTensor,
            "style", styleTensor,
            "speed", speedTensor
        );

        try (OrtSession.Result results = session.run(inputs)) {
            Object value = results.get(0).getValue();
            float[] audioData;
            
            if (value instanceof float[][][]) {
                float[][][] audioOutput = (float[][][]) value;
                audioData = audioOutput[0][0]; 
            } else if (value instanceof float[][]) {
                float[][] audioOutput = (float[][]) value;
                audioData = audioOutput[0];
            } else if (value instanceof float[]) {
                audioData = (float[]) value;
            } else {
                String type = value != null ? value.getClass().getName() : "null";
                throw new Exception("Unexpected output shape from Kokoro model. Type: " + type);
            }
            byte[] pcm = new byte[audioData.length * 2];
            for (int i = 0; i < audioData.length; i++) {
                short val = (short) (Math.max(-1.0f, Math.min(1.0f, audioData[i])) * 32767);
                pcm[i * 2] = (byte) (val & 0xff);
                pcm[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
            }
            
            // Try 22050 or 24000
            return new FastTTSAudio(pcm, 24000); 
        } finally {
            tokenTensor.close();
            styleTensor.close();
            speedTensor.close();
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
            new FastTTSVoice("af", "Bella (US Female)", "en_US", "female", "kokoro"),
            new FastTTSVoice("af_sky", "Sky (US Female)", "en_US", "female", "kokoro"),
            new FastTTSVoice("am_adam", "Adam (US Male)", "en_US", "male", "kokoro"),
            new FastTTSVoice("am_michael", "Michael (US Male)", "en_US", "male", "kokoro"),
            new FastTTSVoice("bf_isabella", "Isabella (UK Female)", "en_UK", "female", "kokoro"),
            new FastTTSVoice("bm_george", "George (UK Male)", "en_UK", "male", "kokoro")
        );
    }

    @Override
    public String getName() {
        return "Kokoro";
    }

    public void close() throws OrtException {
        session.close();
    }
}
