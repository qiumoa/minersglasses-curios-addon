@echo off
chcp 65001 >nul
echo 正在下载 gradle-wrapper.jar...
echo.

REM 尝试从多个源下载
echo [尝试 1/3] 从 GitHub 代理下载...
powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; try { (New-Object System.Net.WebClient).DownloadFile('https://ghproxy.com/https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar', 'gradle\wrapper\gradle-wrapper.jar'); Write-Host '下载成功！' } catch { Write-Host '失败' }"

if exist gradle\wrapper\gradle-wrapper.jar goto success

echo.
echo [尝试 2/3] 从 jsDelivr CDN 下载...
powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; try { (New-Object System.Net.WebClient).DownloadFile('https://cdn.jsdelivr.net/gh/gradle/gradle@v8.5.0/gradle/wrapper/gradle-wrapper.jar', 'gradle\wrapper\gradle-wrapper.jar'); Write-Host '下载成功！' } catch { Write-Host '失败' }"

if exist gradle\wrapper\gradle-wrapper.jar goto success

echo.
echo [尝试 3/3] 从 Gradle 官方下载...
powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; try { (New-Object System.Net.WebClient).DownloadFile('https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar', 'gradle\wrapper\gradle-wrapper.jar'); Write-Host '下载成功！' } catch { Write-Host '失败' }"

if exist gradle\wrapper\gradle-wrapper.jar goto success

:failed
echo.
echo ========================================
echo 所有下载尝试都失败了
echo ========================================
echo.
echo 请运行 setup-gradle.bat 使用完整的 Gradle 下载方案
echo.
pause
exit /b 1

:success
echo.
echo ========================================
echo 成功！gradle-wrapper.jar 已下载
echo ========================================
echo.
echo 现在可以运行 build-first-time.bat 了
echo.
pause
exit /b 0
