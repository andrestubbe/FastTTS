package fasttts.core;

/**
 * Metadata for a TTS voice.
 */
public record FastTTSVoice(
    String id, 
    String name, 
    String locale, 
    String gender, 
    String backendId
) {
    @Override
    public String toString() {
        return String.format("%s (%s, %s)", name, locale, gender);
    }
}
