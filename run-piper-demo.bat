@echo off
echo Building PiperONNX Demo...
cd examples\PiperONNXDemo
call mvn clean package -q
cd ..\..
echo.
echo Running PiperONNX Demo...
echo.
echo IMPORTANT: Make sure you have:
echo 1. A Piper ONNX model (e.g., de_DE-thorsten-medium.onnx) in the current directory
echo 2. espeak-ng.exe installed (or in current directory)
echo 3. The corresponding model.onnx.json config file
echo.
pause
java -jar examples\PiperONNXDemo\target\PiperONNXDemo-1.0.0.jar
pause
