@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

set "VSWHERE=%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe"
if not exist "%VSWHERE%" set "VSWHERE=%ProgramFiles%\Microsoft Visual Studio\Installer\vswhere.exe"

if exist "%VSWHERE%" (
    for /f "usebackq tokens=*" %%i in (`"%VSWHERE%" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath`) do (
        set "VS_PATH=%%i"
    )
)

if not defined VS_PATH (
    if exist "C:\Program Files\Microsoft Visual Studio\18\Community" set "VS_PATH=C:\Program Files\Microsoft Visual Studio\18\Community"
)

if not defined VS_PATH (
    echo [ERROR] Visual Studio with C++ tools not found!
    exit /b 1
)

if not defined JAVA_HOME (
    if exist "C:\Program Files\Java\jdk-21.0.12.1" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.12.1"
    ) else if exist "C:\Program Files\Java\latest" (
        set "JAVA_HOME=C:\Program Files\Java\latest"
    ) else if exist "C:\Program Files\Java\jdk-25.0.3" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.3"
    ) else if exist "C:\Program Files\Java\jdk-21" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    ) else if exist "C:\Program Files\Java\jdk-17" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-17"
    )
)

if not exist "!JAVA_HOME!\include\jni.h" (
    echo [ERROR] Cannot find jni.h in !JAVA_HOME!\include
    exit /b 1
)

call "!VS_PATH!\VC\Auxiliary\Build\vcvars64.bat"

if not exist "build" mkdir build > nul 2>&1
if not exist "release" mkdir release > nul 2>&1
if not exist "src\main\resources\native" mkdir "src\main\resources\native" > nul 2>&1
if not exist "src\main\resources\win32-x86-64" mkdir "src\main\resources\win32-x86-64" > nul 2>&1
if not exist "target\classes\native" mkdir "target\classes\native" > nul 2>&1
set "FASTCORE_DIR=%USERPROFILE%\.fastcore\native\fasttts"
if not exist "!FASTCORE_DIR!" mkdir "!FASTCORE_DIR!" > nul 2>&1

cl.exe /nologo /O2 /arch:AVX2 /EHsc /std:c++17 /MD /LD /D_CRT_SECURE_NO_WARNINGS ^
    /I"!JAVA_HOME!\include" ^
    /I"!JAVA_HOME!\include\win32" ^
    native\fasttts.cpp ^
    /Fo:build\fasttts.obj ^
    /link /DLL /OUT:release\fasttts.dll user32.lib gdi32.lib shcore.lib advapi32.lib dwmapi.lib ole32.lib oleaut32.lib

if errorlevel 1 (
    echo [ERROR] Compilation failed!
    exit /b 1
)

copy /Y release\fasttts.dll build\fasttts.dll > nul
copy /Y release\fasttts.dll src\main\resources\fasttts.dll > nul 2>&1
copy /Y release\fasttts.dll src\main\resources\native\fasttts.dll > nul
copy /Y release\fasttts.dll src\main\resources\win32-x86-64\fasttts.dll > nul
copy /Y release\fasttts.dll target\classes\native\fasttts.dll > nul 2>&1
copy /Y release\fasttts.dll target\classes\win32-x86-64\fasttts.dll > nul 2>&1
copy /Y release\fasttts.dll "!FASTCORE_DIR!\fasttts.dll" > nul
powershell -NoProfile -Command "Unblock-File -Path '!FASTCORE_DIR!\fasttts.dll', 'release\fasttts.dll', 'src\main\resources\native\fasttts.dll' -ErrorAction SilentlyContinue" > nul 2>&1

echo.
echo ===========================================
echo [SUCCESS] FastTTS native DLL built!
echo Copied to release\, resources\, and .fastcore
echo ===========================================
