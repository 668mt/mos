FROM openjdk:22-ea-17-jdk-oraclelinux8
#FROM openjdk:22-ea-17-jdk-slim
MAINTAINER	Martin <765936728@qq.com>
COPY *.jar /app/app.jar
ENV SERVER_PORT=4200
EXPOSE $SERVER_PORT
ENV LANG=en_US.UTF-8
# 设定时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
WORKDIR /app
ENV JVM_OPS="-Dserver.port=4200 -XX:+UseG1GC -Xmx1024m -Xms1024m"
ENTRYPOINT java $JVM_OPS -jar app.jar
