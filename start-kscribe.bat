@echo off
start cmd /k "mvn spring-boot:run"
timeout /t 8 >nul
start http://localhost:8080/index.html 