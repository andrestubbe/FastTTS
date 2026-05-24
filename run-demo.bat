@echo off
chcp 65001 > nul
echo [FastTTS] Rebuilding Core...
call mvn -q clean install -DskipTests
if %ERRORLEVEL% NEQ 0 ( echo Core build failed. & pause & exit /b )

echo [FastTTS] Compiling Demo...
pushd examples\Demo
call mvn -q compile dependency:copy-dependencies -DincludeScope=runtime -DskipTests
if %ERRORLEVEL% NEQ 0 ( popd & echo Demo compile failed. & pause & exit /b )

echo [FastTTS] Launching Demo UI...
java -Dfile.encoding=UTF-8 --enable-native-access=ALL-UNNAMED -cp "target/classes;target/dependency/*" fasttts.Demo
popd
