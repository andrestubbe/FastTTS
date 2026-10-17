package fasttts.backends.huggingface;

import fasttts.core.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Hugging Face Spaces & Inference API TTS Backend.
 * Supports direct Spaces Gradio endpoints as well as Serverless Inference API models.
 */
public final class HuggingFaceBackend implements FastTTSBackend {

    private final String spaceUrlOrModelId;
    private final String token;
    private final HttpClient httpClient;
    private volatile boolean prepared = false;

    public HuggingFaceBackend() {
        this(resolveDefaultEndpoint(), resolveToken());
    }

    public HuggingFaceBackend(String spaceUrlOrModelId) {
        this(spaceUrlOrModelId, resolveToken());
    }

    public HuggingFaceBackend(String spaceUrlOrModelId, String token) {
        this.spaceUrlOrModelId = (spaceUrlOrModelId != null && !spaceUrlOrModelId.isBlank())
                ? spaceUrlOrModelId
                : resolveDefaultEndpoint();
        this.token = token != null ? token : resolveToken();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    private static String resolveDefaultEndpoint() {
        String env = System.getenv("HF_SPACE_URL");
        if (env != null && !env.isBlank()) return env;
        env = System.getenv("HUGGINGFACE_SPACE_URL");
        if (env != null && !env.isBlank()) return env;
        return "facebook/mms-tts-eng";
    }

    private static String resolveToken() {
        String t = System.getenv("HF_TOKEN");
        if (t != null && !t.isBlank()) return t;
        t = System.getenv("HUGGINGFACE_API_KEY");
        if (t != null && !t.isBlank()) return t;
        return null;
    }

    @Override
    public String getName() {
        return "HuggingFace";
    }

    @Override
    public boolean isAvailable() {
        return spaceUrlOrModelId != null && !spaceUrlOrModelId.isBlank();
    }

    @Override
    public synchronized void prepare() {
        prepared = true;
    }

    @Override
    public boolean isPrepared() {
        return prepared;
    }

    @Override
    public FastTTSAudio synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        String target = (voice != null && voice.id() != null && !voice.id().isBlank() && !voice.id().equals("default"))
                ? voice.id()
                : spaceUrlOrModelId;

        String effToken = token;
        if (config != null) {
            String cfgToken = config.getProperty("huggingface.token", null);
            if (cfgToken != null && !cfgToken.isBlank()) effToken = cfgToken;
        }

        if (target.startsWith("http://") || target.startsWith("https://")) {
            return synthesizeSpace(target, text, voice, config, effToken);
        } else {
            return synthesizeInferenceApi(target, text, config, effToken);
        }
    }

    private FastTTSAudio synthesizeInferenceApi(String modelId, String text, FastTTSConfig config, String effToken) throws Exception {
        String[] baseUrls = {
                "https://router.huggingface.co/hf-inference/models/" + modelId,
                "https://api-inference.huggingface.co/models/" + modelId
        };

        String jsonBody = String.format("{\"inputs\": \"%s\"}", escapeJson(text));

        Exception lastEx = null;
        for (String url : baseUrls) {
            try {
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

                if (effToken != null && !effToken.isBlank()) {
                    reqBuilder.header("Authorization", "Bearer " + effToken);
                }

                HttpResponse<byte[]> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200 && response.body() != null && response.body().length > 0) {
                    return new FastTTSAudio(response.body(), 24000);
                } else if (response.statusCode() == 401) {
                    lastEx = new IllegalStateException("HF_TOKEN missing or unauthorized (HTTP 401)");
                } else if (response.statusCode() == 503) {
                    // Model loading on HF side
                    lastEx = new RuntimeException("Model " + modelId + " is currently loading on Hugging Face (503): "
                            + new String(response.body()));
                } else {
                    lastEx = new RuntimeException("Hugging Face API returned HTTP " + response.statusCode());
                }
            } catch (Exception e) {
                lastEx = e;
            }
        }

        throw (lastEx != null ? lastEx : new RuntimeException("Failed to synthesize via Hugging Face Inference API"));
    }

    private FastTTSAudio synthesizeSpace(String spaceUrl, String text, FastTTSVoice voice, FastTTSConfig config, String effToken) throws Exception {
        String cleanUrl = spaceUrl.endsWith("/") ? spaceUrl.substring(0, spaceUrl.length() - 1) : spaceUrl;

        String[] possibleEndpoints = {
                cleanUrl + "/api/predict",
                cleanUrl + "/run/predict",
                cleanUrl + "/api/gradio/predict",
                cleanUrl + "/run/text"
        };

        String requestBody = String.format("{\"data\":[\"%s\"]}", escapeJson(text));

        Exception lastEx = null;
        for (String endpoint : possibleEndpoints) {
            try {
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody));

                if (effToken != null && !effToken.isBlank()) {
                    reqBuilder.header("Authorization", "Bearer " + effToken);
                }

                HttpResponse<byte[]> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200 && response.body().length > 0) {
                    return new FastTTSAudio(response.body(), 24000);
                }
            } catch (Exception e) {
                lastEx = e;
            }
        }

        throw new RuntimeException("No Hugging Face Space endpoint responded successfully at " + cleanUrl, lastEx);
    }

    private static String escapeJson(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    @Override
    public void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception {
        chunkConsumer.accept(synthesize(text, voice, config).getData());
    }

    @Override
    public List<FastTTSVoice> getVoices() {
        return List.of(
                new FastTTSVoice("facebook/mms-tts-eng", "HF-MMS-English", "en-US", "Meta MMS English", "huggingface"),
                new FastTTSVoice("facebook/mms-tts-deu", "HF-MMS-German", "de-DE", "Meta MMS German", "huggingface"),
                new FastTTSVoice("espnet/kan-bayashi_ljspeech_vits", "HF-LJSpeech-VITS", "en-US", "VITS Neural", "huggingface"),
                new FastTTSVoice("default", "HF-Default", "en-US", "Hugging Face Space / Inference", "huggingface")
        );
    }
}