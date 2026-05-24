package fasttts;

import fastansi.FastANSI;
import fastkeyboard.FastKeyboard;
import fastkeyboard.FastKeyboardImpl;
import fastterminal.FastTerminal;
import fastterminal.FastTerminalRenderer;
import fastterminal.AnsiMouse;
import fastmouse.FastMouseListener;
import fastterminal.ui.Panel;
import fastterminal.FastTerminalScene;
import fasttts.core.*;
import fasttts.backends.windows.*;
import fasttts.backends.piper.*;
import fasttts.backends.elevenlabs.*;
import fasttts.backends.kokoro.*;
import fasttts.backends.deepgram.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import javax.sound.sampled.*;

/**
 * Interactive drag-and-drop workspace UI for FastTTS.
 * Built using the high-performance FastTerminal renderer, complete mouse drag/resize,
 * pinstriped BeOS amber panels, a pitch-black background, and viewport-scrolled voice selections.
 */
public class Demo {

    private static FastTTS tts;
    private static List<FastTTSVoice> voices;
    private static int selectedVoiceIndex = 0;
    private static final StringBuilder textInput = new StringBuilder("Hello world - this is text to speech");
    private static String statusMessage = "Drag header to move panel. Drag bottom-right corner to resize. Click rows to select. ESC to exit.";
    private static volatile boolean isRunning = true;
    private static boolean isSynthesizing = false;
    private static final java.util.Map<String, Double> voiceLatencies = new java.util.concurrent.ConcurrentHashMap<>();
    private static AnsiMouse mouse;
    
    private static volatile int currentCols = 100;
    private static volatile int currentRows = 30;
    private static volatile int lastMouseCellX = -1;
    private static volatile int lastMouseCellY = -1;
    private static volatile boolean isLeftPressed = false;
    private static volatile boolean isRightPressed = false;
    private static volatile boolean isDragging = false;
    private static volatile boolean isResizing = false;
    private static volatile int dragOffsetX = 0;
    private static volatile int dragOffsetY = 0;
    private static volatile boolean isMinimizePressed = false;
    private static volatile boolean lastCursorHiddenState = false;

    // The beautiful draggable/resizable mainPanel
    private static Panel mainPanel;

    // Overlay installation dialog variables
    private static volatile String activeDialog = null; // null, "piper", "kokoro", "elevenlabs", "deepgram"
    private static final StringBuilder dialogInput = new StringBuilder();
    private static volatile String dialogStatus = "";
    private static volatile double downloadProgress = 0.0;

