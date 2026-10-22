package fasttts.backends.cosyvoice;

import fasttts.core.FastTTSVoice;
import java.util.List;
import java.util.Locale;

/**
 * Predefined voices for CosyVoice3 TTS.
 * Multi-lingual zero-shot voice cloning with 8 built-in voices plus FLEURS dialects.
 */
public final class CosyVoiceVoice {

    public static final FastTTSVoice ZERO_SHOT = new FastTTSVoice("zero_shot", "Zero-Shot Clone", "en-US", "Natural", "cosyvoice");
    public static final FastTTSVoice FLEURS_EN = new FastTTSVoice("fleurs_en", "FLEURS English", "en-US", "Natural", "cosyvoice");
    public static final FastTTSVoice FLEURS_DE = new FastTTSVoice("fleurs_de", "FLEURS German", "de-DE", "Natural", "cosyvoice");
    public static final FastTTSVoice FLEURS_ZH = new FastTTSVoice("fleurs_zh", "FLEURS Chinese", "zh-CN", "Natural", "cosyvoice");
    public static final FastTTSVoice FLEURS_JA = new FastTTSVoice("fleurs_ja", "FLEURS Japanese", "ja-JP", "Natural", "cosyvoice");
    public static final FastTTSVoice FLEURS_FR = new FastTTSVoice("fleurs_fr", "FLEURS French", "fr-FR", "Natural", "cosyvoice");
    public static final FastTTSVoice FLEURS_ES = new FastTTSVoice("fleurs_es", "FLEURS Spanish", "es-ES", "Natural", "cosyvoice");
    public static final FastTTSVoice FLEURS_KO = new FastTTSVoice("fleurs_ko", "FLEURS Korean", "ko-KR", "Natural", "cosyvoice");

    private static final List<FastTTSVoice> ALL_VOICES = List.of(
            ZERO_SHOT,
            FLEURS_EN,
            FLEURS_DE,
            FLEURS_ZH,
            FLEURS_JA,
            FLEURS_FR,
            FLEURS_ES,
            FLEURS_KO
    );

    private CosyVoiceVoice() {}

    public static List<FastTTSVoice> all() {
        return ALL_VOICES;
    }

    /**
     * Resolves a voice by name or ID.
     */
    public static FastTTSVoice fromName(String name) {
        if (name == null || name.isBlank()) return ZERO_SHOT;
        String clean = name.trim().toLowerCase(Locale.ROOT);
        for (FastTTSVoice v : ALL_VOICES) {
            if (v.name().equalsIgnoreCase(clean) || v.id().equalsIgnoreCase(clean)) {
                return v;
            }
        }
        return ZERO_SHOT;
    }
}
