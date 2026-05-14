package fasttts.core;

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
}
