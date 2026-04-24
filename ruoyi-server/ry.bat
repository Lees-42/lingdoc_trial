@echo off
chcp 65001 >nul
set AppPath=ruoyi-admin\target\ruoyi-admin.jar
set AppName=ruoyi-admin.jar

set JVM_OPTS=-Dname=%AppName% ^
 -Dfile.encoding=UTF-8 ^
 -Duser.timezone=Asia/Shanghai ^
 -Xms512m -Xmx1024m ^
 -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m ^
 -XX:+HeapDumpOnOutOfMemoryError ^
 -Xlog:gc*

:menu
cls
echo.
echo    ================================================
echo    %AppName% Service Manager (JDK 21)
echo    ================================================
echo.
echo    [1] Start Service
echo    [2] Stop Service
echo    [3] Restart Service
echo    [4] Check Status
echo    [5] Exit
echo.
set /p ID="Enter your choice (1-5): "

if "%ID%"=="1" goto start
if "%ID%"=="2" goto stop
if "%ID%"=="3" goto restart
if "%ID%"=="4" goto status
if "%ID%"=="5" exit
goto menu

:start
echo.
echo Checking status for %AppName%...
set pid=
for /f "usebackq tokens=1" %%a in (`jps -l ^| findstr /I /C:"%AppName%"`) do (
    set pid=%%a
)

if defined pid (
    echo [INFO] %AppName% is already running. PID: [%pid%]
    pause
    goto menu
)

echo Starting %AppPath% ...
echo ------------------------------------------------
java %JVM_OPTS% -jar %AppPath%
echo ------------------------------------------------
pause
goto menu

:stop
echo.
set pid=
for /f "usebackq tokens=1" %%a in (`jps -l ^| findstr /I /C:"%AppName%"`) do (
    set pid=%%a
)

if not defined pid (
    echo [INFO] %AppName% is not running.
    pause
    goto menu
)

echo Stopping PID: [%pid%] ...
taskkill /f /pid %pid%

if %errorlevel% equ 0 (
    echo Service stopped successfully.
) else (
    echo Failed to stop service.
)
pause
goto menu

:restart
echo Preparing to restart...
set pid=
for /f "usebackq tokens=1" %%a in (`jps -l ^| findstr /I /C:"%AppName%"`) do (
    set pid=%%a
)
if defined pid (
    taskkill /f /pid %pid%
    timeout /t 2 >nul
)
goto start

:status
echo.
set pid=
for /f "usebackq tokens=1-2" %%a in (`jps -l ^| findstr /I /C:"%AppName%"`) do (
    set pid=%%a
    set image_name=%%b
)

if not defined pid (
    echo [STATUS] Service is [OFFLINE]
) else (
    echo [STATUS] Service is [ONLINE]
    echo Process PID: %pid%
    echo Image Path: %image_name%
)
pause
goto menu
