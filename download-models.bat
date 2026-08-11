@echo off
echo ========================================
echo FastTTS Setup - Download Required Files
echo ========================================
echo.
echo This script will help you download:
echo 1. Piper German Model (de_DE-thorsten-medium)
echo 2. eSpeak-NG (phonemizer)
echo.
echo ========================================
echo.
echo STEP 1: Piper Model Download
echo.
echo Choose download method:
echo 1. Manual download (recommended - more reliable)
echo 2. PowerShell download (automatic)
echo.
set /p choice="Enter choice (1 or 2): "

if "%choice%"=="1" goto manual
if "%choice%"=="2" goto automatic

:manual
echo.
echo ========================================
echo MANUAL DOWNLOAD INSTRUCTIONS
echo ========================================
echo.
echo 1. Visit: https://huggingface.co/Trelis/piper-de-de-thorsten-medium/tree/main
echo 2. Download these files:
echo    - model.onnx (~63 MB)
echo    - model.onnx.json (config file)
echo 3. Place them in: C:\Users\andre\Documents\2026-06-14-Work-FastJava\FastTTS
echo 4. Rename them to:
echo    - de_DE-thorsten-medium.onnx
echo    - de_DE-thorsten-medium.onnx.json
echo.
echo Press any key to continue to eSpeak-NG setup...
pause
goto espeak

:automatic
echo.
echo ========================================
echo AUTOMATIC DOWNLOAD
echo ========================================
echo.
echo Downloading model.onnx (~63 MB)...
powershell -Command "Invoke-WebRequest -Uri 'https://huggingface.co/Trelis/piper-de-de-thorsten-medium/resolve/main/model.onnx' -OutFile 'de_DE-thorsten-medium.onnx'"

if %errorlevel% neq 0 (
    echo Failed to download model.onnx
    echo Trying alternative method...
    goto manual
)

echo Downloading model.onnx.json...
powershell -Command "Invoke-WebRequest -Uri 'https://huggingface.co/Trelis/piper-de-de-thorsten-medium/resolve/main/model.onnx.json' -OutFile 'de_DE-thorsten-medium.onnx.json'"

if %errorlevel% neq 0 (
    echo Failed to download model.onnx.json
    echo Trying alternative method...
    goto manual
)

echo.
echo Model download complete!
echo Files: de_DE-thorsten-medium.onnx, de_DE-thorsten-medium.onnx.json
echo.

:espeak
echo.
echo ========================================
echo STEP 2: eSpeak-NG Setup
echo ========================================
echo.
echo eSpeak-NG is required for phonemization (text to IPA phonemes).
echo.
echo DOWNLOAD INSTRUCTIONS:
echo.
echo 1. Visit: https://github.com/espeak-ng/espeak-ng/releases
echo 2. Download the latest Windows release (.msi installer)
echo 3. Run the installer
echo 4. Find espeak-ng.exe in the installation directory
echo    (typically: C:\Program Files\eSpeak NG\espeak-ng.exe)
echo 5. Copy espeak-ng.exe to: C:\Users\andre\Documents\2026-06-14-Work-FastJava\FastTTS
echo.
echo Alternatively, you can use the installed path directly in the demo
echo by modifying the espeakPath variable.
echo.
echo ========================================
echo SETUP COMPLETE
echo ========================================
echo.
echo Once you have both files in place, you can run:
echo - run-piper-demo.bat (for ONNX demo)
echo - run-piper-benchmark.bat (for performance comparison)
echo.
pause
