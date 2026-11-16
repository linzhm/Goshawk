REM Modified for Goshawk on 2026-11-16; see MODIFICATIONS.md.
@echo off
set ARGS=%*

if "%ARGS%" == "" (
    gradlew.bat run
) else (
	gradlew.bat run --args="%ARGS%"
)
