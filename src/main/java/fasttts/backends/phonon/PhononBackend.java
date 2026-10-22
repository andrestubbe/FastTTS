package fasttts.backends.phonon;

import fasttts.core.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Phonon TTS Backend powered by Gradium's ultra-compact, on-device Rust runtime (ptts.exe).
 * 31M parameter flow-matching model with 24kHz Mimi audio codec and <30ms latency.
 */
public final class PhononBackend implements FastTTSBackend {

    private String binaryPath;

    public PhononBackend() {
        this(resolveBinaryPath());
    }

    public PhononBackend(String binaryPath) {
        this.binaryPath = binaryPath != null ? binaryPath : resolveBinaryPath();
    }

    private static String resolveBinaryPath() {
        String[] candidates = new String[]{
                "Speak+/dist/Speak+/bin/ptts.exe",
                "dist/Speak+/bin/ptts.exe",
                "bin/ptts.exe",
                "../Speak+/dist/Speak+/bin/ptts.exe",
                "C:/Users/andre/Documents/2026-09-17-Work-Applications/Speak+/dist/Speak+/bin/ptts.exe",
                "ptts.exe"
        };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists()) return f.getAbsolutePath();
        }
        String userHome = System.getProperty("user.home", ".");
        File homeBin = new File(userHome, ".speak/bin/ptts.exe");
        if (homeBin.exists()) return homeBin.getAbsolutePath();
        return "dist/Speak+/bin/ptts.exe";
    }

    @Override
    public String getName() {
        return "Phonon";
    }

    @Override
    public boolean isAvailable() {
        return new File(binaryPath).exists();
    }

    @Override
    public void prepare() {
    }

    @Override
    public boolean isPrepared() {
        return isAvailable();
    }

    @Override
    public FastTTSAudio synthesize(String text, FastTTSVoice voice, FastTTSConfig config) throws Exception {
        if (!isAvailable()) {
            throw new FileNotFoundException("Phonon binary not found at: " + binaryPath);
        }
        if (text == null || text.isBlank()) {
            return new FastTTSAudio(new byte[0], 24000, 16, 1);
        }

        Path tempWav = Files.createTempFile("phonon_out_", ".wav");
        try {
            List<String> cmd = buildCommandLine(text, voice, tempWav.toAbsolutePath().toString());

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Consume stdout to prevent hang
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // discard or log
                }
            }

            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new TimeoutException("Phonon synthesis timed out after 60s");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IOException("ptts.exe exited with code " + exitCode);
            }

            if (Files.exists(tempWav) && Files.size(tempWav) > 44) {
                byte[] wavBytes = Files.readAllBytes(tempWav);
                return new FastTTSAudio(wavBytes, 24000, 16, 1);
            } else {
                throw new IOException("Phonon generated empty or invalid WAV file.");
            }
        } finally {
            try {
                Files.deleteIfExists(tempWav);
            } catch (IOException ignored) {}
        }
    }

    @Override
    public void stream(String text, FastTTSVoice voice, FastTTSConfig config, Consumer<byte[]> chunkConsumer) throws Exception {
        FastTTSAudio audio = synthesize(text, voice, config);
        if (audio != null && audio.getData() != null && audio.getData().length > 0) {
            chunkConsumer.accept(audio.getData());
        }
    }

    @Override
    public List<FastTTSVoice> getVoices() {
        List<FastTTSVoice> voices = new ArrayList<>();
        // English
        voices.add(new FastTTSVoice("phonon-zoey", "Zoey (Playful, Upbeat)", "en-US", "Female", "Phonon"));
        voices.add(new FastTTSVoice("phonon-juniper", "Juniper (Cheerful, Resonant)", "en-US", "Female", "Phonon"));
        voices.add(new FastTTSVoice("phonon-maeve", "Maeve (Attentive British)", "en-GB", "Female", "Phonon"));
        voices.add(new FastTTSVoice("phonon-alfie", "Alfie (Punchy British)", "en-GB", "Male", "Phonon"));
        voices.add(new FastTTSVoice("phonon-silas", "Captain Silas Blackwater (Pirate)", "en-GB", "Male", "Phonon"));
        voices.add(new FastTTSVoice("phonon-aldric", "Master Aldric (Wise Wizard)", "en-GB", "Male", "Phonon"));
        // German
        voices.add(new FastTTSVoice("phonon-svea", "Svea (Klar & Natürlich)", "de-DE", "Female", "Phonon"));
        // Spanish
        voices.add(new FastTTSVoice("phonon-noa", "Noa (Bubbly Castilian)", "es-ES", "Female", "Phonon"));
        voices.add(new FastTTSVoice("phonon-marcos", "Marcos (Upbeat Castilian)", "es-ES", "Male", "Phonon"));
        // French
        voices.add(new FastTTSVoice("phonon-ninon", "Ninon (Expressive Parisian)", "fr-FR", "Female", "Phonon"));
        // Portuguese
        voices.add(new FastTTSVoice("phonon-davi", "Davi (Upbeat Brazilian)", "pt-BR", "Male", "Phonon"));
        return voices;
    }

    private List<String> buildCommandLine(String text, FastTTSVoice voice, String outPath) {
        List<String> cmd = new ArrayList<>();
        cmd.add(new File(binaryPath).getAbsolutePath());

        String lang = "en";
        String voiceName = null;

        if (voice != null) {
            String id = voice.id() != null ? voice.id().toLowerCase(Locale.ROOT) : "";
            if (voice.locale() != null) {
                String l = voice.locale().toLowerCase(Locale.ROOT);
                if (l.startsWith("de")) lang = "de";
                else if (l.startsWith("es")) lang = "es";
                else if (l.startsWith("fr")) lang = "fr";
                else if (l.startsWith("pt")) lang = "pt";
            }
            if (id.contains("svea")) { lang = "de"; voiceName = "Svea"; }
            else if (id.contains("zoey")) { lang = "en"; voiceName = "Zoey"; }
            else if (id.contains("juniper")) { lang = "en"; voiceName = "Juniper"; }
            else if (id.contains("maeve")) { lang = "en"; voiceName = "Maeve"; }
            else if (id.contains("alfie")) { lang = "en"; voiceName = "Alfie"; }
            else if (id.contains("silas")) { lang = "en"; voiceName = "Captain Silas Blackwater"; }
            else if (id.contains("aldric")) { lang = "en"; voiceName = "Master Aldric"; }
            else if (id.contains("noa")) { lang = "es"; voiceName = "Noa"; }
            else if (id.contains("marcos")) { lang = "es"; voiceName = "Marcos"; }
            else if (id.contains("ninon")) { lang = "fr"; voiceName = "Ninon"; }
            else if (id.contains("davi")) { lang = "pt"; voiceName = "Davi"; }
        }

        cmd.add("--lang");
        cmd.add(lang);

        if (voiceName != null) {
            cmd.add("-v");
            cmd.add(voiceName);
        }

        cmd.add("-o");
        cmd.add(outPath);

        cmd.add(text);

        return cmd;
    }

    @Override
    public void close() {
    }
}
