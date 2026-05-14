package fasttts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * FastTTS Main API.
 */
public final class FastTTS {
    
    private final Map<String, FastTTSBackend> backends = new ConcurrentHashMap<>();
    private String defaultBackendName;

    public void registerBackend(FastTTSBackend backend) {
        backends.put(backend.getName().toLowerCase(), backend);
        if (defaultBackendName == null) {
            defaultBackendName = backend.getName().toLowerCase();
        }
    }

    /**
     * Quick speak using default engine.
     */
    public void speak(String text) {
        try {
            speak(defaultBackendName, text, null, null);
        } catch (Exception e) {
            System.err.println("FastTTS Error: " + e.getMessage());
        }
    }

    public byte[] speak(String backendName, String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        FastTTSBackend backend = getBackend(backendName);
        return backend.synthesize(text, voice, config != null ? config : new FastTTSConfig());
    }

    public void stream(String backendName, String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> consumer) throws Exception {
        FastTTSBackend backend = getBackend(backendName);
        backend.stream(text, voice, config != null ? config : new FastTTSConfig(), consumer);
    }

    public List<FastTTSVoice> getAllVoices() {
        List<FastTTSVoice> all = new ArrayList<>();
        for (FastTTSBackend backend : backends.values()) {
            all.addAll(backend.getVoices());
        }
        return all;
    }

    private FastTTSBackend getBackend(String name) {
        FastTTSBackend b = backends.get(name.toLowerCase());
        if (b == null) throw new IllegalArgumentException("Backend not found: " + name);
        return b;
    }

    public void setDefaultBackend(String name) {
        this.defaultBackendName = name.toLowerCase();
    }
}
