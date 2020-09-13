# MOS简介
Martin Object Storage 简易的对象存储服务

功能如下：
1. 提供对象存储服务
2. 支持签名访问
3. 支持sdk上传文件
4. 支持文件自动导入
5. 支持分布式部署、数据分片

# 起步
# 准备环境
## 安装MYSQL8+
请在网上找相关教程
## 安装Redis
请在网上找相关教程
# 下载和解压
[点击下载](http://rs.668mt.cn:6500/mos/mos/1.0/mos-1.0-RELEASE.rar?sign=cN-OKsCqkmZoPijfcyIW1HFidWrov8NSTQO4DnPEYyiN1oQR6JVOI2QF8qrgaARmuroKPgAYN1M-ELs4U_sPC5XJRYmRqvpoXdeVUIhznV9xzK71d71yGMjttNKee3aXDJ3OCMOvK9A4QzSbvZFavz2aG-b-OlLmYBGeTNEYWJU=&openId=2)

下载完成后，解压。

## 客户端配置
进入client目录，编辑application.properties文件。
1. 配置客户端存储路径。
2. 配置服务端的地址。
3. 配置客户端主机的ip。
4. 其他配置选填。

## 服务端配置
进入server目录，编辑application.properties文件。
1. 配置MYSQL数据源
2. 配置REDIS
3. 配置管理员的账号密码
4. 配置mos暴露的域名端口
5. 其他配置选填

## 启动和访问
进入client目录，点击start脚本启动，windows环境使用start.bat，linux环境使用start.sh。
进入server目录，点击start脚本启动。
访问本地`http://localhost:9700`地址，填入管理员账号进行登录。

# SDK的使用
1. 引入mos-sdk依赖
2. 配置sdk参数
3. 使用MosSdk进行接口调用