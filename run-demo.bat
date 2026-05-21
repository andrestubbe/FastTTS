@echo off
echo [FastTTS] Rebuilding and Starting Demo (Quiet Mode)...
call mvn install -DskipTests -q
pushd examples\Demo
call mvn compile exec:java -Dexec.mainClass="fasttts.Demo" -q
popd
