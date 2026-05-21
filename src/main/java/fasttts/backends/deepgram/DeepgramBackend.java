package fasttts.backends.deepgram;

import fasttts.core.*;
import java.net.URI;
import java.net.http.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Deepgram Aura TTS Backend.
 */
public final class DeepgramBackend implements FastTTSBackend {

    private final String apiKey;
    private final HttpClient client = HttpClient.newHttpClient();

    public DeepgramBackend(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public FastTTSAudio synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        String model = voice != null ? voice.id() : "aura-asteria-en";
        String url = "https://api.deepgram.com/v1/speak?model=" + model + "&encoding=linear16&sample_rate=16000";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Token " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"text\":\"" + text + "\"}"))
            .build();
 
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        
        if (response.statusCode() != 200) {
            throw new Exception("Deepgram Error " + response.statusCode() + ": " + new String(response.body()));
        }
 
        return new FastTTSAudio(response.body(), 16000); 
    }

    @Override
    public void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception {
        FastTTSAudio audio = synthesize(text, voice, config);
        chunkConsumer.accept(audio.getData());
    }

    @Override
    public List<FastTTSVoice> getVoices() {
        return List.of(
            new FastTTSVoice("aura-asteria-en", "Asteria (English)", "en", "female", "deepgram"),
            new FastTTSVoice("aura-luna-en", "Luna (English)", "en", "female", "deepgram"),
            new FastTTSVoice("aura-stella-en", "Stella (English)", "en", "female", "deepgram")
        );
    }

    @Override
    public String getName() {
        return "Deepgram";
    }
}
