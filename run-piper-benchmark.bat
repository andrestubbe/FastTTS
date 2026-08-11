@echo off
echo Building Piper Benchmark...
cd examples\PiperBenchmark
call mvn clean package
cd ..\..
echo.
echo Running Piper Benchmark...
echo.
echo IMPORTANT: Make sure you have:
echo 1. A Piper ONNX model (e.g., de_DE-thorsten-medium.onnx) in the current directory
echo 2. piper.exe in the current directory
echo 3. espeak-ng.exe in the current directory
echo 4. The corresponding model.onnx.json config file
echo.
pause
java -cp examples\PiperBenchmark\target\PiperBenchmark-1.0.0.jar fasttts.PiperBenchmark
pause
