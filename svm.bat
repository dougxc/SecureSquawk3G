@echo off
REM This file is used to build squawk and run a simple test.

del squawk.exe

echo Building squawk ...
java -jar build.jar -debug -diag  

echo Romizing ..
java -jar build.jar -prod -o2 rom -prune:d -tracesvm -tracesvmload j2me translator

echo Running test...
squawk -cp:../myTests -tracesvm P.X