    public static void main(String[] args) {
        tts = new FastTTS();
        
        // Ensure standard directory exists
        new File(PathResolver.getInstallDir()).mkdirs();
        PathResolver.loadProperties();

        // 1. Register backends if they are already installed/configured
        registerElevenLabsIfConfigured();
        registerDeepgramIfConfigured();
        registerPiperIfInstalled();
        registerKokoroIfInstalled();

        // Always register Windows backend
        tts.registerBackend(new WindowsTTSBackend());

        // 2. Initialize the unified voices list (includes virtual ones)
        initializeVoicesList();

        // Initialize FastKeyboard JNI listener
        final FastKeyboard keyboard = new FastKeyboardImpl();

        // Alternate screen buffer and hide standard cursor
        System.out.print(FastANSI.ALT_BUFFER_ON + FastANSI.CURSOR_HIDE);

        int cols = 100;
        int rows = 30;
        try {
            int[] size = FastTerminal.getTerminalSize();
            if (size != null && size[0] > 0 && size[1] > 0) {
                cols = size[0];
                rows = size[1];
            }
        } catch (Throwable ignored) {}
        currentCols = cols;
        currentRows = rows;

        FastTerminalRenderer renderer = new FastTerminalRenderer(cols, rows);
        FastTerminalScene canvas = new FastTerminalScene(0, 0, cols, rows);
        renderer.addScene(canvas);

        // Center main panel dynamically on screen startup
        int dashW = 96;
        int dashH = 22;
        int dashX = Math.max(0, (cols - dashW) / 2);
        int dashY = Math.max(0, (rows - dashH) / 2);

        mainPanel = new Panel(dashX, dashY, dashW, dashH, 0x000000); // pitch black inside
        mainPanel.setBorderStyle(Panel.BorderStyle.DOUBLE);
        mainPanel.setBorderFg(0xF0A500); // Amber border
        mainPanel.setHasHeaderBar(true);
        mainPanel.setHeaderBg(0xF0A500); // Amber header
        mainPanel.setHeaderFg(0x3D1C00); // Deep brown text
        mainPanel.setBeosStyle(true);
        mainPanel.setTitle(" ⚡ FastTTS Multi-Engine Control Center ");
        mainPanel.setShowWindowButtons(true);
        mainPanel.setHasShadow(true);
        mainPanel.setShadowAlpha(0.25);

        // Initialize AnsiMouse SGR listener
        mouse = AnsiMouse.open(new FastMouseListener() {
            @Override
            public void onMouseMove(long deviceHandle, int deltaX, int deltaY, int absX, int absY) {
                lastMouseCellX = absX;
                lastMouseCellY = absY;
                
                if (isResizing) {
                    int newW = absX - mainPanel.getX() + 1;
                    int newH = absY - mainPanel.getY() + 1;
                    if (newW < 40) newW = 40;
                    if (newH < 12) newH = 12;
                    mainPanel.setWidth(newW);
                    mainPanel.setHeight(newH);
                } else if (isDragging) {
                    int newX = absX - dragOffsetX;
                    int newY = absY - dragOffsetY;
                    if (newX < 0) newX = 0;
                    if (newX + mainPanel.getWidth() > currentCols) newX = currentCols - mainPanel.getWidth();
                    if (newY < 0) newY = 0;
                    if (newY + mainPanel.getHeight() > currentRows) newY = currentRows - mainPanel.getHeight();
                    mainPanel.setX(newX);
                    mainPanel.setY(newY);
                }
            }

            @Override
            public void onMouseButton(long deviceHandle, int buttonId, boolean isPressed) {
                if (buttonId == 0) { // Left Button
                    isLeftPressed = isPressed;
                    if (isPressed) {
                        int dx = mainPanel.getX();
                        int dy = mainPanel.getY();
                        int dw = mainPanel.getWidth();
                        int dh = mainPanel.getHeight();
                        
                        if (activeDialog != null) {
                            // Click outside dialog closes it
                            int ow = 60;
                            int oh = 10;
                            int ox = Math.max(1, (currentCols - ow) / 2);
                            int oy = Math.max(1, (currentRows - oh) / 2);
                            if (lastMouseCellX < ox || lastMouseCellX >= ox + ow || lastMouseCellY < oy || lastMouseCellY >= oy + oh) {
                                activeDialog = null;
                            }
                            return;
                        }
                        
                        if (mainPanel.isMinimized()) {
                            if (mainPanel.isIconHit(lastMouseCellX, lastMouseCellY, 2, currentRows - 2)) {
                                mainPanel.toggleMinimize();
                            }
                        } else {
                            if (mainPanel.isResizeClick(lastMouseCellX, lastMouseCellY)) {
                                isResizing = true;
                            } else if (lastMouseCellX >= dx && lastMouseCellX < dx + dw && lastMouseCellY == dy) {
                                if (mainPanel.isCloseClick(lastMouseCellX, lastMouseCellY)) {
                                    System.exit(0);
                                } else if (mainPanel.isMinimizeClick(lastMouseCellX, lastMouseCellY)) {
                                    if (!mainPanel.isMinimized()) {
                                        mainPanel.toggleMinimize();
                                    }
                                    isMinimizePressed = true;
                                } else {
                                    isDragging = true;
                                    dragOffsetX = lastMouseCellX - dx;
                                    dragOffsetY = lastMouseCellY - dy;
                                }
                            } else {
                                // Content area click inside mainPanel
                                int relativeY = lastMouseCellY - dy;
                                int relativeX = lastMouseCellX - dx;
                                
                                int voiceRowStart = 4;
                                int footerSpace = 7;
                                int visibleVoices = dh - voiceRowStart - footerSpace;
                                if (visibleVoices < 1) visibleVoices = 1;
                                
                                int startIndex = Math.max(0, selectedVoiceIndex - visibleVoices / 2);
                                int endIndex = Math.min(voices.size(), startIndex + visibleVoices);
                                if (endIndex - startIndex < visibleVoices) {
                                    startIndex = Math.max(0, endIndex - visibleVoices);
                                }
                                
                                int clickedRowIndex = relativeY - voiceRowStart;
                                if (clickedRowIndex >= 0 && clickedRowIndex < (endIndex - startIndex)) {
                                    int targetVoiceIdx = startIndex + clickedRowIndex;
                                    if (selectedVoiceIndex != targetVoiceIdx) {
                                        selectedVoiceIndex = targetVoiceIdx;
                                    } else {
                                        // Clicked already selected voice - trigger Speak or Install/Config!
                                        FastTTSVoice selected = voices.get(selectedVoiceIndex);
                                        boolean active = isBackendActive(selected.backendId());
                                        
                                        // If clicked button columns
                                        int btnStart = dw - 18;
                                        int btnEnd = dw - 4;
                                        if (relativeX >= btnStart && relativeX <= btnEnd) {
                                            triggerInstallOrConfig(selected.backendId());
                                        } else if (active) {
                                            triggerSynthesis();
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        isDragging = false;
                        isResizing = false;
                        if (isMinimizePressed) {
                            if (mainPanel.isMinimizeClick(lastMouseCellX, lastMouseCellY)) {
                                if (mainPanel.isMinimized()) {
                                    mainPanel.toggleMinimize();
                                }
                            }
                            isMinimizePressed = false;
                        }
                    }
                } else if (buttonId == 1) {
                    isRightPressed = isPressed;
                }
            }

            @Override
            public void onMouseWheel(long deviceHandle, int delta) {
                if (activeDialog != null || mainPanel.isMinimized()) return;
                
                if (delta > 0) { // Scroll up
                    selectedVoiceIndex = (selectedVoiceIndex - 1 + voices.size()) % voices.size();
                } else if (delta < 0) { // Scroll down
                    selectedVoiceIndex = (selectedVoiceIndex + 1) % voices.size();
                }
            }
        });

        // Safe cleanup shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.print(FastANSI.ALT_BUFFER_OFF + FastANSI.CURSOR_SHOW + FastANSI.RESET);
            try {
                FastTerminal.setSystemCursorVisible(true);
            } catch (Throwable ignored) {}
            try {
                keyboard.stopListening();
            } catch (Throwable ignored) {}
            try {
                if (mouse != null) mouse.close();
            } catch (Throwable ignored) {}
        }));

        keyboard.startListening((deviceHandle, vKey, makeCode, isPressed, isE0, timestamp, keyChar) -> {
            if (isPressed) {
                try {
                    if (!FastTerminal.isTerminalFocused()) {
                        return;
                    }
                } catch (Throwable ignored) {}

                // Handle active overlay dialog key captures
                if (activeDialog != null) {
                    if (vKey == 0x1B) { // ESC -> Cancel
                        activeDialog = null;
                    } else if ("elevenlabs".equals(activeDialog) || "deepgram".equals(activeDialog)) {
                        if (vKey == 0x0D) { // ENTER -> Save
                            String key = dialogInput.toString().trim();
                            if (!key.isEmpty()) {
                                saveApiKey(activeDialog, key);
                            }
                            activeDialog = null;
                        } else if (vKey == 0x08) { // BACKSPACE
                            if (dialogInput.length() > 0) {
                                dialogInput.deleteCharAt(dialogInput.length() - 1);
                            }
                        } else if (keyChar != null && !keyChar.isEmpty()) {
                            char c = keyChar.charAt(0);
                            if (c >= 32 && c != 127) {
                                dialogInput.append(keyChar);
                            }
                        }
                    }
                    return; // Intercept all events when dialog is active
                }

                if (mainPanel.isMinimized()) {
                    return; // ignore keys when minimized
                }

                // Normal main screen key navigation
                if (vKey == 0x26) { // VK_UP
                    selectedVoiceIndex = (selectedVoiceIndex - 1 + voices.size()) % voices.size();
                } else if (vKey == 0x28) { // VK_DOWN
                    selectedVoiceIndex = (selectedVoiceIndex + 1) % voices.size();
                } else if (vKey == 0x08) { // VK_BACK (Backspace)
                    if (textInput.length() > 0) {
                        textInput.deleteCharAt(textInput.length() - 1);
                    }
                } else if (vKey == 0x1B) { // VK_ESCAPE (Exit)
                    isRunning = false;
                } else if (vKey == 0x0D) { // VK_RETURN (Speak or trigger installation)
                    FastTTSVoice selected = voices.get(selectedVoiceIndex);
                    if (!isBackendActive(selected.backendId())) {
                        triggerInstallOrConfig(selected.backendId());
                    } else {
                        triggerSynthesis();
                    }
                } else if (keyChar != null && !keyChar.isEmpty()) {
                    char c = keyChar.charAt(0);
                    if (c >= 32 && c != 127) {
                        textInput.append(keyChar);
                    }
                }
            }
        });

        // 30 FPS Composition Loop
        while (isRunning) {
            long startTime = System.currentTimeMillis();

            // Update cursor visibility based on terminal focus and mouse hover
            boolean shouldHide = false;
            try {
                shouldHide = FastTerminal.isTerminalFocused() && FastTerminal.isMouseOverTerminal();
            } catch (Throwable ignored) {}
            if (shouldHide != lastCursorHiddenState) {
                try {
                    FastTerminal.setSystemCursorVisible(!shouldHide);
                    lastCursorHiddenState = shouldHide;
                } catch (Throwable ignored) {}
            }

            // Handle terminal resizing
            int[] currentSize = FastTerminal.getWindowSize(cols, rows);
            if (renderer.resize(currentSize[0], currentSize[1])) {
                cols = currentSize[0];
                rows = currentSize[1];
                currentCols = cols;
                currentRows = rows;
                canvas.resize(cols, rows);

                // Reposition main panel if it goes out of screen bounds
                if (mainPanel.getX() + mainPanel.getWidth() > cols) {
                    mainPanel.setX(Math.max(0, cols - mainPanel.getWidth()));
                }
                if (mainPanel.getY() + mainPanel.getHeight() > rows) {
                    mainPanel.setY(Math.max(0, rows - mainPanel.getHeight()));
                }
            }

            canvas.clear();

            // Fill canvas with complete pitch-black background
            for (int y = 0; y < rows; y++) {
                for (int x = 0; x < cols; x++) {
                    canvas.writeCell(x, y, ' ', 0x000000, 0x000000);
                }
            }

            // Render main panel
            mainPanel.render(canvas);

            if (mainPanel.isMinimized()) {
                mainPanel.renderDesktopIcon(canvas, 2, rows - 2);
            } else {
                int px = mainPanel.getX();
                int py = mainPanel.getY();
                int pw = mainPanel.getWidth();
                int ph = mainPanel.getHeight();

                // Draw Table Headers inside mainPanel (relativeY = 2)
                String tableHeader = String.format("   %-8s %-12s %-22s %-22s %-10s %s", "TYPE", "ENGINE", "VOICE", "DETAILS", "LATENCY", "STATUS");
                canvas.writeString(px + 1, py + 2, tableHeader, 0xFFFFFF, 0x000000);

                // Draw separating line inside mainPanel
                for (int c = 1; c < pw - 1; c++) {
                    canvas.writeCell(px + c, py + 3, '─', 0x333333, 0x000000);
                }

                // Render voice rows (relativeY = 4 onwards)
                int voiceRowStart = 4;
                int footerSpace = 7; // room for input (3 rows) + status (3 rows) + border (1 row)
                int visibleVoices = ph - voiceRowStart - footerSpace;
                if (visibleVoices < 1) visibleVoices = 1;

                // Viewport calculations centered around selectedVoiceIndex
                int startIndex = Math.max(0, selectedVoiceIndex - visibleVoices / 2);
                int endIndex = Math.min(voices.size(), startIndex + visibleVoices);
                if (endIndex - startIndex < visibleVoices) {
                    startIndex = Math.max(0, endIndex - visibleVoices);
                }

                int drawY = py + voiceRowStart;
                for (int i = startIndex; i < endIndex; i++) {
                    FastTTSVoice v = voices.get(i);
                    boolean active = isBackendActive(v.backendId());
                    String type = isLocal(v) ? "local" : "online";
                    
                    Double latency = voiceLatencies.get(v.id());
                    String latencyStr = "";
                    if (latency != null) {
                        latencyStr = String.format(java.util.Locale.GERMAN, "%.2f ms", latency);
                    }
                    
                    String[] split = splitName(v.name());
                    String name = split[0];
                    String details = split[1];
                    
                    // Format status text
                    String statusField = "";
                    int statusColor = 0xFFFFFF;
                    if (!active) {
                        if ("piper".equals(v.backendId()) || "kokoro".equals(v.backendId())) {
                            statusField = "[INSTALL]";
                            statusColor = 0xF0A500; // Amber
                        } else {
                            statusField = "[CONFIG ]";
                            statusColor = 0x00B4D8; // Blue
                        }
                    } else {
                        statusField = " [READY] ";
                        statusColor = 0x00FF80; // Green
                    }

                    // Format row string to fit panel width dynamically
                    int nameWidth = (int)(pw * 0.25);
                    if (nameWidth < 10) nameWidth = 10;
                    int detailsWidth = (int)(pw * 0.25);
                    if (detailsWidth < 10) detailsWidth = 10;

                    // Pad fields to fit pw
                    String nameFmt = name.length() > nameWidth ? name.substring(0, nameWidth - 3) + "..." : name;
                    nameFmt = String.format("%-" + nameWidth + "s", nameFmt);
                    String detailsFmt = details.length() > detailsWidth ? details.substring(0, detailsWidth - 3) + "..." : details;
                    detailsFmt = String.format("%-" + detailsWidth + "s", detailsFmt);
                    
                    String rowText = String.format(" %-8s %-12s %s %s %-10s ", type, v.backendId(), nameFmt, detailsFmt, latencyStr);

                    int rowBg = 0x000000;
                    int rowFg = 0x999999;
                    if (selectedVoiceIndex == i) {
                        rowBg = active ? 0x00B4D8 : 0x2D2D2D;
                        rowFg = active ? 0x000000 : 0xCCCCCC;
                    } else {
                        rowFg = active ? 0xCCCCCC : 0x555555;
                    }

                    // Clear the row width inside panel
                    for (int c = 1; c < pw - 1; c++) {
                        canvas.writeCell(px + c, drawY, ' ', rowFg, rowBg);
                    }

                    // Write row text
                    canvas.writeString(px + 2, drawY, (selectedVoiceIndex == i ? "-> " : "   ") + rowText, rowFg, rowBg);

                    // Write button status
                    int btnX = px + pw - 18;
                    canvas.writeString(btnX, drawY, statusField, statusColor, rowBg);

                    drawY++;
                }

                // Render blank separator line
                for (int c = 1; c < pw - 1; c++) {
                    canvas.writeCell(px + c, py + ph - 7, '─', 0x333333, 0x000000);
                }

                // Render Text Input (relativeY = ph - 6 and ph - 5)
                canvas.writeString(px + 2, py + ph - 6, "📝 ENTER TEXT TO SYNTHESIZE (Type to edit):", 0xFFFFFF, 0x000000);
                String cursorStr = "█";
                String visibleText = textInput.toString();
                int maxInputLen = pw - 8;
                if (visibleText.length() > maxInputLen) {
                    visibleText = visibleText.substring(visibleText.length() - maxInputLen);
                }
                canvas.writeString(px + 3, py + ph - 5, "> " + visibleText, 0xFFFFFF, 0x000000);
                canvas.writeString(px + 3 + 2 + visibleText.length(), py + ph - 5, cursorStr, 0x00FF80, 0x000000);

                // Render separator line
                for (int c = 1; c < pw - 1; c++) {
                    canvas.writeCell(px + c, py + ph - 4, '─', 0x333333, 0x000000);
                }

                // Render Status (relativeY = ph - 3 and ph - 2)
                canvas.writeString(px + 2, py + ph - 3, "⚡ STATUS:", 0xFFFFFF, 0x000000);
                int statusColorVal = isSynthesizing ? 0xFFA500 : 0x00FF80;
                String statusTruncated = statusMessage.length() > pw - 6 ? statusMessage.substring(0, pw - 9) + "..." : statusMessage;
                canvas.writeString(px + 3, py + ph - 2, statusTruncated, statusColorVal, 0x000000);
            }

            // Render Center float overlay panel dialog if active
            if (activeDialog != null) {
                int dw = 60;
                int dh = 10;
                int dx = Math.max(1, (cols - dw) / 2);
                int dy = Math.max(1, (rows - dh) / 2);
                
                String title = "";
                if ("piper".equals(activeDialog)) title = "INSTALL PIPER BACKEND";
                else if ("kokoro".equals(activeDialog)) title = "INSTALL KOKORO BACKEND";
                else if ("elevenlabs".equals(activeDialog)) title = "CONFIGURE ELEVENLABS API KEY";
                else if ("deepgram".equals(activeDialog)) title = "CONFIGURE DEEPGRAM API KEY";
                
                Panel dialogPanel = new Panel(dx - 1, dy - 1, dw, dh, 0x000000); // Pitch black panel background
                dialogPanel.setHasHeaderBar(true);
                dialogPanel.setBorderStyle(Panel.BorderStyle.DOUBLE);
                dialogPanel.setBorderFg(0xF0A500); // 🟠 fastterminal's classic amber
                dialogPanel.setHeaderBg(0xF0A500); // Amber header
                dialogPanel.setHeaderFg(0x3D1C00); // Deep brown title text
                dialogPanel.setBeosStyle(true);     // 🟠 BeOS style pinstripes
                dialogPanel.setShowWindowButtons(false);
                dialogPanel.setTitle(" " + title + " ");
                dialogPanel.setHasShadow(true);
                dialogPanel.setShadowAlpha(0.25);
                
                dialogPanel.render(canvas);
                
                // Write text content directly onto the canvas body area
                int contentColor = 0xFFFFFF; // White text
                if ("piper".equals(activeDialog) || "kokoro".equals(activeDialog)) {
                    canvas.writeString(dx + 1, dy + 1, "Status: " + dialogStatus, contentColor, 0x000000);
                    
                    int barWidth = 40;
                    int filled = (int) (downloadProgress * barWidth);
                    StringBuilder bar = new StringBuilder("[");
                    for (int b = 0; b < barWidth; b++) {
                        if (b < filled) bar.append("■");
                        else bar.append("□");
                    }
                    bar.append(String.format("] %d%%", (int) (downloadProgress * 100)));
                    canvas.writeString(dx + 1, dy + 3, bar.toString(), contentColor, 0x000000);
                    
                    canvas.writeString(dx + 1, dy + 6, "[ESC] Cancel / Close", 0x94A3B8, 0x000000); // slate-400
                } else {
                    canvas.writeString(dx + 1, dy + 1, "Please enter your API Key:", contentColor, 0x000000);
                    canvas.writeString(dx + 1, dy + 3, "> " + dialogInput.toString() + "█", contentColor, 0x000000);
                    canvas.writeString(dx + 1, dy + 6, "[ENTER] Save & Close      [ESC] Cancel", 0x94A3B8, 0x000000);
                }
            }

            // Draw glowing custom neon mouse pointer on top
            int mx = lastMouseCellX;
            int my = lastMouseCellY;
            if (mx >= 0 && mx < cols && my >= 0 && my < rows) {
                int cursorFg = 0xFFFFFF; // Crisp white cursor body
                int cursorBg = isLeftPressed ? 0xEF4444 : isRightPressed ? 0x3B82F6 : 0x10B981; // Glow matching click state (Red/Blue/Green)
                canvas.writeCellAlpha(mx, my, '↖', cursorFg, cursorBg, 1.0, 0.4);
            }

            // Render to terminal
            renderer.render();

            long elapsed = System.currentTimeMillis() - startTime;
            long sleepTime = (1000 / 30) - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ignored) {}
            }
        }

        // Clean shutdown exit
        System.exit(0);
    }

