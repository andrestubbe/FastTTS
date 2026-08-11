package fasttts.backends.piper;

import fasttts.core.*;
import fastaimodel.FastAIOnnxModel;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtSession;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

/**
 * Piper TTS Backend (ONNX-based).
 * Direct ONNX inference using FastAIModel for maximum performance.
 */
public final class PiperONNXBackend implements FastTTSBackend {

    private String modelPath = "model.onnx";
    private String espeakPath = "espeak-ng.exe";
    private String espeakVoice = "de";
    private FastAIOnnxModel onnxModel;
    private PiperConfig config;
    
    // Default synthesis parameters
    private float noiseScale = 0.667f;
    private float lengthScale = 1.0f;
    private float noiseW = 0.8f;
    
    public PiperONNXBackend() {}
    
    public PiperONNXBackend(String modelPath, String espeakPath) {
        this.modelPath = modelPath;
        this.espeakPath = espeakPath;
    }
    
    /**
     * Initialize the ONNX model and config.
     */
    public void initialize() throws Exception {
        // Load ONNX model
        onnxModel = new FastAIOnnxModel(modelPath);
        
        // Load config
        String configPath = modelPath + ".json";
        if (!new File(configPath).exists()) {
            throw new FileNotFoundException("Config file not found: " + configPath);
        }
        config = new PiperConfig(configPath);
    }
    
    @Override
    public FastTTSAudio synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        if (onnxModel == null) {
            initialize();
        }
        
        // Step 1: Text to phonemes using eSpeak-NG
        EspeakPhonemizer phonemizer = new EspeakPhonemizer(espeakPath, espeakVoice);
        String ipaPhonemes = phonemizer.textToPhonemes(text);
        System.out.println("DEBUG: IPA Phonemes: '" + ipaPhonemes + "'");
        
        // Step 2: Phonemes to IDs
        long[] phonemeIds = this.config.phonemesToIds(ipaPhonemes);
        System.out.println("DEBUG: Phoneme IDs count: " + phonemeIds.length);
        System.out.println("DEBUG: First few IDs: " + java.util.Arrays.toString(java.util.Arrays.copyOf(phonemeIds, Math.min(10, phonemeIds.length))));
        
        // Step 3: Prepare input tensors
        Map<String, OnnxTensor> inputs = prepareInputs(phonemeIds);
        
        // Step 4: Run ONNX inference
        OrtSession.Result result = onnxModel.run(inputs);
        
        // Step 5: Extract audio samples
        float[] audioSamples = extractAudioSamples(result);
        System.out.println("DEBUG: Audio samples count: " + audioSamples.length);
        System.out.println("DEBUG: Audio duration: " + (audioSamples.length / (float)this.config.getSampleRate()) + " seconds");
        
        // Step 6: Convert to WAV
        byte[] wavData = AudioConverter.floatToWav(audioSamples, this.config.getSampleRate());
        System.out.println("DEBUG: WAV size: " + wavData.length + " bytes");
        
