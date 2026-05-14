@echo off
echo [FastTTS] Starting Manager...
pushd examples\Manager
mvn exec:java -Dexec.mainClass="fasttts.FastTTSManager"
popd
