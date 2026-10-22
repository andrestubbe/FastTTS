package fasttts.backends.orpheus;

import fasttts.core.FastTTSVoice;
import java.util.List;
import java.util.Locale;

/**
 * Predefined voices for Orpheus TTS.
 * 8 fixed English speakers with natural intonation and emotion.
 */
public final class OrpheusVoice {

    public static final FastTTSVoice TARA = new FastTTSVoice("tara", "Tara", "en-US", "Female", "orpheus");
    public static final FastTTSVoice LEAH = new FastTTSVoice("leah", "Leah", "en-US", "Female", "orpheus");
    public static final FastTTSVoice JESS = new FastTTSVoice("jess", "Jess", "en-US", "Female", "orpheus");
    public static final FastTTSVoice LEO = new FastTTSVoice("leo", "Leo", "en-US", "Male", "orpheus");
    public static final FastTTSVoice DAN = new FastTTSVoice("dan", "Dan", "en-US", "Male", "orpheus");
    public static final FastTTSVoice MIA = new FastTTSVoice("mia", "Mia", "en-US", "Female", "orpheus");
    public static final FastTTSVoice ZAC = new FastTTSVoice("zac", "Zac", "en-US", "Male", "orpheus");
    public static final FastTTSVoice ZOE = new FastTTSVoice("zoe", "Zoe", "en-US", "Female", "orpheus");

    private static final List<FastTTSVoice> ALL_VOICES = List.of(
            TARA,
            LEAH,
            JESS,
            LEO,
            DAN,
            MIA,
            ZAC,
            ZOE
    );

    private OrpheusVoice() {}

    public static List<FastTTSVoice> all() {
        return ALL_VOICES;
    }

    /**
     * Resolves a voice by name or ID.
     */
    public static FastTTSVoice fromName(String name) {
        if (name == null || name.isBlank()) return TARA;
        String clean = name.trim().toLowerCase(Locale.ROOT);
        for (FastTTSVoice v : ALL_VOICES) {
            if (v.name().equalsIgnoreCase(clean) || v.id().equalsIgnoreCase(clean)) {
                return v;
            }
        }
        return TARA;
    }
}
