@echo off
echo [FastTTS] Starting Installer...
pushd examples\Installer
mvn exec:java -Dexec.mainClass="fasttts.FastTTSInstaller"
popd
