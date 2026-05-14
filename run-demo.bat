@echo off
echo [FastTTS] Starting Demo Example...
cd examples/Demo
mvn compile exec:java -q -Dexec.mainClass="fasttts.Demo"
cd ../..
