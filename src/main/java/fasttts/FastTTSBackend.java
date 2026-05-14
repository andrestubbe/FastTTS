package fasttts;

import java.util.List;
import java.util.function.Consumer;

public interface FastTTSBackend {
    byte[] synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception;
    void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception;
    List<FastTTSVoice> getVoices();
    String getName();
}