        return new FastTTSAudio(wavData, this.config.getSampleRate());
    }
    
    @Override
    public void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception {
        FastTTSAudio audio = synthesize(text, voice, config);
        chunkConsumer.accept(audio.getData());
    }
    
    @Override
    public List<FastTTSVoice> getVoices() {
        List<FastTTSVoice> voices = new ArrayList<>();
        File dir = new File(modelPath).getParentFile();
        if (dir == null) dir = new File(".");
        
        File[] models = dir.listFiles((d, name) -> name.endsWith(".onnx"));
        if (models != null) {
            for (File m : models) {
                String name = m.getName().replace(".onnx", "");
                voices.add(new FastTTSVoice(m.getAbsolutePath(), name, "unknown", "unknown", "piper-onnx"));
            }
        }
        return voices;
    }
    
    @Override
    public String getName() {
        return "PiperONNX";
    }
    
    /**
     * Prepare input tensors for ONNX model.
     */
    private Map<String, OnnxTensor> prepareInputs(long[] phonemeIds) throws Exception {
        Map<String, OnnxTensor> inputs = new HashMap<>();
        
        // Input: phoneme IDs [1, N] - reshape to 2D array
        long[][] inputShape = new long[1][phonemeIds.length];
        System.arraycopy(phonemeIds, 0, inputShape[0], 0, phonemeIds.length);
        OnnxTensor inputTensor = OnnxTensor.createTensor(
            onnxModel.getEnv(), 
            inputShape
        );
        inputs.put("input", inputTensor);
        
        // Input lengths: [1] - 1D array as expected by the model
        long[] inputLengths = {phonemeIds.length};
        OnnxTensor lengthsTensor = OnnxTensor.createTensor(
            onnxModel.getEnv(),
            inputLengths
        );
        inputs.put("input_lengths", lengthsTensor);
        
        // Scales: [3] - 1D array
        float[] scales = {noiseScale, lengthScale, noiseW};
        OnnxTensor scalesTensor = OnnxTensor.createTensor(
            onnxModel.getEnv(),
            scales
        );
        inputs.put("scales", scalesTensor);
        
        return inputs;
    }
    
    /**
     * Extract audio samples from ONNX result.
     */
    private float[] extractAudioSamples(OrtSession.Result result) throws Exception {
        // Try to get output tensor by index (most common approach)
        try {
            OnnxTensor outputTensor = (OnnxTensor) result.get(0);
            if (outputTensor != null) {
                Object value = outputTensor.getValue();
                
                // Handle multi-dimensional arrays and flatten them
                return flattenArray(value);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract audio samples from ONNX result", e);
        }
        
        throw new RuntimeException("No output tensors found in ONNX result");
    }
    
    /**
     * Recursively flatten multi-dimensional float arrays to 1D.
     */
    private float[] flattenArray(Object array) {
        if (array instanceof float[]) {
            return (float[]) array;
        } else if (array instanceof float[][]) {
            return flatten2D((float[][]) array);
        } else if (array instanceof float[][][]) {
            return flatten3D((float[][][]) array);
        } else if (array instanceof float[][][][]) {
            return flatten4D((float[][][][]) array);
        } else {
            throw new RuntimeException("Unexpected output tensor type: " + array.getClass().getName());
        }
    }
    
    private float[] flatten2D(float[][] array2d) {
        int totalLength = 0;
        for (float[] row : array2d) {
            totalLength += row.length;
        }
        float[] flattened = new float[totalLength];
        int offset = 0;
        for (float[] row : array2d) {
            System.arraycopy(row, 0, flattened, offset, row.length);
            offset += row.length;
        }
        return flattened;
    }
    
    private float[] flatten3D(float[][][] array3d) {
        int totalLength = 0;
        for (float[][] matrix : array3d) {
            for (float[] row : matrix) {
                totalLength += row.length;
            }
        }
        float[] flattened = new float[totalLength];
        int offset = 0;
        for (float[][] matrix : array3d) {
            for (float[] row : matrix) {
                System.arraycopy(row, 0, flattened, offset, row.length);
                offset += row.length;
            }
        }
        return flattened;
    }
    
    private float[] flatten4D(float[][][][] array4d) {
        int totalLength = 0;
        for (float[][][] tensor : array4d) {
            for (float[][] matrix : tensor) {
                for (float[] row : matrix) {
                    totalLength += row.length;
                }
            }
        }
        float[] flattened = new float[totalLength];
        int offset = 0;
        for (float[][][] tensor : array4d) {
            for (float[][] matrix : tensor) {
                for (float[] row : matrix) {
                    System.arraycopy(row, 0, flattened, offset, row.length);
                    offset += row.length;
                }
            }
        }
        return flattened;
    }
    
    /**
     * Cleanup resources.
     */
    public void close() {
        if (onnxModel != null) {
            onnxModel.close();
        }
    }
    
    // Setter methods for synthesis parameters
    public void setNoiseScale(float noiseScale) {
        this.noiseScale = noiseScale;
    }
    
    public void setLengthScale(float lengthScale) {
        this.lengthScale = lengthScale;
    }
    
    public void setNoiseW(float noiseW) {
        this.noiseW = noiseW;
    }
    
    public void setEspeakVoice(String voice) {
        this.espeakVoice = voice;
    }
}
