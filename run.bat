@echo off
REM Compiles and runs the Bank Management System website.
REM Needs a JDK (javac) and a running MongoDB server (mongodb://localhost:27017).
REM Compiled .class files go to a temporary folder, so the project keeps only .java files.
cd /d "%~dp0"
set "JDK=%USERPROFILE%\.jdks\jdk-21.0.13+9\bin"
if exist "%JDK%\javac.exe" set "PATH=%JDK%;%PATH%"
set "BUILD=%TEMP%\oops-bank-build"
if exist "%BUILD%" rmdir /s /q "%BUILD%"
mkdir "%BUILD%"
javac -cp "lib\*" -d "%BUILD%" src\Main.java src\model\*.java src\view\*.java src\controller\*.java src\database\*.java
if errorlevel 1 (
    echo Compilation failed. Make sure a JDK is installed and javac is on PATH.
    pause
    exit /b 1
)
java -cp "%BUILD%;lib\*" Main
pause
