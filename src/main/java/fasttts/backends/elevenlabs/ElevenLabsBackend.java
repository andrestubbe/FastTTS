package fasttts.backends.elevenlabs;

import fasttts.core.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Consumer;

/**
 * ElevenLabs Cloud TTS Backend.
 */
public final class ElevenLabsBackend implements FastTTSBackend {

    private final String apiKey;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private String defaultVoiceId;
    private float defaultStability;
    private float defaultSimilarity;

    public ElevenLabsBackend(String apiKey) {
        this(apiKey, "21m00Tcm4TlvDq8ikWAM", 0.5f, 0.75f);
    }

    public ElevenLabsBackend(String apiKey, String defaultVoiceId, float defaultStability, float defaultSimilarity) {
        this.apiKey = apiKey;
        this.defaultVoiceId = defaultVoiceId;
        this.defaultStability = defaultStability;
        this.defaultSimilarity = defaultSimilarity;
    }

    public void setDefaultVoiceId(String id) { this.defaultVoiceId = id; }
    public void setStability(float stability) { this.defaultStability = stability; }
    public void setSimilarity(float similarity) { this.defaultSimilarity = similarity; }

    @Override
    public byte[] synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        String voiceId = (voice != null) ? voice.id() : defaultVoiceId;
        
        // Settings from config or defaults
        float stability = Float.parseFloat(config.getProperty("elevenlabs.stability", String.valueOf(defaultStability)));
        float similarity = Float.parseFloat(config.getProperty("elevenlabs.similarity", String.valueOf(defaultSimilarity)));

        String json = String.format(java.util.Locale.US,
            "{\"text\":\"%s\", \"model_id\":\"eleven_turbo_v2_5\", \"voice_settings\":{\"stability\":%f, \"similarity_boost\":%f}}",
            text.replace("\"", "\\\""), stability, similarity
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.elevenlabs.io/v1/text-to-speech/" + voiceId + "?output_format=pcm_44100"))
            .header("Content-Type", "application/json")
            .header("xi-api-key", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("ElevenLabs Error " + response.statusCode() + ": " + new String(response.body()));
        }

        return response.body();
    }

    @Override
    public void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception {
        // Simple version: just synthesize and return one big chunk
        chunkConsumer.accept(synthesize(text, voice, config));
    }

    @Override
    public List<FastTTSVoice> getVoices() {
        return List.of(
            new FastTTSVoice("pNInz6obpgDQGcFmaJgB", "Adam (Multilingual)", "en/de", "male", "elevenlabs"),
            new FastTTSVoice("hpp4J3VqNfWAUOO0d1Us", "Bella (Multilingual)", "en/de", "female", "elevenlabs"),
            new FastTTSVoice("IKne3meq5aSn9XLyUdCD", "Charlie (Multilingual)", "en/de", "male", "elevenlabs"),
            new FastTTSVoice("Xb7hH8MSUJpSbSDYk0k2", "Alice (Multilingual)", "en/de", "female", "elevenlabs")
        );
    }

    @Override
    public String getName() {
        return "ElevenLabs";
    }
}
