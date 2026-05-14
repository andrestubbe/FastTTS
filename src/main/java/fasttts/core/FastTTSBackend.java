package fasttts.core;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interface for all TTS engines (Windows, Piper, ElevenLabs, etc.).
 */
public interface FastTTSBackend {
    byte[] synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception;
    void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception;
    List<FastTTSVoice> getVoices();
    String getName();
}
