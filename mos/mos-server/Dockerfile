FROM registry.cn-shenzhen.aliyuncs.com/library-jdk/jdk:8-jdk-alpine
MAINTAINER	Martin <765936728@qq.com>
COPY target/*.jar /app/app.jar
ENV SERVER_PORT=8080

EXPOSE $SERVER_PORT
ENV LANG=en_US.UTF-8
RUN echo "http://mirrors.aliyun.com/alpine/edge/main" > /etc/apk/repositories \
    && echo "http://mirrors.aliyun.com/alpine/edge/community" >> /etc/apk/repositories \
    && apk update upgrade \
    && apk add --no-cache procps unzip curl bash tzdata \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app
ENTRYPOINT java -XX:+UseG1GC -Xmx300m -Xms300m -Dlogging.file.name=/app/logs/oss-server.log -jar app.jar