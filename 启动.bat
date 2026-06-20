@echo off
cd /d "%~dp0"
if not exist "target\zuul-1.0-SNAPSHOT-jar-with-dependencies.jar" (
    echo 正在打包，首次需要下载依赖，请稍候...
    call mvn package -DskipTests -q
)
echo 启动 Chronicle of the Lost Realms...
start javaw -jar "target\zuul-1.0-SNAPSHOT-jar-with-dependencies.jar"
