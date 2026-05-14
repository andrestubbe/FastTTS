package fasttts;

public record FastTTSVoice(String id, String name, String language, String gender) {
    @Override
    public String toString() {
        return String.format("%s (%s, %s)", name, language, gender);
    }
}
