@echo off
chcp 65001 >nul
echo ========================================
echo 矿工眼镜 Curios 附属模组 - 快速构建
echo ========================================
echo.

echo 清理旧的构建输出...
call gradlew.bat --init-script init.gradle clean -Dnet.minecraftforge.gradle.check.certs=false
if errorlevel 1 goto error

echo.
echo 构建项目...
call gradlew.bat --init-script init.gradle build -Dnet.minecraftforge.gradle.check.certs=false
if errorlevel 1 goto error

echo.
echo ========================================
echo 构建成功！
echo JAR 文件位置: build\libs\minersglasses-curios-addon-1.0.1.jar
echo ========================================
echo.
pause
exit /b 0

:error
echo.
echo ========================================
echo 构建失败！请检查上方的错误信息。
echo ========================================
echo.
pause
exit /b 1
