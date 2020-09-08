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
[点击下载客户端](http://rs.668mt.cn:6500/oss/mos/1.0/client-1.0.zip?sign=pazRVf1pt6zhim34a81Lz1m-mj3IZVsB5HF3PCqq3qMbmdqM54-VCKUAFAxIaSB-xi4W54xPr1QVKE-G_NT9LmRaTxphKTmgNZYzT3SOyfq51qd9xcW5BrJXC3tx1YePGqr7QlhZfhFhSkYaeK2GfjoPXd8Dn-B-RQHRU4VaZ5I=&openId=2)

[点击下载服务端](http://rs.668mt.cn:6500/oss/mos/1.0/server-1.0.zip?sign=WSeVEj61ihJw7AvHOyV6NQBCQUDPhOI5V4mI_QnMUba0X98Vd7qe-cUlx99awhs-pCC-BKUr58QXu76YLydscIATLLt3xlg-7xs3ZoKCmxt6r4bjFMPudmuta5hNLRyXDqi7E2NDIENNRNGB3JfzVESogfAvF3BPPXd4YAcUFig=&openId=2)

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
4. 其他配置选填

## 启动和访问
进入client目录，点击start脚本启动，windows环境使用start.bat，linux环境使用start.sh。
进入server目录，点击start脚本启动。
访问本地`http://localhost:9700`地址，填入管理员账号进行登录。

# SDK的使用
1. 引入mos-sdk依赖
2. 配置sdk参数
3. 使用MosSdk进行接口调用