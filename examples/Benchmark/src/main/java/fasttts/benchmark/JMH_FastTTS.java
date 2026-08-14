package fasttts.benchmark;
import fasttts.FastTTS;
import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class JMH_TTS {
    private FastTTS tts;
    @Setup public void setup() { tts = FastTTS.createWindows(); }
    @Benchmark public byte[] benchmarkSynthesize() { return tts.synthesize("FastJava SIMD Text to Speech"); }
}