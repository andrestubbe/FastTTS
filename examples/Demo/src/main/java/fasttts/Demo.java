package fasttts;

import fastansi.FastANSI;
import fastkeyboard.FastKeyboard;
import fastkeyboard.FastKeyboardImpl;
import fastterminal.FastTerminal;
import fastterminal.AnsiMouse;
import fastmouse.FastMouseListener;
import fasttts.core.*;
import fasttts.backends.windows.*;
import fasttts.backends.piper.*;
import fasttts.backends.elevenlabs.*;
import fasttts.backends.kokoro.*;
import fasttts.backends.deepgram.*;

import java.util.List;
import java.util.Properties;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.sound.sampled.*;

/**
 * Terminal TUI Demo for FastTTS using FastKeyboard and FastANSI.
 * Displays all engines and voices on a single screen with real-time text input
 * and interactive arrow-key selection.
 */
public class Demo {

    private static FastTTS tts;
    private static List<FastTTSVoice> voices;
    private static List<FastTTSVoice> originalVoices;
    private static int selectedVoiceIndex = 0;
    private static final StringBuilder textInput = new StringBuilder("Hello world - this is text to speech");
    private static String statusMessage = "Ready. Start typing to edit text, UP/DOWN to select voice, ENTER to speak, ESC to exit.";
    private static boolean isRunning = true;
    private static final Object lock = new Object();
    private static boolean isSynthesizing = false;
    private static final java.util.Map<String, Double> voiceLatencies = new java.util.concurrent.ConcurrentHashMap<>();
    private static AnsiMouse mouse;
    private static volatile int lastMouseCellY = -1;

