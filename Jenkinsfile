pipeline {
    agent any
    environment {
        IMAGE_NAME = "novel-cast"
        IMAGE_TAG = "1.0.0"
        CONTAINER_NAME = "novel-cast"
    }
    tools {
        jdk 'jdk25'
        maven 'maven3'
        nodejs "node25"
    }
    stages {
        stage('Load Environment Variables') {
            steps {
                script {
                    // Read env.config file content
                    def envConfigContent = readFile('/env.config')
                    // Split content by line and parse key-value pairs
                    def envVars = envConfigContent.split("\n").collectEntries { line ->
                        def pair = line.split('=')
                        [(pair[0]): pair[1]]
                    }
                    // Set environment variables explicitly
                    env.MODELSCOPE_APIKEY = envVars.MODELSCOPE_APIKEY
                    env.MODELSCOPE_MODEL = envVars.MODELSCOPE_MODEL
                }
            }
        }

        // 阶段：安装前端依赖
        stage('Install Frontend Dependencies') {
            steps {
                dir('novel-cast-webui') { // 进入前端目录
                    sh 'npm install -g pnpm'
                    sh 'pnpm install'
                }
            }
        }

        // 阶段：构建前端
        stage('Build Frontend') {
            steps {
                dir('novel-cast-webui') { // 进入前端目录
                    sh 'pnpm run build'
                }
            }
        }

        // 阶段：将前端产物移动到后端静态资源目录
        stage('Copy Frontend to Backend') {
            steps {
                script {
                    // 确保 static 目录存在
                    sh 'mkdir -p novel-cast-server/src/main/resources/static'
                    // 复制前端构建产物 (假设 pnpm build 输出在 dist 目录)
                    sh 'cp -r novel-cast-webui/dist/* novel-cast-server/src/main/resources/static/'
                }
            }
        }

        // 阶段：构建后端 (此时静态资源已包含在内)
        stage('Build Backend with Maven') {
            steps {
                dir('novel-cast-server') { // 进入后端目录
                    sh 'mvn clean package -DskipTests -Pdocker'
                }
            }
        }

        // 阶段：构建 Docker 镜像
        stage('Build Docker Image') {
            steps {
                script {
                    // 注意：这里在根目录构建，所以 Dockerfile 中的路径需要对应调整
                    sh "docker build . -t ${IMAGE_NAME}:${IMAGE_TAG}"
                }
            }
        }

        stage('Stop and Remove Previous Container') {
            steps {
                script {
                    sh """
                        # 查找并停止精确匹配的运行中容器
                        if [ \$(docker ps -q --filter "name=^${CONTAINER_NAME}\$") ]; then
                            echo "Stopping container: ${CONTAINER_NAME}"
                            docker stop ${CONTAINER_NAME}
                        else
                            echo "No running container found with exact name: ${CONTAINER_NAME}"
                        fi

                        # 查找并删除精确匹配的容器（无论是否在运行）
                        if [ \$(docker ps -aq --filter "name=^${CONTAINER_NAME}\$") ]; then
                            echo "Removing container: ${CONTAINER_NAME}"
                            docker rm ${CONTAINER_NAME}
                        else
                            echo "No container found with exact name: ${CONTAINER_NAME} to remove."
                        fi
                    """
                }
            }
        }

        stage('Run New Container') {
            steps {
                script {
                    sh """
                        docker run -d --name ${CONTAINER_NAME} \
                        --network app \
                        -p 19626:8080 \
                        -e MODELSCOPE_APIKEY=${env.MODELSCOPE_APIKEY} \
                        -e MODELSCOPE_MODEL=${env.MODELSCOPE_MODEL} \
                        -v /root/work/docker/aigc-platform-server/model/ref-audio:/app/novelCast/模型/语音 \
                        -v /root/work/docker/aigc-platform-server/model/gpt-sovits:/app/novelCast/模型/GPT-SoVITS模型 \
                        ${IMAGE_NAME}:${IMAGE_TAG}
                    """
                }
            }
        }
    }
    post {
        always {
            echo 'Deployment finished'
        }
    }
}