    private static void initializeVoicesList() {
        voices = new java.util.ArrayList<>();
        
        // 1. Fetch Windows system voices
        try {
            voices.addAll(new WindowsTTSBackend().getVoices());
        } catch (Exception ignored) {}
        
        // 2. Add standard Piper voice
        voices.add(new FastTTSVoice("thorsten.onnx", "Thorsten (German Male)", "de_DE", "male", "piper"));
        
        // 3. Add Kokoro voices
        voices.add(new FastTTSVoice("af", "Bella (US Female)", "en_US", "female", "kokoro"));
        voices.add(new FastTTSVoice("af_sky", "Sky (US Female)", "en_US", "female", "kokoro"));
        voices.add(new FastTTSVoice("am_adam", "Adam (US Male)", "en_US", "male", "kokoro"));
        voices.add(new FastTTSVoice("am_michael", "Michael (US Male)", "en_US", "male", "kokoro"));
        voices.add(new FastTTSVoice("bf_isabella", "Isabella (UK Female)", "en_UK", "female", "kokoro"));
        voices.add(new FastTTSVoice("bm_george", "George (UK Male)", "en_UK", "male", "kokoro"));
        
        // 4. Add ElevenLabs voice
        voices.add(new FastTTSVoice("rachel", "Rachel (ElevenLabs)", "en_US", "female", "elevenlabs"));
        
        // 5. Add Deepgram voice
        voices.add(new FastTTSVoice("aura-asteria-en", "Aura Asteria (Deepgram)", "en_US", "female", "deepgram"));
    }