    public static void main(String[] args) {
        tts = new FastTTS();
        
        java.util.Properties props = new java.util.Properties();
        try (InputStream is = new FileInputStream("../../fasttts.properties")) {
            props.load(is);
            String elKey = props.getProperty("elevenlabs.api.key");
            if (elKey != null && !elKey.isEmpty()) {
                tts.registerBackend(new ElevenLabsBackend(elKey, props.getProperty("elevenlabs.default.voice"), 0.5f, 0.75f));
            }
            
            String dgKey = props.getProperty("deepgram.api.key");
            if (dgKey != null && !dgKey.isEmpty()) {
                tts.registerBackend(new DeepgramBackend(dgKey));
            }
        } catch (Exception e) {
            statusMessage = "[Warn] Properties failed to load: " + e.getMessage();
        }

        // 2. Register Piper
        String piperPath = PathResolver.resolve("piper.path", "piper.exe");
        String piperModel = PathResolver.resolve("piper.model", "thorsten.onnx");
        if (new File(piperPath).exists()) {
            tts.registerBackend(new PiperBackend(piperPath, piperModel));
        }

        // 3. Register Kokoro Native
        String kokoroModel = PathResolver.resolve("kokoro.model", "kokoro-v0_19.onnx");
        if (new File(kokoroModel).exists()) {
            try {
                tts.registerBackend(new KokoroBackend(kokoroModel));
            } catch (Exception e) {
                statusMessage = "[Kokoro Error] Load fail: " + e.getMessage();
            }
        }

        // 4. Register Windows (Lowest priority)
        tts.registerBackend(new WindowsTTSBackend());

        originalVoices = tts.getAllVoices();
        if (originalVoices.isEmpty()) {
            statusMessage = "[Error] No voices registered!";
            return;
        }

        // Sort voices: local first, then online
        voices = new java.util.ArrayList<>(originalVoices);
        voices.sort((v1, v2) -> {
            boolean local1 = isLocal(v1);
            boolean local2 = isLocal(v2);
            if (local1 && !local2) return -1;
            if (!local1 && local2) return 1;
            return 0;
        });

        // Initialize FastKeyboard JNI listener
        final FastKeyboard keyboard = new FastKeyboardImpl();

        // Initialize AnsiMouse SGR listener
        mouse = AnsiMouse.open(new FastMouseListener() {
            @Override
            public void onMouseMove(long deviceHandle, int deltaX, int deltaY, int absX, int absY) {
                lastMouseCellY = absY;
                int hoveredIndex = absY - 4;
                if (hoveredIndex >= 0 && hoveredIndex < voices.size()) {
                    if (selectedVoiceIndex != hoveredIndex) {
                        selectedVoiceIndex = hoveredIndex;
                        drawScreen();
                    }
                }
            }

            @Override
            public void onMouseButton(long deviceHandle, int buttonId, boolean isPressed) {
                if (buttonId == 0 && isPressed) { // Left click
                    int hoveredIndex = lastMouseCellY - 4;
                    if (hoveredIndex >= 0 && hoveredIndex < voices.size()) {
                        triggerSynthesis();
                    }
                }
            }

            @Override
            public void onMouseWheel(long deviceHandle, int delta) {
                if (delta > 0) { // Scroll up
                    selectedVoiceIndex = (selectedVoiceIndex - 1 + voices.size()) % voices.size();
                    drawScreen();
                } else if (delta < 0) { // Scroll down
                    selectedVoiceIndex = (selectedVoiceIndex + 1) % voices.size();
                    drawScreen();
                }
            }
        });

        // Safe cleanup shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.print(FastANSI.ALT_BUFFER_OFF + FastANSI.CURSOR_SHOW + FastANSI.RESET);
            try {
                keyboard.stopListening();
            } catch (Throwable ignored) {}
            try {
                if (mouse != null) mouse.close();
            } catch (Throwable ignored) {}
        }));

        // Enable alternate screen buffer and hide standard cursor
        System.out.print(FastANSI.ALT_BUFFER_ON + FastANSI.CURSOR_HIDE);
        drawScreen();

        keyboard.startListening((deviceHandle, vKey, makeCode, isPressed, isE0, timestamp, keyChar) -> {
            if (isPressed) {
                // Focus check
                try {
                    if (!FastTerminal.isTerminalFocused()) {
                        return;
                    }
                } catch (Throwable ignored) {}

                boolean redrawNeeded = false;

                if (vKey == 0x26) { // VK_UP
                    selectedVoiceIndex = (selectedVoiceIndex - 1 + voices.size()) % voices.size();
                    redrawNeeded = true;
                } else if (vKey == 0x28) { // VK_DOWN
                    selectedVoiceIndex = (selectedVoiceIndex + 1) % voices.size();
                    redrawNeeded = true;
                } else if (vKey == 0x08) { // VK_BACK (Backspace)
                    if (textInput.length() > 0) {
                        textInput.deleteCharAt(textInput.length() - 1);
                        redrawNeeded = true;
                    }
                } else if (vKey == 0x1B) { // VK_ESCAPE (Exit)
                    synchronized (lock) {
                        isRunning = false;
                        lock.notifyAll();
                    }
                } else if (vKey == 0x0D) { // VK_RETURN (Enter -> Synthesize and speak)
                    triggerSynthesis();
                } else if (keyChar != null && !keyChar.isEmpty()) {
                    char c = keyChar.charAt(0);
                    // Filter control characters (values < 32 and 127)
                    if (c >= 32 && c != 127) {
                        textInput.append(keyChar);
                        redrawNeeded = true;
                    }
                }

                if (redrawNeeded) {
                    drawScreen();
                }
            }
        });

        // Main thread block
        synchronized (lock) {
            while (isRunning) {
                try {
                    lock.wait(100);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        // Cleanup and exit alternate buffer
        System.out.print(FastANSI.ALT_BUFFER_OFF + FastANSI.CURSOR_SHOW + FastANSI.RESET);
        try {
            keyboard.stopListening();
        } catch (Throwable ignored) {}
        try {
            if (mouse != null) mouse.close();
        } catch (Throwable ignored) {}
        System.out.println("Goodbye!");
    }

    private static void drawScreen() {
        StringBuilder sb = new StringBuilder();
        sb.append(FastANSI.CLEAR_SCREEN).append(FastANSI.CURSOR_HOME);
        
        // Header
        sb.append("\n");
        sb.append(FastANSI.BOLD).append(FastANSI.fg(255, 255, 255)).append("   FastTTS Real-Time Multi-Engine TUI Demo v0.2.0\n").append(FastANSI.RESET);
        sb.append("\n");

        // Table Header
        String header = String.format("    %-8s %-12s %-25s %-30s %s", "TYPE", "ENGINE", "VOICE", "DETAILS", "LATENCY");
        sb.append(FastANSI.BOLD).append(FastANSI.fg(255, 255, 255)).append(header).append("\n").append(FastANSI.RESET);

        for (int i = 0; i < voices.size(); i++) {
            FastTTSVoice v = voices.get(i);
            String type = isLocal(v) ? "local" : "online";
            
            Double latency = voiceLatencies.get(v.id());
            String latencyStr = "";
            if (latency != null) {
                latencyStr = String.format(java.util.Locale.GERMAN, "%.2f ms", latency);
            }
            
            // Format name and details
            String[] split = splitName(v.name());
            String name = split[0];
            String details = split[1];
            
            // Format row
            String row = String.format("%-8s %-12s %-25s %-30s %s", 
                type, v.backendId(), name, details, latencyStr);
            
            if (selectedVoiceIndex == i) {
                sb.append(FastANSI.bg(0, 180, 216)).append(FastANSI.fg(0, 0, 0))
                  .append(" -> ").append(row).append(FastANSI.RESET).append("\n");
            } else {
                sb.append(FastANSI.fg(180, 180, 180))
                  .append("    ").append(row).append(FastANSI.RESET).append("\n");
            }
        }

        // Text input section
        sb.append("\n");
        sb.append(FastANSI.BOLD).append(FastANSI.fg(255, 255, 255)).append("  📝  ENTER TEXT TO SYNTHESIZE (Start typing to edit):\n").append(FastANSI.RESET);
        sb.append("\n");
        
        String cursorStr = FastANSI.fg(0, 255, 128) + "█" + FastANSI.RESET;
        sb.append("  ").append(FastANSI.fg(255, 255, 255)).append("> ").append(textInput).append(cursorStr).append(FastANSI.RESET).append("\n");
        
        sb.append("\n");
        
        // Status section
        sb.append(FastANSI.BOLD).append(FastANSI.fg(255, 255, 255)).append("  ⚡  STATUS:\n").append(FastANSI.RESET);
        String statusColor = isSynthesizing ? FastANSI.fg(255, 165, 0) : FastANSI.fg(0, 255, 128);
        sb.append("  ").append(statusColor).append(statusMessage).append(FastANSI.RESET).append("\n");
        sb.append("\n");

        System.out.print(sb.toString());
        System.out.flush();
    }

    private static boolean isLocal(FastTTSVoice voice) {
        String backend = voice.backendId().toLowerCase();
        return "windows".equals(backend) || "piper".equals(backend) || "kokoro".equals(backend);
    }

    private static String[] splitName(String fullName) {
        if (fullName.contains("Microsoft Hazel Desktop - English (Great Britain)")) {
            return new String[]{"Microsoft Hazel Desktop", "English (Great Britain)"};
        }
        if (fullName.contains("Microsoft Zira Desktop - English (United States)")) {
            return new String[]{"Microsoft Zira Desktop", "English (United States)"};
        }
        int idx = fullName.indexOf('(');
        if (idx != -1) {
            String baseName = fullName.substring(0, idx).trim();
            String details = fullName.substring(idx).trim();
            return new String[]{ baseName, details };
        }
        return new String[]{ fullName, "" };
    }

    private static void triggerSynthesis() {
        if (isSynthesizing || textInput.length() == 0) {
            return;
        }

        final FastTTSVoice selected = voices.get(selectedVoiceIndex);
        
        new Thread(() -> {
            isSynthesizing = true;
            statusMessage = "[Synthesizing...] Generating audio using " + selected.backendId().toUpperCase() + "...";
            drawScreen();
            
            long startTime = System.nanoTime();
            try {
                FastTTSAudio audio = tts.speak(selected.backendId(), textInput.toString(), selected, null);
                long endTime = System.nanoTime();
                double durationMs = (endTime - startTime) / 1_000_000.0;
                
                if (audio != null) {
                    voiceLatencies.put(selected.id(), durationMs);
                    statusMessage = "Playing audio...";
                    drawScreen();
                    
                    // Log latency
                    try (java.io.FileWriter fw = new java.io.FileWriter("../../latency-results.log", true)) {
                        String text = textInput.toString();
                        String snippet = text.length() > 30 ? text.substring(0, 27) + "..." : text;
                        fw.write(String.format("[%s] Latency: %.2f ms | Text: %s\n", 
                            selected.backendId().toUpperCase(), durationMs, snippet));
                    } catch (Exception ignored) {}

                    playAudio(audio);
                    
                    statusMessage = "[Success] Speech generated successfully. Press ENTER to repeat, type to edit.";
                } else {
                    statusMessage = "[Error] No audio generated.";
                }
            } catch (Exception e) {
                statusMessage = "[Error] Synthesis failed: " + e.getMessage();
            } finally {
                isSynthesizing = false;
                drawScreen();
            }
        }).start();
    }

    private static void playAudio(FastTTSAudio audio) {
        try {
            byte[] data = audio.getData();
            AudioInputStream ais;
            try {
                ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
            } catch (UnsupportedAudioFileException e) {
                AudioFormat rawFormat = new AudioFormat(audio.getSampleRate(), 16, 1, true, false);
                ais = new AudioInputStream(new ByteArrayInputStream(data), rawFormat, data.length / 2);
            }

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, ais.getFormat());
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(ais.getFormat());
            line.start();

            byte[] buffer = new byte[4096];
            int read;
            while ((read = ais.read(buffer)) != -1) {
                line.write(buffer, 0, read);
            }
            
            line.drain();
            line.close();
        } catch (Exception e) {
            statusMessage = "[Playback Error] " + e.getMessage();
        }
    }
}
