FROM openjdk:22-ea-17-jdk-oraclelinux8
MAINTAINER	Martin <765936728@qq.com>
COPY *.jar /app/app.jar
ENV SERVER_PORT=8080
EXPOSE $SERVER_PORT
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8
# 设定时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
WORKDIR /app
ENTRYPOINT java -Xmx2g -Xms2g -XX:+UseZGC -XX:ReservedCodeCacheSize=256m --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED -jar app.jar
