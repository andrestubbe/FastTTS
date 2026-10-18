package fasttts.installer;

/**
 * Listener interface for monitoring engine and model installation progress.
 * Suitable for both CLI indicators and UI progress bars.
 */
@FunctionalInterface
public interface FastTTSInstallListener {

    /**
     * Called whenever download or extraction progress changes.
     *
     * @param step         Human-readable description of current task (e.g. "Downloading f5-tts-v1-base-f16.gguf")
     * @param percent      Progress in percent (0.0 to 100.0, or -1.0 if indeterminate/extracting)
     * @param currentBytes Number of bytes transferred so far
     * @param totalBytes   Total expected bytes (-1 if unknown)
     */
    void onProgress(String step, double percent, long currentBytes, long totalBytes);
}
