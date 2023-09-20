FROM java:8
MAINTAINER	Martin <765936728@qq.com>
COPY *.jar /app/app.jar
ENV SERVER_PORT=8080
EXPOSE $SERVER_PORT
ENV LANG=en_US.UTF-8
# 设定时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
WORKDIR /app
ENTRYPOINT java -XX:+UseG1GC -Xmx1024m -Xms1024m -jar app.jar
