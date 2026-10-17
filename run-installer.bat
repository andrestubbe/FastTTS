@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

if not exist "target\FastTTS-0.1.4.jar" (
    echo [FastTTS] Building core library...
    call mvn clean install -DskipTests -q
)

if not exist "examples\Installer\target\classes\fasttts\installer\FastTTSInstaller.class" (
    echo [FastTTS] Compiling Installer...
    call mvn -f examples\Installer\pom.xml compile -q
)

set CP=examples\Installer\target\classes;target\FastTTS-0.1.4.jar;target\classes;%USERPROFILE%\.m2\repository\com\github\andrestubbe\FastAudioPlayer\0.1.2\FastAudioPlayer-0.1.2.jar;%USERPROFILE%\.m2\repository\com\github\andrestubbe\FastCore\0.1.1\FastCore-0.1.1.jar;%USERPROFILE%\.m2\repository\com\github\andrestubbe\FastCore\0.1.0\FastCore-0.1.0.jar

java -Djava.library.path="bin;build;dll" -cp "%CP%" fasttts.installer.FastTTSInstaller %*
