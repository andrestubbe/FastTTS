@echo off
setlocal
chcp 65001 > nul
cd /d "%~dp0"

echo ===================================================
echo  Building FastTTS Native & Main Project
echo ===================================================
call compile.bat
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Native build failed!
    pause
    exit /b %ERRORLEVEL%
)

call mvn clean install -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] FastTTS install failed!
    pause
    exit /b %ERRORLEVEL%
)

echo ===================================================
echo  Building JMH Benchmark Uber-JAR
echo ===================================================
cd examples\Benchmark
call mvn clean package -DskipTests -q
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Benchmark build failed!
    cd ..\..
    pause
    exit /b %ERRORLEVEL%
)

echo ===================================================
echo  Running JMH Microbenchmarks for FastTTS
echo ===================================================
java --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED -jar target\benchmarks.jar -jvmArgs "-Xmx4g"

cd ..\..
pause