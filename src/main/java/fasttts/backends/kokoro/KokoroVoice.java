package fasttts.backends.kokoro;

import fasttts.core.FastTTSVoice;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Predefined voices and speaker ID mapping for Kokoro TTS.
 */
public final class KokoroVoice {

    public static final FastTTSVoice AF_BELLA = new FastTTSVoice("0", "af_bella", "en-US", "Female", "kokoro");
    public static final FastTTSVoice AF_SARAH = new FastTTSVoice("1", "af_sarah", "en-US", "Female", "kokoro");
    public static final FastTTSVoice AF_NICOLE = new FastTTSVoice("2", "af_nicole", "en-US", "Female", "kokoro");
    public static final FastTTSVoice AF_SKY = new FastTTSVoice("3", "af_sky", "en-US", "Female", "kokoro");
    public static final FastTTSVoice AM_ADAM = new FastTTSVoice("4", "am_adam", "en-US", "Male", "kokoro");
    public static final FastTTSVoice AM_MICHAEL = new FastTTSVoice("5", "am_michael", "en-US", "Male", "kokoro");
    public static final FastTTSVoice BF_EMMA = new FastTTSVoice("6", "bf_emma", "en-GB", "Female", "kokoro");
    public static final FastTTSVoice BF_ISABELLA = new FastTTSVoice("7", "bf_isabella", "en-GB", "Female", "kokoro");
    public static final FastTTSVoice BM_GEORGE = new FastTTSVoice("8", "bm_george", "en-GB", "Male", "kokoro");
    public static final FastTTSVoice BM_LEWIS = new FastTTSVoice("9", "bm_lewis", "en-GB", "Male", "kokoro");

    private static final List<FastTTSVoice> ALL_VOICES = List.of(
            AF_BELLA,
            AF_SARAH,
            AF_NICOLE,
            AF_SKY,
            AM_ADAM,
            AM_MICHAEL,
            BF_EMMA,
            BF_ISABELLA,
            BM_GEORGE,
            BM_LEWIS
    );

    private static final Map<String, Integer> NAME_TO_SID;

    static {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (FastTTSVoice v : ALL_VOICES) {
            int sid = Integer.parseInt(v.id());
            map.put(v.name().toLowerCase(Locale.ROOT), sid);
            // Also allow matching without prefix, e.g. "bella" -> 0, "sarah" -> 1
            if (v.name().contains("_")) {
                String shortName = v.name().substring(v.name().indexOf('_') + 1).toLowerCase(Locale.ROOT);
                map.put(shortName, sid);
            }
        }
        NAME_TO_SID = Collections.unmodifiableMap(map);
    }

    private KokoroVoice() {}

    public static List<FastTTSVoice> all() {
        return ALL_VOICES;
    }

    /**
     * Resolves the speaker ID (sid) for sherpa-onnx from a FastTTSVoice or name/id.
     */
    public static int resolveSpeakerId(FastTTSVoice voice) {
        if (voice == null) {
            return 0; // Default to af_bella
        }

        // 1. Try parsing voice.id() directly as integer
        if (voice.id() != null && !voice.id().isBlank()) {
            try {
                return Integer.parseInt(voice.id().trim());
            } catch (NumberFormatException ignored) {}

            Integer sid = NAME_TO_SID.get(voice.id().trim().toLowerCase(Locale.ROOT));
            if (sid != null) return sid;
        }

        // 2. Try looking up by voice.name()
        if (voice.name() != null && !voice.name().isBlank()) {
            Integer sid = NAME_TO_SID.get(voice.name().trim().toLowerCase(Locale.ROOT));
            if (sid != null) return sid;
        }

        return 0;
    }

    public static FastTTSVoice fromName(String name) {
        if (name == null || name.isBlank()) return AF_BELLA;
        String clean = name.trim().toLowerCase(Locale.ROOT);
        for (FastTTSVoice v : ALL_VOICES) {
            if (v.name().equalsIgnoreCase(clean) || v.id().equalsIgnoreCase(clean)) {
                return v;
            }
            if (v.name().contains("_")) {
                String shortName = v.name().substring(v.name().indexOf('_') + 1);
                if (shortName.equalsIgnoreCase(clean)) {
                    return v;
                }
            }
        }
        return AF_BELLA;
    }
}
