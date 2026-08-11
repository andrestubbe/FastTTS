package fasttts.backends.openai;

import fasttts.core.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Consumer;

/**
 * OpenAI TTS Backend.
 * Uses OpenAI's text-to-speech API (tts-1, tts-1-hd models).
 */
public final class OpenAIBackend implements FastTTSBackend {

    private final String apiKey;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private String defaultModel = "tts-1";
    private String defaultVoice = "alloy";

    public OpenAIBackend(String apiKey) {
        this.apiKey = apiKey;
    }

    public OpenAIBackend(String apiKey, String defaultModel, String defaultVoice) {
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.defaultVoice = defaultVoice;
    }

    public void setDefaultModel(String model) {
        this.defaultModel = model;
    }

    public void setDefaultVoice(String voice) {
        this.defaultVoice = voice;
    }

    @Override
    public String getName() {
        return "OpenAI";
    }

    @Override
    public FastTTSAudio synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        String model = defaultModel;
        String voiceId = defaultVoice;
        
        if (voice != null) {
            voiceId = voice.id();
        }
        
        String requestBody = String.format(
            "{\"model\":\"%s\",\"input\":\"%s\",\"voice\":\"%s\"}",
            model,
            text.replace("\"", "\\\"").replace("\n", " "),
            voiceId
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/audio/speech"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<byte[]> response = httpClient.send(
            request, 
            HttpResponse.BodyHandlers.ofByteArray()
        );

        if (response.statusCode() == 200) {
            byte[] audioData = response.body();
            return new FastTTSAudio(audioData, 24000); // OpenAI TTS uses 24kHz
        } else {
            throw new RuntimeException("OpenAI API error: " + response.statusCode() + " - " + new String(response.body()));
        }
    }

    @Override
    public void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception {
        chunkConsumer.accept(synthesize(text, voice, config).getData());
    }

    @Override
    public List<FastTTSVoice> getVoices() {
        // OpenAI TTS available voices
        return List.of(
            new FastTTSVoice("alloy", "Alloy", "en-US", null, "openai"),
            new FastTTSVoice("echo", "Echo", "en-US", null, "openai"),
            new FastTTSVoice("fable", "Fable", "en-US", null, "openai"),
            new FastTTSVoice("onyx", "Onyx", "en-US", null, "openai"),
            new FastTTSVoice("nova", "Nova", "en-US", null, "openai"),
            new FastTTSVoice("shimmer", "Shimmer", "en-US", null, "openai")
        );
    }
}