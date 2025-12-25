@echo off
set /p SkillName=Enter skill name: 
powershell -NoProfile -ExecutionPolicy Bypass -Command "& '.\FindSkill.ps1' -SkillName '%SkillName%'"
pause
