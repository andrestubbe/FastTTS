@echo off
echo [FastTTS] Starting Demo Example...
pushd examples\Demo
mvn exec:java -Dexec.mainClass="fasttts.Demo"
popd
cd ..
cd ..