    private static boolean isBackendActive(String backendId) {
        String bid = backendId.toLowerCase();
        if ("windows".equals(bid)) {
            return true;
        } else if ("piper".equals(bid)) {
            String path = PathResolver.resolve("piper.path", "piper.exe");
            return new File(path).exists();
        } else if ("kokoro".equals(bid)) {
            String path = PathResolver.resolve("kokoro.model", "kokoro-v0_19.onnx");
            return new File(path).exists();
        } else if ("elevenlabs".equals(bid)) {
            String elKey = PathResolver.getProperty("elevenlabs.api.key");
            return elKey != null && !elKey.isEmpty();
        } else if ("deepgram".equals(bid)) {
            String dgKey = PathResolver.getProperty("deepgram.api.key");
            return dgKey != null && !dgKey.isEmpty();
        }
        return false;
    }

    private static void registerElevenLabsIfConfigured() {
        String elKey = PathResolver.getProperty("elevenlabs.api.key");
        if (elKey != null && !elKey.isEmpty()) {
            tts.registerBackend(new ElevenLabsBackend(elKey, PathResolver.getProperty("elevenlabs.default.voice"), 0.5f, 0.75f));
        }
    }

    private static void registerDeepgramIfConfigured() {
        String dgKey = PathResolver.getProperty("deepgram.api.key");
        if (dgKey != null && !dgKey.isEmpty()) {
            tts.registerBackend(new DeepgramBackend(dgKey));
        }
    }

