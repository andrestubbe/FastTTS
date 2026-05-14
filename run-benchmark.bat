@echo off
echo [FastTTS] Starting Performance Benchmark...
pushd examples\Benchmark
mvn exec:java -Dexec.mainClass="fasttts.Benchmark"
popd
pause
