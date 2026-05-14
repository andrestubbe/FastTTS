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
        // For Windows, we can either stream via a callback from C++ 
        // or just synthesize and split (simulated streaming for now, 
        // will upgrade to true native streaming if needed).
        byte[] full = synthesize(text, voice, config);
        // Split into 4KB chunks for demo purposes
        int chunkSize = 4096;
        for (int i = 0; i < full.length; i += chunkSize) {
            int length = Math.min(chunkSize, full.length - i);
            byte[] chunk = new byte[length];
            System.arraycopy(full, i, chunk, 0, length);
            chunkConsumer.accept(chunk);
        }
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
    
    private native List<FastTTSVoice> getVoicesNative();
}
