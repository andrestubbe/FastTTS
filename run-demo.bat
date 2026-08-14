@echo off
echo [FastTTS] Building Native Library...
call compile.bat
call mvn clean package -DskipTests -q
cd examples\Demo
call mvn package -DskipTests -q
java -cp "target\demo-0.1.2.jar;..\..\target\FastTTS-0.1.2.jar;%USERPROFILE%\.m2\repository\com\github\andrestubbe\FastCore\0.1.0\FastCore-0.1.0.jar;%USERPROFILE%\.m2\repository\com\github\andrestubbe\fastcore\0.1.0\fastcore-0.1.0.jar" fasttts.demo.Demo
cd ..\..
pause