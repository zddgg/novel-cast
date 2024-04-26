# novel-cast
### 一个小说多角色自动化AI配音程序
1. 角色分析：kimi
2. 音频生成：GPT-SoVITS
3. 示例音频素材：来源于B站 `白菜工厂1145号员工`

## 运行前准备
1. kimi api key
2. GPT-SoVITS api

## 运行程序
### 编译后的程序运行
1. 打开压缩包至D盘根路径，其他盘符修改yaml和nginx的nginx.conf
2. 运行 `启动.bat`
3. 浏览器打开 `127.0.0.1:8080`


### 源码运行
1. 技术栈 
jdk21, springboot3, vue3, nginx。
2. 需要nginx配置参考：
```
location /novelCast {
			alias D:/novelCast;
			}
```
3. 项目结构
```
D:
└─novelCast
    ├─模型
    │  ├─语音
    │  │  ├─原神
    │  │  │  ├─迪卢克
    │  │  │  │  └─默认
    │  └─配置
    └─项目

```


### 关闭项目时先关闭命令行窗口，再点击 `停止后再执行关闭-nginx.bat`