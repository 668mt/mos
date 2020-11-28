jarname="mos-server-@project.version@.jar"
startCmd="java -Xmx500m -Xms500m -XX:+UseG1GC -Dspring.config.additional-location=application.properties -jar $jarname"
echo $startCmd && nohup $startCmd >> ./logs/${jarname}.out 2>&1 &
