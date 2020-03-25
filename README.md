# FinalSpeed
FinalSpeed是高速双边加速软件,可加速所有基于tcp协议的网络服务,在高丢包和高延迟环境下,仍可达到90%的物理带宽利用率,即使高峰时段也能轻松跑满带宽.

### 安装教程
[客户端安装说明](http://www.d1sm.net/thread-7-1-1.html)
<br />
[服务端安装说明](http://www.d1sm.net/thread-8-1-1.html)

### 使用帮助
```
需要管理员权限
java -jar finalspeed.jar -b 运行CLI版
java -jar finalspeed.jar 运行GUI版
```

### 一键安装代码：
```
wget -N --no-check-certificate https://raw.githubusercontent.com/91yun/finalspeed/master/install_fs.sh && bash install_fs.sh
```
###一键卸载代码
```
wget -N --no-check-certificate https://raw.githubusercontent.com/91yun/finalspeed/master/install_fs.sh && bash install_fs.sh uninstall
```
### finalspeed操作命令

启动： /etc/init.d/finalspeed start

停止命令：/etc/init.d/finalspeed stop

状态命令（查看日志）：/etc/init.d/finalspeed status

### finalspeed安装路径

安装路径： /fs/

日志路径：/fs/server.log

