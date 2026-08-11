package fasttts.backends.huggingface;

import fasttts.core.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Consumer;

/**
 * Hugging Face Space TTS Backend.
 * Supports inference via Hugging Face Spaces HTTP API.
 */
public final class HuggingFaceBackend implements FastTTSBackend {

    private final String spaceUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public HuggingFaceBackend(String spaceUrl) {
        this.spaceUrl = spaceUrl;
    }

    @Override
    public String getName() {
        return "HuggingFace";
    }

    @Override
    public FastTTSAudio synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        try {
            // Try Gradio API first (most spaces use Gradio)
            String[] possibleEndpoints = {
                spaceUrl + "/run/text",
                spaceUrl + "/run/predict",
                spaceUrl + "/api/predict",
                spaceUrl + "/api/gradio/predict"
            };
            
            String requestBody = buildRequestBody(text, voice, config);
            
            for (String endpoint : possibleEndpoints) {
                try {
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                    HttpResponse<byte[]> response = httpClient.send(
                        request, 
                        HttpResponse.BodyHandlers.ofByteArray()
                    );

                    if (response.statusCode() == 200 && response.body().length > 0) {
                        byte[] audioData = response.body();
                        return new FastTTSAudio(audioData, 22050);
                    }
                } catch (Exception e) {
                    // Try next endpoint
                    continue;
                }
            }
            
            throw new RuntimeException("No Hugging Face API endpoint responded successfully");
        } catch (Exception e) {
            throw new RuntimeException("Hugging Face synthesis failed", e);
        }
    }

    @Override
    public void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception {
        // Streaming not implemented for Hugging Face Spaces
        chunkConsumer.accept(synthesize(text, voice, config).getData());
    }

    @Override
    public List<FastTTSVoice> getVoices() {
        // Spaces typically don't expose voice lists via API
        return List.of(new FastTTSVoice("default", "Default Voice", null));
    }

    private String buildRequestBody(String text, FastTTSVoice voice, FastTTSConfig config) {
        // Generic JSON body - may need adjustment for specific spaces
        return String.format(
            "{\"data\":[\"%s\"]}",
            text.replace("\"", "\\\"")
        );
    }
}