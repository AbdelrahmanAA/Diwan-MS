@echo off
echo ==========================================
echo  Diwan - Starting All Microservices
echo ==========================================
echo.

echo [1/5] Starting diwan-users (port 8083)...
start "diwan-users" "C:\Program Files\OpenJDK\jdk-25\bin\java.exe" -jar "D:\SpringBoot\diwan-users\target\diwan-users-1.0.0.jar"
timeout /t 10 /nobreak

echo [2/5] Starting diwan-transactions (port 8081)...
start "diwan-transactions" "C:\Program Files\OpenJDK\jdk-25\bin\java.exe" -jar "D:\SpringBoot\diwan-transactions\target\diwan-transactions-0.0.1-SNAPSHOT.jar"
timeout /t 8 /nobreak

echo [3/5] Starting diwan-medical (port 8082)...
start "diwan-medical" "C:\Program Files\OpenJDK\jdk-25\bin\java.exe" -jar "D:\SpringBoot\diwan-medical\target\diwan-medical-0.0.1-SNAPSHOT.jar"
timeout /t 8 /nobreak

echo [4/5] Starting diwan-logging (port 8084)...
start "diwan-logging" "C:\Program Files\OpenJDK\jdk-25\bin\java.exe" -jar "D:\SpringBoot\diwan-logging\target\diwan-logging-1.0.0.jar"
timeout /t 8 /nobreak

echo [5/5] Starting diwan-gateway (port 8080)...
start "diwan-gateway" "C:\Program Files\OpenJDK\jdk-25\bin\java.exe" -jar "D:\SpringBoot\diwan-gateway\target\diwan-gateway-1.0.0.jar"

echo.
echo ==========================================
echo  All services started!
echo ==========================================
echo   Gateway:      http://localhost:8080
echo   Users:        http://localhost:8083
echo   Transactions: http://localhost:8081
echo   Medical:      http://localhost:8082
echo   Logging:      http://localhost:8084
echo.
echo  Kafka topics consumed:
echo   [logging-group]  request-logs
echo.
pause
