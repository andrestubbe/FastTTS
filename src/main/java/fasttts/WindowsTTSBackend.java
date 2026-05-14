package fasttts;

import fastcore.FastCore;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Windows-native TTS Backend using WinRT SpeechSynthesis.
 * Adheres to FastJava philosophy: Direct native calls, minimal overhead.
 */
public final class WindowsTTSBackend implements FastTTSBackend {

    static {
        FastCore.loadLibrary("fasttts");
    }

    @Override
    public byte[] synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        return synthesizeNative(
            text, 
            voice != null ? voice.id() : null, 
            config != null ? config.getRate() : 1.0f,
            config != null ? config.getPitch() : 1.0f,
            config != null ? config.getVolume() : 1.0f
        );
    }

    @Override
    public void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception {
        streamNative(
            text, 
            voice != null ? voice.id() : null, 
            config != null ? config.getRate() : 1.0f,
            config != null ? config.getPitch() : 1.0f,
            config != null ? config.getVolume() : 1.0f,
            chunkConsumer
        );
    }

    @Override
    public List<FastTTSVoice> getVoices() {
        // This would ideally call a native method to list WinRT voices.
        // For the first version, let's return a placeholder or implement the native call.
        return getVoicesNative();
    }

    @Override
    public String getName() {
        return "Windows";
    }

    // --- Native Methods ---

    private native byte[] synthesizeNative(String text, String voiceId, float rate, float pitch, float volume);
    
    private native void streamNative(String text, String voiceId, float rate, float pitch, float volume, Consumer<byte[]> chunkConsumer);
    
    private native List<FastTTSVoice> getVoicesNative();
}
