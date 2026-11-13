@echo off
setlocal
set "GRADLE_EXIT_CONSOLE="
cd /d "%~dp0"
call "%~dp0gradlew.bat" build %*
set "BUILD_EXIT_CODE=%ERRORLEVEL%"
echo.
if "%BUILD_EXIT_CODE%"=="0" (
    echo Build succeeded.
) else (
    echo Build failed with exit code %BUILD_EXIT_CODE%.
)
pause
exit /b %BUILD_EXIT_CODE%
