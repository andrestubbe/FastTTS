package fasttts;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * ElevenLabs Cloud TTS Backend.
 * High-quality neural voices via API.
 */
public final class ElevenLabsBackend implements FastTTSBackend {

    private final String apiKey;
    private final String voiceId;
    private final HttpClient httpClient;

    public ElevenLabsBackend(String apiKey) {
        this(apiKey, "pNInz6obpg8n9Y9o8GuS"); // Default voice: Rachel
    }

    public ElevenLabsBackend(String apiKey, String voiceId) {
        this.apiKey = apiKey;
        this.voiceId = voiceId;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public byte[] synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        String url = "https://api.elevenlabs.io/v1/text-to-speech/" + (voice != null ? voice.id() : voiceId);
        
        String json = "{\"text\":\"" + text + "\",\"model_id\":\"eleven_monolingual_v1\"}";
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("xi-api-key", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
            
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("ElevenLabs API Error: " + response.statusCode() + " - " + new String(response.body()));
        }
        
        return response.body();
    }

    @Override
    public void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception {
        // ElevenLabs supports WebSockets for true streaming, 
        // but for this minimal version we provide the buffer at once.
        chunkConsumer.accept(synthesize(text, voice, config));
    }

    @Override
    public List<FastTTSVoice> getVoices() {
        return Collections.emptyList(); // Fetching voices requires an extra API call
    }

    @Override
    public String getName() {
        return "ElevenLabs";
    }
}
