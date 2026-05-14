@echo off
echo [FastTTS] Starting Performance Benchmark...
cd examples/Benchmark
mvn compile exec:java -q -Dexec.mainClass="fasttts.Benchmark"
cd ../..
pause
