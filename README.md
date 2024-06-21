# 本项目不再维护，新项目[aigc-server](https://github.com/zddgg/aigc-platform-server)，全新UI及交互，欢迎关注。


# novel-cast
### 一个小说多角色自动化AI配音程序
1. 角色分析：kimi
2. 音频生成：GPT-SoVITS
3. 示例音频素材：来源于B站 `白菜工厂1145号员工`

## 运行前准备
1. kimi api key
2. GPT-SoVITS api

## 分支说明
### 主分支以及gsv-base分支的gpt-sovits接口是api.py
### gsv-fast-inference的gpt-sovits接口是它的快速推理分支api_v2.py

## 运行程序
### 编译后的程序运行
1. 下载压缩包 网盘 (https://pan.quark.cn/s/93450e54d99c) 。
2. 打开压缩包，修改 `application.yaml` 文件中的 `kimi api key` 和 `GPT-SoVITS api`
3. 运行 `启动.bat`
4. 浏览器打开 `127.0.0.1:8080`

1. 技术栈 
jdk21, springboot3, vue3, ffmpeg
2. 项目结构
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
