docker rmi mos-server
docker stop mos-server
docker rm mos-server
docker build -t mos-server .
docker run -d -p 8080:8080 --name=mos-server -e active=dev --restart=on-failure:5 -v d:/work/docker/mos-server/logs:/app/logs mos-server
docker logs --tail=200 -f mos-server

 "registry-mirrors": [
    "https://n8oiqp7i.mirror.aliyuncs.com"
  ]

docker rmi mos-server
docker stop mos-server-dev
docker rm mos-server-dev
docker build -t mos-server .
docker run -d -p 9700:8080 --name=mos-server-dev -e ACTIVE=dev -e NACOS_ENABLED=true -e NACOS_SERVER=xxxx -e NACOS_USERNAME=xxxx -e NACOS_PASSWORD=xxxx --restart=on-failure:3 -v d:/work/docker/mos-server/logs:/app/logs mos-server
docker logs --tail=200 -f mos-server-dev
