@echo off
setlocal

if not exist "examples\Demo\target\classes\fasttts\demo\Demo.class" (
    echo [FastTTS] Compiling examples/Demo...
    call mvn -f examples\Demo\pom.xml compile -q
)

set CP=examples\Demo\target\classes;target\FastTTS-0.1.4.jar;target\FastTTS-0.1.3.jar;target\classes;%USERPROFILE%\.m2\repository\com\github\andrestubbe\FastAudioPlayer\0.1.2\FastAudioPlayer-0.1.2.jar;%USERPROFILE%\.m2\repository\com\github\andrestubbe\FastCore\0.1.1\FastCore-0.1.1.jar;%USERPROFILE%\.m2\repository\com\github\andrestubbe\FastCore\0.1.0\FastCore-0.1.0.jar

if "%~1"=="" (
    echo [FastTTS] Starte Standard-Kokoro Demo...
    echo.
    java -Djava.library.path="bin;build;dll" -cp "%CP%" fasttts.demo.Demo kokoro "Hello from FastTTS! Kokoro offline studio neural speech is working." af_bella
) else (
    java -Djava.library.path="bin;build;dll" -cp "%CP%" fasttts.demo.Demo %*
)