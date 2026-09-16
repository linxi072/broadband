# ============================================================================
# 宽带业务管理系统 · 后端容器镜像（多阶段构建）
#   build  : maven 离线预热依赖 + 编译打 jar
#   runtime: Eclipse Temurin 21 JRE，仅保留运行所需
# 构建：docker build -t broadband-backend:1.0.1 ./backend
# 运行：docker run -e DB_HOST=... -e DB_PASSWORD=... -p 8082:8082 broadband-backend:1.0.1
#   （schema.sql / data.sql 在启动时自动初始化，连接信息由环境变量注入）
# ============================================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
# 预热依赖到本地仓库（离线缓存，失败不阻断）
RUN mvn -q -B dependency:go-offline || true
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"
# 运行期数据源（可被 docker run -e 覆盖）
ENV DB_HOST=127.0.0.1 \
    DB_PORT=3306 \
    DB_NAME=broadband \
    DB_USERNAME=root \
    DB_PASSWORD=
EXPOSE 8082
COPY --from=build /workspace/target/broadband-backend-1.0.0.jar /app/app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar --server.port=8082"]
