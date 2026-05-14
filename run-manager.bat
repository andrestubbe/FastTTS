@echo off
echo [FastTTS] Starting Engine Manager...
cd examples/Manager
mvn compile exec:java -q -Dexec.mainClass="fasttts.FastTTSManager"
cd ../..
