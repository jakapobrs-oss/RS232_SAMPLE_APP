@echo off
setlocal
cd /d "%~dp0"

rem ---- Find Java 17+: 1) JAVA_HOME  2) java on PATH  3) Android Studio's bundled JDK ----
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto run

where java >nul 2>nul
if %errorlevel%==0 goto run

if exist "D:\Android_Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=D:\Android_Studio\jbr"
    goto run
)
if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" (
    set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
    goto run
)

echo [ERROR] Java 17+ not found. Install Android Studio (or a JDK 17+), or set JAVA_HOME first.
pause
exit /b 1

:run
call gradlew.bat :composeApp:run
pause
