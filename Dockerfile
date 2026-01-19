# 构建阶段
FROM maven:3.9.12-eclipse-temurin-25-alpine AS build
WORKDIR /app

# 1. 缓存依赖 (利用 Docker 层缓存)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. 复制源码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:25-jre-alpine-3.23
WORKDIR /app

# 复制构建好的 jar 包
COPY --from=build /app/target/*.jar app.jar

# 设置时区为 UTC
ENV TZ=UTC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 暴露端口
EXPOSE 8080

# 启动命令，显式指定 UTC 时区
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-jar", "app.jar"]
