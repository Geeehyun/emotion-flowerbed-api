@echo off
echo Checking ANTHROPIC_API_KEY environment variable...
echo.
if defined ANTHROPIC_API_KEY (
    echo ✓ ANTHROPIC_API_KEY is set
    echo Value starts with: %ANTHROPIC_API_KEY:~0,10%...
) else (
    echo ✗ ANTHROPIC_API_KEY is NOT set
    echo.
    echo To set it, run:
    echo setx ANTHROPIC_API_KEY "your_api_key_here"
    echo.
    echo Then restart your terminal and IntelliJ
)
pause
