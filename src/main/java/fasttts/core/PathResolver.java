package fasttts.core;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 * Intelligent path resolver for models and executables.
 */
public final class PathResolver {

    private static final String DEFAULT_INSTALL_DIR = "C:\\ProgramData\\FastTTS";
    private static Properties props = new Properties();

    static {
        // Try to load properties from project root or current dir
        try (FileInputStream fis = new FileInputStream("fasttts.properties")) {
            props.load(fis);
        } catch (Exception e) {
            // Fallback: try one level up (for examples)
            try (FileInputStream fis = new FileInputStream("../../fasttts.properties")) {
                props.load(fis);
            } catch (Exception ignored) {}
        }
    }

    public static String resolve(String key, String defaultFilename) {
        System.out.println("[DEBUG] Resolving " + key + " (default: " + defaultFilename + ")...");
        
        // 1. Check properties (explicit path)
        String path = props.getProperty(key);
        if (path != null && new File(path).exists()) {
            System.out.println("  -> Found in properties: " + path);
            return path;
        }

        // 2. Check local directory (portable)
        File local = new File(defaultFilename);
        if (local.exists()) {
            System.out.println("  -> Found in local dir: " + local.getAbsolutePath());
            return local.getAbsolutePath();
        }
        
        // 3. Check examples/Installer (where installer downloads)
        File installerLoc = new File("../Installer/" + defaultFilename);
        if (installerLoc.exists()) {
            System.out.println("  -> Found in Installer dir: " + installerLoc.getAbsolutePath());
            return installerLoc.getAbsolutePath();
        }

        // 4. Check project root (for cases where demo is in examples/Demo)
        File rootLoc = new File("../../" + defaultFilename);
        if (rootLoc.exists()) {
            System.out.println("  -> Found in project root: " + rootLoc.getAbsolutePath());
            return rootLoc.getAbsolutePath();
        }

        // 5. Check standard install directory
        File standard = new File(DEFAULT_INSTALL_DIR, defaultFilename);
        if (standard.exists()) {
            System.out.println("  -> Found in standard install dir: " + standard.getAbsolutePath());
            return standard.getAbsolutePath();
        }

        System.out.println("  [WARN] " + key + " not found!");
        return defaultFilename; // Fallback
    }
}
