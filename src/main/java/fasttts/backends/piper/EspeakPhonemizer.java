package fasttts.backends.piper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * eSpeak-NG Phonemizer (process-based).
 * Converts text to eSpeak phonemes (not IPA) for Piper compatibility.
 */
public class EspeakPhonemizer {
    
    private String espeakPath = "espeak-ng.exe";
    private String voice = "de";
    
    public EspeakPhonemizer() {}
    
    public EspeakPhonemizer(String espeakPath, String voice) {
        this.espeakPath = espeakPath;
        this.voice = voice;
    }
    
    /**
     * Convert text to eSpeak phonemes (phonemes, not IPA).
     * Piper expects eSpeak phonemes, not IPA.
     */
    public String textToPhonemes(String text) throws Exception {
        if (!new File(espeakPath).exists()) {
            throw new FileNotFoundException("espeak-ng.exe not found at: " + new File(espeakPath).getAbsolutePath());
        }
        
        // Use -x flag for phonemes (not IPA), and -q for quiet mode
        ProcessBuilder pb = new ProcessBuilder(
            espeakPath,
            "-v", voice,
            "-x",  // phonemes (not IPA)
            "-q",  // quiet mode
            text
        );
        
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process p = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        if (p.waitFor() != 0) {
            throw new RuntimeException("eSpeak-NG execution failed.");
        }
        
        return output.toString().trim();
    }
    
    /**
     * Check if eSpeak-NG is available.
     */
    public boolean isAvailable() {
        return new File(espeakPath).exists();
    }
    
    public void setEspeakPath(String path) {
        this.espeakPath = path;
    }
    
    public void setVoice(String voice) {
        this.voice = voice;
    }
}
