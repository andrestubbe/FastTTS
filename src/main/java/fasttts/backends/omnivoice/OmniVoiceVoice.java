package fasttts.backends.omnivoice;

import fasttts.core.FastTTSVoice;
import java.util.List;

/**
 * Predefined voice presets and voice cloning references for OmniVoice.
 */
public final class OmniVoiceVoice {

    public static final FastTTSVoice BRIA = new FastTTSVoice("bria", "OmniVoice-Bria", "en-US", "Female (Cloned)", "omnivoice");
    public static final FastTTSVoice LOONA = new FastTTSVoice("loona", "OmniVoice-Loona", "en-US", "Female (Cloned)", "omnivoice");
    public static final FastTTSVoice CUSTOM = new FastTTSVoice("custom", "OmniVoice-Custom", "multilingual", "Neural (Zero-Shot)", "omnivoice");

    private static final List<FastTTSVoice> ALL = List.of(BRIA, LOONA, CUSTOM);

    private OmniVoiceVoice() {}

    public static List<FastTTSVoice> all() {
        return ALL;
    }
}
