@echo off
call compile.bat
call mvn clean package -DskipTests -q
cd examples\Benchmark
call mvn clean package -DskipTests -q
java -cp "target\benchmarks.jar;..\..\target\FastTTS-0.1.2.jar;%USERPROFILE%\.m2\repository\com\github\andrestubbe\FastCore\0.1.0\FastCore-0.1.0.jar;%USERPROFILE%\.m2\repository\com\github\andrestubbe\fastcore\0.1.0\fastcore-0.1.0.jar" org.openjdk.jmh.Main -f 1 -i 2 -wi 1 -w 1s -r 1s
cd ..\..
pause