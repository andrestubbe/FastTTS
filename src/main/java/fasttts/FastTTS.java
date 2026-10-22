package fasttts;

import fasttts.backends.cosyvoice.CosyVoiceBackend;
import fasttts.backends.dia2.Dia2Backend;
import fasttts.backends.f5tts.F5TTSBackend;
import fasttts.backends.huggingface.HuggingFaceBackend;
import fasttts.backends.kokoro.KokoroBackend;
import fasttts.backends.matcha.MatchaBackend;
import fasttts.backends.omnivoice.OmniVoiceBackend;
import fasttts.backends.openai.OpenAIBackend;
import fasttts.backends.orpheus.OrpheusBackend;
import fasttts.backends.piper.PiperBackend;
import fasttts.backends.pockettts.PocketTTSBackend;
import fasttts.backends.phonon.PhononBackend;
import fasttts.backends.vits.VITSBackend;
import fasttts.backends.windows.WindowsTTSBackend;
import fasttts.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * FastTTS Orchestrator.
 * High-performance TTS Registry for the FastJava ecosystem.
 */
public final class FastTTS implements AutoCloseable {

    private final Map<String, FastTTSBackend> backends = new ConcurrentHashMap<>();
    private String activeBackend;

    public static FastTTS standard() {
        FastTTS tts = new FastTTS();
        tts.registerBackend(new WindowsTTSBackend());
        tts.registerBackend(new KokoroBackend());
        tts.registerBackend(new MatchaBackend());
        tts.registerBackend(new VITSBackend());
        tts.registerBackend(new PiperBackend());
        tts.registerBackend(new PocketTTSBackend());
        tts.registerBackend(new PhononBackend());
        tts.registerBackend(new OmniVoiceBackend());
        tts.registerBackend(new F5TTSBackend());
        tts.registerBackend(new Dia2Backend());
        tts.registerBackend(new OrpheusBackend());
        tts.registerBackend(new CosyVoiceBackend());
        tts.registerBackend(new OpenAIBackend());
        tts.registerBackend(new HuggingFaceBackend());
        return tts;
    }

    public void registerBackend(FastTTSBackend backend) {
        backends.put(backend.getName().toLowerCase(), backend);
        if (activeBackend == null) {
            activeBackend = backend.getName().toLowerCase();
        }
    }

    public FastTTSAudio speak(String text) throws Exception {
        return speak(activeBackend, text, null, null);
    }

    public FastTTSAudio speak(String backendName, String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        FastTTSBackend backend = getBackend(backendName);
        return backend.synthesize(text, voice, config != null
                ? config
                : new FastTTSConfig());
    }

    public void stream(String backendName, String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> consumer) throws Exception {
        FastTTSBackend backend = getBackend(backendName);
        backend.stream(text, voice, config != null
                        ? config
                        : new FastTTSConfig(),
                consumer);
    }

    public void use(String name) {
        if (!backends.containsKey(name.toLowerCase())) {
            throw new IllegalArgumentException("Backend not registered: " + name);
        }
        this.activeBackend = name.toLowerCase();
    }

    public void prepare() {
        if (activeBackend != null) {
            prepare(activeBackend);
        }
    }

    public void prepare(String name) {
        FastTTSBackend backend = getBackend(name);
        backend.prepare();
    }

    public void prepareAll() {
        for (FastTTSBackend b : backends.values()) {
            b.prepare();
        }
    }

    public List<FastTTSVoice> getAllVoices() {
        List<FastTTSVoice> all = new ArrayList<>();
        for (FastTTSBackend backend : backends.values()) {
            all.addAll(backend.getVoices());
        }
        return all;
    }

    public FastTTSBackend getBackend(String name) {
        FastTTSBackend b = backends.get(name.toLowerCase());
        if (b == null) throw new IllegalArgumentException("Backend not found: " + name);
        return b;
    }

    public void setDefaultBackend(String name) {
        use(name);
    }

    public List<String> getRegisteredBackendNames() {
        return new ArrayList<>(backends.keySet());
    }

    @Override
    public void close() {
        for (FastTTSBackend b : backends.values()) {
            try {
                b.close();
            } catch (Exception ignored) {}
        }
    }
}
