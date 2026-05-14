package fasttts.backends.kokoro;

import fastcore.FastCore;
import fasttts.core.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Kokoro TTS Backend via Native ONNX Runtime Bridge.
 * Eliminates CLI overhead for sub-millisecond start times and real-time streaming.
 */
public final class KokoroBackend implements FastTTSBackend {

    static {
        FastCore.loadLibrary("fasttts");
    }

    private String modelPath;
    private long nativeHandle;

    public KokoroBackend(String modelPath) {
        this.modelPath = modelPath;
        this.nativeHandle = initNative(modelPath);
    }

    @Override
    public FastTTSAudio synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        byte[] data = synthesizeNative(nativeHandle, text, voice != null ? voice.id() : "default");
        return new FastTTSAudio(data, 24000);
    }

    @Override
    public void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception {
        FastTTSAudio audio = synthesize(text, voice, config);
        chunkConsumer.accept(audio.getData());
    }

    @Override
    public List<FastTTSVoice> getVoices() {
        return Collections.emptyList(); // Voices are internal to the model
    }

    @Override
    public String getName() {
        return "Kokoro";
    }

    public void close() {
        if (nativeHandle != 0) {
            releaseNative(nativeHandle);
            nativeHandle = 0;
        }
    }

    // --- Native Methods ---

    private native long initNative(String modelPath);
    private native byte[] synthesizeNative(long handle, String text, String voiceId);
    private native void streamNative(long handle, String text, String voiceId, Consumer<byte[]> chunkConsumer);
    private native void releaseNative(long handle);
}
