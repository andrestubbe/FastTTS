package fasttts.core;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interface for all TTS engines (Windows, Piper, ElevenLabs, etc.).
 */
public interface FastTTSBackend extends AutoCloseable {
    FastTTSAudio synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception;
    void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception;
    List<FastTTSVoice> getVoices();
    String getName();

    /**
     * Pre-warms the backend, loading models into memory or OS cache
     * so that subsequent synthesis requests execute with minimum latency.
     */
    default void prepare() {}

    /**
     * Checks if this backend has already been prepared/warmed up.
     */
    default boolean isPrepared() { return true; }

    /**
     * Checks if required binaries, models, or network connections are ready.
     */
    default boolean isAvailable() { return true; }

    /**
     * Releases any held native resources, background threads, or model handles.
     */
    default void close() {}
}
