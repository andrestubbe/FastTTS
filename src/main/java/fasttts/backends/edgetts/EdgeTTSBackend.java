package fasttts.backends.edgetts;

import fasttts.core.*;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * High-performance, zero-dependency Edge TTS Backend.
 * Connects directly to Microsoft's ReadAloud WebSocket API to synthesize
 * hyper-realistic Azure Neural Voices for free without API keys.
 */
public final class EdgeTTSBackend implements FastTTSBackend {

    private static final String EDGE_WSS_URL =
            "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?trustedclienttoken=6A5AA1D4EAFF4E9FB37E23D68491D6F4";

    private static final String ORIGIN_HEADER = "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold";

    private final HttpClient httpClient;

    public EdgeTTSBackend() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
    }

    @Override
    public FastTTSAudio synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        String voiceName = resolveVoiceName(voice);
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String timestamp = getFormattedTimestamp();

        CompletableFuture<byte[]> future = new CompletableFuture<>();
        ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();

        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                // Audio packet in Edge TTS: 2 bytes header length + header text + raw audio data
                if (data.remaining() > 2) {
                    int headerLength = data.getShort();
                    if (data.remaining() >= headerLength) {
                        byte[] headerBytes = new byte[headerLength];
                        data.get(headerBytes);
                        // The remaining payload is raw audio (MP3)
                        byte[] audioBytes = new byte[data.remaining()];
                        data.get(audioBytes);
                        audioBuffer.write(audioBytes, 0, audioBytes.length);
                    }
                }
                webSocket.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                String msg = data.toString();
                if (msg.contains("turn.end")) {
                    future.complete(audioBuffer.toByteArray());
                }
                webSocket.request(1);
                return null;
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                future.completeExceptionally(error);
            }
        };

        WebSocket ws = httpClient.newWebSocketBuilder()
                .header("Origin", ORIGIN_HEADER)
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0")
                .connectTimeout(Duration.ofSeconds(6))
                .buildAsync(URI.create(EDGE_WSS_URL), listener)
                .get(8, TimeUnit.SECONDS);

        // 1. Send speech.config message
        String speechConfig = "X-Timestamp:" + timestamp + "\r\n" +
                "Content-Type:application/json; charset=utf-8\r\n" +
                "Path:speech.config\r\n\r\n" +
                "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"},\"outputFormat\":\"riff-24khz-16bit-mono-pcm\"}}}}";
        ws.sendText(speechConfig, true).get(5, TimeUnit.SECONDS);

        // 2. Build SSML request
        float rate = (config != null) ? config.getRate() : 1.0f;
        float pitch = (config != null) ? config.getPitch() : 1.0f;
        float volume = (config != null) ? config.getVolume() : 1.0f;

        String ratePercent = String.format(Locale.US, "%+d%%", Math.round((rate - 1.0f) * 100));
        String pitchPercent = String.format(Locale.US, "%+d%%", Math.round((pitch - 1.0f) * 100));
        String volPercent = String.format(Locale.US, "%+d%%", Math.round((volume - 1.0f) * 100));

        String ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>" +
                "<voice name='" + voiceName + "'>" +
                "<prosody pitch='" + pitchPercent + "' rate='" + ratePercent + "' volume='" + volPercent + "'>" +
                escapeXml(text) +
                "</prosody></voice></speak>";

        String ssmlMessage = "X-RequestId:" + requestId + "\r\n" +
                "Content-Type:application/ssml+xml\r\n" +
                "X-Timestamp:" + timestamp + "\r\n" +
                "Path:ssml\r\n\r\n" +
                ssml;

        ws.sendText(ssmlMessage, true).get(5, TimeUnit.SECONDS);

        // 3. Await audio data
        byte[] audioBytes = future.get(15, TimeUnit.SECONDS);
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "Done");

        if (audioBytes == null || audioBytes.length == 0) {
            throw new RuntimeException("Edge TTS returned no audio data.");
        }

        return new FastTTSAudio(audioBytes, 24000);
    }

    @Override
    public void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception {
        FastTTSAudio audio = synthesize(text, voice, config);
        chunkConsumer.accept(audio.getData());
    }

    @Override
    public List<FastTTSVoice> getVoices() {
        return List.of(
                new FastTTSVoice("de-DE-FlorianMultilingualNeural", "Florian (Neural)", "de-DE", "Male", "edgetts"),
                new FastTTSVoice("de-DE-SeraphinaMultilingualNeural", "Seraphina (Neural)", "de-DE", "Female", "edgetts"),
                new FastTTSVoice("de-DE-ConradNeural", "Conrad (Neural)", "de-DE", "Male", "edgetts"),
                new FastTTSVoice("de-DE-KatjaNeural", "Katja (Neural)", "de-DE", "Female", "edgetts"),
                new FastTTSVoice("de-DE-KillianNeural", "Killian (Neural)", "de-DE", "Male", "edgetts"),
                new FastTTSVoice("en-US-JennyNeural", "Jenny (Neural)", "en-US", "Female", "edgetts"),
                new FastTTSVoice("en-US-GuyNeural", "Guy (Neural)", "en-US", "Male", "edgetts"),
                new FastTTSVoice("en-US-AriaNeural", "Aria (Neural)", "en-US", "Female", "edgetts"),
                new FastTTSVoice("en-GB-RyanNeural", "Ryan (Neural)", "en-GB", "Male", "edgetts")
        );
    }

    @Override
    public String getName() {
        return "EdgeTTS";
    }

    private static String resolveVoiceName(FastTTSVoice voice) {
        if (voice == null || voice.id() == null || voice.id().isBlank()) {
            return "de-DE-KatjaNeural";
        }
        String id = voice.id();
        if (id.contains("Florian")) return "de-DE-FlorianMultilingualNeural";
        if (id.contains("Seraphina")) return "de-DE-SeraphinaMultilingualNeural";
        if (id.contains("Conrad")) return "de-DE-ConradNeural";
        if (id.contains("Katja")) return "de-DE-KatjaNeural";
        if (id.contains("Killian")) return "de-DE-KillianNeural";
        if (id.contains("Klara")) return "de-DE-KlaraNeural";
        if (id.contains("Jenny")) return "en-US-JennyNeural";
        if (id.contains("Guy")) return "en-US-GuyNeural";
        if (id.contains("Aria")) return "en-US-AriaNeural";
        if (id.contains("Ryan")) return "en-GB-RyanNeural";
        return id;
    }

    private static String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z (zzzz)", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
