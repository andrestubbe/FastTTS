package fasttts.benchmark;

import fasttts.FastTTS;
import fasttts.backends.windows.WindowsTTSBackend;
import fasttts.core.FastTTSAudio;
import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class JMH_FastTTS {
    private FastTTS tts;

    @Setup
    public void setup() throws Exception {
        tts = new FastTTS();
        tts.registerBackend(new WindowsTTSBackend());
    }

    @Benchmark
    public FastTTSAudio benchmarkSynthesize() throws Exception {
        return tts.speak("FastJava SIMD Text to Speech");
    }
}