    private static void registerPiperIfInstalled() {
        String piperPath = PathResolver.resolve("piper.path", "piper.exe");
        String piperModel = PathResolver.resolve("piper.model", "thorsten.onnx");
        if (new File(piperPath).exists()) {
            tts.registerBackend(new PiperBackend(piperPath, piperModel));
        }
    }

    private static void registerKokoroIfInstalled() {
        String kokoroModel = PathResolver.resolve("kokoro.model", "kokoro-v0_19.onnx");
        if (new File(kokoroModel).exists()) {
            try {
                tts.registerBackend(new KokoroBackend(kokoroModel));
            } catch (Exception e) {
                statusMessage = "[Kokoro Error] Load fail: " + e.getMessage();
            }
        }
    }

    private static void saveApiKey(String backend, String key) {
        Properties newProps = new Properties();
        File propFile = new File(PathResolver.getInstallDir(), "fasttts.properties");
        if (propFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propFile)) {
                newProps.load(fis);
            } catch (Exception ignored) {}
        }
        if ("elevenlabs".equals(backend)) {
            newProps.setProperty("elevenlabs.api.key", key);
            PathResolver.saveProperties(newProps);
            registerElevenLabsIfConfigured();
        } else if ("deepgram".equals(backend)) {
            newProps.setProperty("deepgram.api.key", key);
            PathResolver.saveProperties(newProps);
            registerDeepgramIfConfigured();
        }
        statusMessage = "Saved " + backend.toUpperCase() + " API Key successfully.";
    }

    private static void triggerInstallOrConfig(String backendId) {
        if ("piper".equals(backendId)) {
            activeDialog = "piper";
            dialogStatus = "Initializing download...";
            downloadProgress = 0.0;
            startPiperInstallation();
        } else if ("kokoro".equals(backendId)) {
            activeDialog = "kokoro";
            dialogStatus = "Initializing download...";
            downloadProgress = 0.0;
            startKokoroInstallation();
        } else if ("elevenlabs".equals(backendId) || "deepgram".equals(backendId)) {
            activeDialog = backendId;
            dialogInput.setLength(0);
        }
    }

    private static void startPiperInstallation() {
        new Thread(() -> {
            try {
                File installDir = new File(PathResolver.getInstallDir(), "piper");
                installDir.mkdirs();
                
                // 1. Download ZIP
                dialogStatus = "Downloading piper_windows_amd64.zip...";
                File zipFile = new File(installDir, "piper.zip");
                downloadFile("https://github.com/rhasspy/piper/releases/download/2023.11.14-2/piper_windows_amd64.zip", zipFile);
                
                // 2. Extract
                dialogStatus = "Extracting piper binaries...";
                downloadProgress = 0.5;
                unzip(zipFile, installDir);
                zipFile.delete();
                
                // 3. Download Thorsten Voice ONNX
                dialogStatus = "Downloading Thorsten Voice ONNX...";
                File modelFile = new File(installDir, "thorsten.onnx");
                downloadFile("https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/de/de_DE/thorsten/low/de_DE-thorsten-low.onnx", modelFile);
                
                // 4. Download Thorsten Voice ONNX Json
                dialogStatus = "Downloading Thorsten Voice Config...";
                File modelJson = new File(installDir, "thorsten.onnx.json");
                downloadFile("https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/de/de_DE/thorsten/low/de_DE-thorsten-low.onnx.json", modelJson);
                
                dialogStatus = "Piper offline installation complete!";
                downloadProgress = 1.0;
                Thread.sleep(1000);
                
                registerPiperIfInstalled();
                statusMessage = "Piper offline backend installed and registered.";
            } catch (Exception e) {
                statusMessage = "Piper install failed: " + e.getMessage();
            } finally {
                activeDialog = null;
            }
        }).start();
    }

    private static void startKokoroInstallation() {
        new Thread(() -> {
            try {
                File installDir = new File(PathResolver.getInstallDir(), "kokoro");
                installDir.mkdirs();
                
                // 1. Download Model ONNX
                dialogStatus = "Downloading Kokoro v0.19 ONNX model...";
                File modelFile = new File(installDir, "kokoro-v0_19.onnx");
                downloadFile("https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files/kokoro-v0_19.int8.onnx", modelFile);
                
                // 2. Download voices.bin
                dialogStatus = "Downloading Kokoro voices.bin...";
                File voicesFile = new File(installDir, "voices.bin");
                downloadFile("https://github.com/thewh1teagle/kokoro-onnx/releases/download/model-files/voices.bin", voicesFile);
                
                dialogStatus = "Kokoro native installation complete!";
                downloadProgress = 1.0;
                Thread.sleep(1000);
                
                registerKokoroIfInstalled();
                statusMessage = "Kokoro native backend installed and registered.";
            } catch (Exception e) {
                statusMessage = "Kokoro install failed: " + e.getMessage();
            } finally {
                activeDialog = null;
            }
        }).start();
    }

    private static void downloadFile(String fileUrl, File outputFile) throws IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        int fileSize = conn.getContentLength();
        try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] data = new byte[4096];
            int count;
            int downloaded = 0;
            while ((count = in.read(data, 0, 4096)) != -1) {
                if (activeDialog == null) {
                    throw new IOException("Download cancelled by user.");
                }
                downloaded += count;
                fos.write(data, 0, count);
                if (fileSize > 0) {
                    downloadProgress = (double) downloaded / fileSize;
                }
            }
        }
    }

    private static void unzip(File zipFile, File destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith("piper/")) {
                    name = name.substring(6);
                }
                if (name.isEmpty()) {
                    continue;
                }
                File file = new File(destDir, name);
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) != -1) {
                            bos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
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
            statusMessage = "Generating audio using " + selected.backendId().toUpperCase() + "...";
            
            long startTime = System.nanoTime();
            try {
                FastTTSAudio audio = tts.speak(selected.backendId(), textInput.toString(), selected, null);
                long endTime = System.nanoTime();
                double durationMs = (endTime - startTime) / 1_000_000.0;
                
                if (audio != null) {
                    voiceLatencies.put(selected.id(), durationMs);
                    statusMessage = "Playing audio...";
                    
                    try (java.io.FileWriter fw = new java.io.FileWriter(PathResolver.getInstallDir() + "/latency-results.log", true)) {
                        String text = textInput.toString();
                        String snippet = text.length() > 30 ? text.substring(0, 27) + "..." : text;
                        fw.write(String.format("[%s] Latency: %.2f ms | Text: %s\n", 
                            selected.backendId().toUpperCase(), durationMs, snippet));
                    } catch (Exception ignored) {}

                    playAudio(audio);
                    
                    statusMessage = "Speech generated successfully. Press ENTER to repeat.";
                } else {
                    statusMessage = "Error: No audio generated.";
                }
            } catch (Exception e) {
                statusMessage = "Synthesis failed: " + e.getMessage();
            } finally {
                isSynthesizing = false;
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
            statusMessage = "Playback Error: " + e.getMessage();
        }
    }
}
