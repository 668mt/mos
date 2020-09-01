FROM registry.cn-shenzhen.aliyuncs.com/library-jdk/jdk:8-jdk-alpine
MAINTAINER	Martin <765936728@qq.com>
COPY target/*.jar /app/app.jar
ENV SERVER_PORT=8080

EXPOSE $SERVER_PORT
ENV LANG=en_US.UTF-8
RUN echo "http://mirrors.aliyun.com/alpine/v3.9/main" > /etc/apk/repositories \
    && echo "http://mirrors.aliyun.com/alpine/v3.9/community" >> /etc/apk/repositories \
    && apk update upgrade \
    && apk add --no-cache procps unzip curl bash tzdata \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app
ENTRYPOINT java -XX:+UseG1GC -Xmx200m -Xms200m -Doss.client.base-paths=$BASE_PATHS -Doss.client.server-hosts=$SERVER_HOSTS -Dlogging.file.name=/app/logs/oss-client.log -jar app.jar