package fasttts.core;

public class FastTTSConfig {
    private float rate = 1.0f;
    private float pitch = 1.0f;
    private float volume = 1.0f;
    private java.util.Map<String, String> properties = new java.util.HashMap<>();

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }

    public float getRate() { return rate; }
    public void setRate(float rate) { this.rate = rate; }
    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    public float getVolume() { return volume; }
    public void setVolume(float volume) { this.volume = volume; }
}
