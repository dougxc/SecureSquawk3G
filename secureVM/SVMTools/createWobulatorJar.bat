@echo off
REM This batch file is used to create the wobulator jar
echo Building Wobulator JAR file
cd classes
jar -cfm ../wobulator.jar ../mainClass .
cd ../../bcel-5.1/classes
jar uf ../../SVMTools/wobulator.jar .