@echo off
:: FastJava Native DLL Compiler Script
:: Auto-detects Visual Studio and JAVA_HOME

echo ========================================
echo FastXXX Native Library Builder
echo ========================================

:: Configuration
set LIB_NAME=fasttts

:: Try to find VS using vswhere.exe (most reliable)
set "VSWHERE=%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe"
if exist "%VSWHERE%" (
    for /f "usebackq tokens=*" %%i in (`"%VSWHERE%" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath`) do (
        set "VS_PATH=%%i"
    )
)

:: Fallback: Check standard paths if vswhere didn't work
if not defined VS_PATH (
    if exist "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat" (
        set "VS_PATH=C:\Program Files\Microsoft Visual Studio\2022\Community"
    ) else if exist "C:\Program Files\Microsoft Visual Studio\2022\Enterprise\VC\Auxiliary\Build\vcvars64.bat" (
        set "VS_PATH=C:\Program Files\Microsoft Visual Studio\2022\Enterprise"
    ) else if exist "C:\Program Files\Microsoft Visual Studio\2022\Professional\VC\Auxiliary\Build\vcvars64.bat" (
        set "VS_PATH=C:\Program Files\Microsoft Visual Studio\2022\Professional"
    ) else if exist "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat" (
        set "VS_PATH=C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools"
    ) else if exist "C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build\vcvars64.bat" (
        set "VS_PATH=C:\Program Files (x86)\Microsoft Visual Studio\2019\Community"
    )
)

if not defined VS_PATH (
    echo ERROR: Visual Studio not found!
    echo Please install Visual Studio 2019 or 2022 with "Desktop development with C++"
    exit /b 1
)

echo Found Visual Studio at: %VS_PATH%

:: Try to detect JAVA_HOME if not set
if not defined JAVA_HOME (
    if exist "C:\Program Files\Java\jdk-25" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-25"
    ) else if exist "C:\Program Files\Eclipse Adoptium\jdk-17-hotspot" (
        set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17-hotspot"
    ) else if exist "C:\Program Files\Java\jdk-17" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-17"
    )
)

if not defined JAVA_HOME (
    echo ERROR: JAVA_HOME not set!
    exit /b 1
)

echo Using JAVA_HOME: %JAVA_HOME%

:: Setup environment
call "%VS_PATH%\VC\Auxiliary\Build\vcvars64.bat"

:: Create build directory
if not exist build mkdir build

:: Compile C++ source
cl.exe /O2 /W3 /MD /EHsc /LD ^
   /I "%JAVA_HOME%\include" ^
   /I "%JAVA_HOME%\include\win32" ^
   /Fo:build\ ^
   /Fe:build\%LIB_NAME%.dll ^
   native\*.cpp ^
   user32.lib gdi32.lib shcore.lib advapi32.lib dwmapi.lib ole32.lib ^
   /link /DLL /MACHINE:X64 /DEF:native\%LIB_NAME%.def

if %ERRORLEVEL% == 0 (
    echo.
    echo [SUCCESS] DLL built at: build\%LIB_NAME%.dll
    :: Optional: copy to resources if needed
    :: copy build\%LIB_NAME%.dll src\main\resources\native\
) else (
    echo.
    echo [FAILED] Compilation failed.
    exit /b 1
)

echo.
pause
