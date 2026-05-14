package fasttts;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Azure Cognitive Services TTS Backend (Placeholder).
 * Ready for implementation when Azure credentials are available.
 */
public final class AzureBackend implements FastTTSBackend {

    private final String subscriptionKey;
    private final String region;

    public AzureBackend(String subscriptionKey, String region) {
        this.subscriptionKey = subscriptionKey;
        this.region = region;
    }

    @Override
    public byte[] synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        throw new UnsupportedOperationException("Azure Backend is currently a placeholder. Implement REST API call here.");
    }

    @Override
    public void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception {
        throw new UnsupportedOperationException("Azure Backend is currently a placeholder.");
    }

    @Override
    public List<FastTTSVoice> getVoices() {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "Azure";
    }
}
