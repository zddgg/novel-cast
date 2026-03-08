# 使用 JRE 基础镜像
FROM eclipse-temurin:25-jre

# 设置工作目录
WORKDIR /app

# 复制构建好的 JAR 文件到工作目录
COPY novel-cast-server/target/novel-cast-server.jar /app/app.jar

# 启动 Spring Boot 应用程序
CMD ["java", "-Dspring.profiles.active=prod", "-jar", "/app/app.jar"]
