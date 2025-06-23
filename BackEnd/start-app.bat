@echo off
chcp 65001
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
call .\gradlew.bat bootRun 