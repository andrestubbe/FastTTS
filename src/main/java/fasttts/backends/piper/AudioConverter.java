package fasttts.backends.piper;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Audio converter for float32 PCM to WAV format.
 */
public class AudioConverter {
    
    /**
     * Convert float32 PCM samples to WAV format byte array.
     * 
     * @param samples float32 PCM samples (-1.0 to 1.0)
     * @param sampleRate sample rate in Hz
     * @return WAV format byte array
     */
    public static byte[] floatToWav(float[] samples, int sampleRate) {
        // Convert float32 to int16 PCM
        byte[] pcmData = floatToInt16(samples);
        
        // Create WAV header
        byte[] wavHeader = createWavHeader(pcmData.length, sampleRate);
        
        // Combine header and data
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(wavHeader, 0, wavHeader.length);
        outputStream.write(pcmData, 0, pcmData.length);
        
        return outputStream.toByteArray();
    }
    
    /**
     * Convert float32 samples to int16 PCM bytes.
     */
    private static byte[] floatToInt16(float[] samples) {
        byte[] pcmData = new byte[samples.length * 2];
        ByteBuffer buffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN);
        
        for (float sample : samples) {
            // Clamp to [-1.0, 1.0]
            float clamped = Math.max(-1.0f, Math.min(1.0f, sample));
            // Convert to int16 range [-32768, 32767]
            short int16 = (short) (clamped * 32767.0f);
            buffer.putShort(int16);
        }
        
        return pcmData;
    }
    
    /**
     * Create WAV header for PCM data.
     */
    private static byte[] createWavHeader(int pcmDataSize, int sampleRate) {
        byte[] header = new byte[44];
        ByteBuffer bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        
        int fileSize = pcmDataSize + 36;
        int byteRate = sampleRate * 2; // 16-bit mono
        
        // RIFF header
        bb.put("RIFF".getBytes());
        bb.putInt(fileSize);
        bb.put("WAVE".getBytes());
        
        // fmt chunk
        bb.put("fmt ".getBytes());
        bb.putInt(16); // Subchunk1Size (16 for PCM)
        bb.putShort((short) 1); // AudioFormat (1 = PCM)
        bb.putShort((short) 1); // NumChannels (1 = mono)
        bb.putInt(sampleRate); // SampleRate
        bb.putInt(byteRate); // ByteRate
        bb.putShort((short) 2); // BlockAlign (2 = 16-bit mono)
        bb.putShort((short) 16); // BitsPerSample
        
        // data chunk
        bb.put("data".getBytes());
        bb.putInt(pcmDataSize); // Subchunk2Size
        
        return header;
    }
}
