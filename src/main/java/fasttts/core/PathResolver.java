package fasttts.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Intelligent path resolver for models and executables.
 */
public final class PathResolver {

    private static final String DEFAULT_INSTALL_DIR = System.getProperty("user.home") + File.separator + ".fasttts";
    private static Properties props = new Properties();

    static {
        loadProperties();
    }

    public static void loadProperties() {
        File propFile = new File(DEFAULT_INSTALL_DIR, "fasttts.properties");
        if (propFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propFile)) {
                props.clear();
                props.load(fis);
            } catch (Exception ignored) {}
        } else {
            // Fallback to local fasttts.properties if exists
            try (FileInputStream fis = new FileInputStream("fasttts.properties")) {
                props.load(fis);
            } catch (Exception e) {
                try (FileInputStream fis = new FileInputStream("../../fasttts.properties")) {
                    props.load(fis);
                } catch (Exception ignored) {}
            }
        }
    }

    public static String getProperty(String key) {
        return props.getProperty(key);
    }

    public static void saveProperties(Properties newProps) {
        File dir = new File(DEFAULT_INSTALL_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File propFile = new File(dir, "fasttts.properties");
        try (FileOutputStream fos = new FileOutputStream(propFile)) {
            newProps.store(fos, "FastTTS Settings");
            props = (Properties) newProps.clone();
        } catch (Exception ignored) {}

        // Also save portable backups in project directories so settings survive folder clearing
        try (FileOutputStream fos = new FileOutputStream("fasttts.properties")) {
            newProps.store(fos, "Portable local backup");
        } catch (Exception ignored) {}
        try (FileOutputStream fos = new FileOutputStream("../../fasttts.properties")) {
            newProps.store(fos, "Portable parent backup");
        } catch (Exception ignored) {}
    }

    public static String getInstallDir() {
        return DEFAULT_INSTALL_DIR;
    }

    public static String resolve(String key, String defaultFilename) {
        // 1. Check properties (explicit path)
        String path = props.getProperty(key);
        if (path != null && new File(path).exists()) {
            return path;
        }

        // 2. Check standard ~/.fasttts install directory
        File standard = new File(DEFAULT_INSTALL_DIR, defaultFilename);
        if (standard.exists()) {
            return standard.getAbsolutePath();
        }

        // 3. Check specific subdirectories under ~/.fasttts/ (e.g. piper/ or kokoro/)
        if (key.startsWith("piper.")) {
            File piperFile = new File(new File(DEFAULT_INSTALL_DIR, "piper"), defaultFilename);
            if (piperFile.exists()) {
                return piperFile.getAbsolutePath();
            }
        } else if (key.startsWith("kokoro.")) {
            File kokoroFile = new File(new File(DEFAULT_INSTALL_DIR, "kokoro"), defaultFilename);
            if (kokoroFile.exists()) {
                return kokoroFile.getAbsolutePath();
            }
        }

        // 4. Check local directory (portable)
        File local = new File(defaultFilename);
        if (local.exists()) {
            return local.getAbsolutePath();
        }

        // 5. Check examples/Installer (where installer downloads)
        File installerLoc = new File("../Installer/" + defaultFilename);
        if (installerLoc.exists()) {
            return installerLoc.getAbsolutePath();
        }

        // 6. Check project root
        File rootLoc = new File("../../" + defaultFilename);
        if (rootLoc.exists()) {
            return rootLoc.getAbsolutePath();
        }

        return defaultFilename; // Fallback
    }
}
