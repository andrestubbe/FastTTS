package fasttts.demo;
import fasttts.FastTTS;

public class Demo {
    public static void main(String[] args) {
        System.out.println("--- FastTTS 0.1.2 Demo ---");
        try {
            FastTTS tts = FastTTS.createWindows();
            byte[] wav = tts.synthesize("FastJava SIMD Hardware Vector Synthesis Engine 2026!");
            System.out.println("Synthesized WAV size: " + (wav != null ? wav.length : 0) + " bytes");
            System.out.println("✔ FastTTS demo completed.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}