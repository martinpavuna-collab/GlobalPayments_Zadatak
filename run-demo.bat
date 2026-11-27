@echo off
rem One-command demo (Windows): builds, starts the server in its own window, sends both sample
rem files with the client, prints the output file and the database contents, stops the server.
rem Needs JDK 11+ and Maven.
rem
rem Server and client only take a port, everything else is a fixed relative path (input\, output\,
rem messages.db). So both are started from target\demo, which gets its own copy of input\ and
rem never touches the project's own files.
rem
rem Waits use "ping" instead of "timeout", because "timeout" fails when stdin is redirected
rem (e.g. when started from an IDE).
setlocal
cd /d "%~dp0"
chcp 65001 >nul

if "%PORT%"=="" set PORT=5077
set DEMO=target\demo
set CP=..\classes;..\lib\*

if not "%SKIP_BUILD%"=="1" (
  echo == Building ^(first run downloads dependencies^) ==
  call mvn -q -DskipTests package dependency:copy-dependencies -DoutputDirectory=target/lib
  if errorlevel 1 exit /b 1
)

if exist %DEMO% rmdir /s /q %DEMO%
mkdir %DEMO%
xcopy /e /i /q input %DEMO%\input >nul

echo == Starting server on port %PORT% ==
start "demo-server" /min /d %DEMO% cmd /c "java -cp %CP% com.globalpayments.task.ServerMain %PORT% > server.log 2>&1"

rem wait (max ~30 s) until the server reports that it is listening
set TRIES=0
:waitserver
findstr /c:"listening" %DEMO%\server.log >nul 2>&1 && goto serverup
set /a TRIES+=1
if %TRIES% GEQ 30 goto servertimeout
ping -n 2 127.0.0.1 >nul
goto waitserver

:servertimeout
echo Server did not start in time:
type %DEMO%\server.log
taskkill /f /t /fi "WINDOWTITLE eq demo-server*" >nul 2>&1
exit /b 1

:serverup
pushd %DEMO%

rem The client always reads input\sample-input-hr.bin (sample 2, Croatian characters).
echo == Client sends sample 2: input\sample-input-hr.bin ==
java -cp %CP% com.globalpayments.task.ClientMain %PORT%

rem To send sample 1 as well, the demo copy of that file is replaced with the ASCII sample.
echo == Client sends sample 1: input\sample-input-ascii.bin ==
copy /y input\sample-input-ascii.bin input\sample-input-hr.bin >nul
java -cp %CP% com.globalpayments.task.ClientMain %PORT%

rem give the server a moment to finish processing, then stop it
ping -n 3 127.0.0.1 >nul
taskkill /f /t /fi "WINDOWTITLE eq demo-server*" >nul 2>&1

echo.
echo == Server log ==
type server.log
echo.
echo == Output file ^(target\demo\output\messages.txt, UTF-8^) ==
type output\messages.txt
echo.
java -cp %CP% com.globalpayments.task.QueryMain
popd
endlocal
