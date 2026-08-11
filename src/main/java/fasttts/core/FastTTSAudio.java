package fasttts.core;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Container for synthesized audio data and its format metadata.
 */
public final class FastTTSAudio {
    private final byte[] data;
    private final int sampleRate;
    private final int bitsPerSample;
    private final int channels;

    public FastTTSAudio(byte[] data, int sampleRate) {
        this(data, sampleRate, 16, 1);
    }

    public FastTTSAudio(byte[] data, int sampleRate, int bitsPerSample, int channels) {
        this.data = data;
        this.sampleRate = sampleRate;
        this.bitsPerSample = bitsPerSample;
        this.channels = channels;
    }

    public byte[] getData() { return data; }
    public int getSampleRate() { return sampleRate; }
    public int getBitsPerSample() { return bitsPerSample; }
    public int getChannels() { return channels; }
    public int getLength() { return data.length; }
    
    /**
     * Play the audio using Java Sound API.
     */
    public void play() throws IOException, LineUnavailableException {
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
            AudioFormat format = audioStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            
            Clip clip = (Clip) AudioSystem.getLine(info);
            clip.open(audioStream);
            clip.start();
            
            // Wait for playback to complete
            while (!clip.isRunning()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            while (clip.isRunning()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            clip.close();
            audioStream.close();
        } catch (UnsupportedAudioFileException e) {
            throw new IOException("Unsupported audio format", e);
        }
    }
}
