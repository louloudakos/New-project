@echo off
set /p ItemName=Enter item name: 
powershell -NoProfile -ExecutionPolicy Bypass -Command "& '.\FindItem.ps1' -ItemName '%ItemName%'"
pause
