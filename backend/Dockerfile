# 多阶段构建：第一阶段使用Maven构建，第二阶段使用JRE运行
FROM maven:3.8.8-eclipse-temurin-11 AS builder

# 设置工作目录
WORKDIR /app

# 复制POM文件和源代码
COPY pom.xml .
COPY src ./src

# 下载依赖（利用Docker缓存层）
RUN mvn dependency:go-offline -B

# 编译打包
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:11-jre-alpine

# 设置工作目录
WORKDIR /app

# 从构建阶段复制jar文件
COPY --from=builder /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 设置环境变量
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]