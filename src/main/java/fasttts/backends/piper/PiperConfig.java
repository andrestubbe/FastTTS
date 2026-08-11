package fasttts.backends.piper;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Piper config.json parser.
 * Extracts phoneme-to-ID mapping and sample rate.
 */
public class PiperConfig {
    
    private Map<String, Integer> phonemeIdMap = new HashMap<>();
    private int sampleRate = 22050;
    
    public PiperConfig(String configPath) throws IOException {
        parseConfig(configPath);
    }
    
    private void parseConfig(String configPath) throws IOException {
        String content = Files.readString(Paths.get(configPath));
        
        // Parse sample rate
        java.util.regex.Matcher rateMatcher = java.util.regex.Pattern.compile("\"sample_rate\"\\s*:\\s*(\\d+)").matcher(content);
        if (rateMatcher.find()) {
            sampleRate = Integer.parseInt(rateMatcher.group(1));
        }
        
        // Parse phoneme-to-ID mapping
        // Looking for phoneme_id_map or similar structure
        java.util.regex.Matcher phonemeMatcher = java.util.regex.Pattern.compile("\"phoneme_id_map\"\\s*:\\s*\\{([^}]+)\\}").matcher(content);
        if (phonemeMatcher.find()) {
            String mapContent = phonemeMatcher.group(1);
            // Parse individual phoneme: id pairs
            java.util.regex.Pattern pairPattern = java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\\d+)");
            java.util.regex.Matcher pairMatcher = pairPattern.matcher(mapContent);
            
            while (pairMatcher.find()) {
                String phoneme = pairMatcher.group(1);
                int id = Integer.parseInt(pairMatcher.group(2));
                phonemeIdMap.put(phoneme, id);
            }
        }
        
        // Alternative: Try to parse phoneme list if no explicit map
        if (phonemeIdMap.isEmpty()) {
            java.util.regex.Matcher listMatcher = java.util.regex.Pattern.compile("\"phonemes\"\\s*:\\s*\\[([^\\]]+)\\]").matcher(content);
            if (listMatcher.find()) {
                String listContent = listMatcher.group(1);
                String[] phonemes = listContent.split(",");
                for (int i = 0; i < phonemes.length; i++) {
                    String phoneme = phonemes[i].trim().replaceAll("\"", "");
                    if (!phoneme.isEmpty()) {
                        phonemeIdMap.put(phoneme, i);
                    }
                }
            }
        }
    }
    
    /**
     * Convert IPA phoneme string to ID array.
     */
    public long[] phonemesToIds(String ipaPhonemes) {
        // Split by space first, then split into individual phonemes
        String[] wordPhonemes = ipaPhonemes.split("\\s+");
        System.out.println("DEBUG: Raw phoneme array: " + java.util.Arrays.toString(wordPhonemes));
        
        // Collect individual phonemes
        java.util.List<Long> idList = new java.util.ArrayList<>();
        
        for (String wordPhoneme : wordPhonemes) {
            wordPhoneme = wordPhoneme.trim();
            System.out.println("DEBUG: Processing word phoneme: '" + wordPhoneme + "'");
            
            // Split into individual phonemes (eSpeak format uses stress markers as separate)
            java.util.List<String> individualPhonemes = splitIntoIndividualPhonemes(wordPhoneme);
            System.out.println("DEBUG: Individual phonemes: " + individualPhonemes);
            
            for (String phoneme : individualPhonemes) {
                Integer id = phonemeIdMap.get(phoneme);
                if (id != null) {
                    idList.add((long) id);
                    System.out.println("DEBUG: Mapped '" + phoneme + "' -> " + id);
                } else {
                    // Try without stress markers
                    String cleaned = phoneme.replaceAll("[ˌˈːˑ'`_]", "");
                    if (!cleaned.isEmpty()) {
                        id = phonemeIdMap.get(cleaned);
                        if (id != null) {
                            idList.add((long) id);
                            System.out.println("DEBUG: Mapped cleaned '" + cleaned + "' -> " + id);
                        } else {
                            System.out.println("DEBUG: Not found: '" + phoneme + "' (cleaned: '" + cleaned + "')");
                            idList.add(0L); // fallback
                        }
                    } else {
                        idList.add(0L); // was just a marker
                    }
                }
            }
        }
        
        long[] ids = new long[idList.size()];
        for (int i = 0; i < idList.size(); i++) {
            ids[i] = idList.get(i);
        }
        
        return ids;
    }
    
    /**
     * Split eSpeak phoneme string into individual phonemes.
     * eSpeak format: h'alo: -> [h, ', a, l, o, :]
     */
    private java.util.List<String> splitIntoIndividualPhonemes(String phonemeString) {
        java.util.List<String> phonemes = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (char c : phonemeString.toCharArray()) {
            // Stress markers and special chars are separate phonemes
            if (c == '\'' || c == ',' || c == '_' || c == '|' || c == 'ˌ' || c == 'ˈ' || c == 'ː' || c == 'ˑ') {
                if (current.length() > 0) {
                    phonemes.add(current.toString());
                    current = new StringBuilder();
                }
                phonemes.add(String.valueOf(c));
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            phonemes.add(current.toString());
        }
        
        return phonemes;
    }
    
    public int getSampleRate() {
        return sampleRate;
    }
    
    public Map<String, Integer> getPhonemeIdMap() {
        return phonemeIdMap;
    }
}
