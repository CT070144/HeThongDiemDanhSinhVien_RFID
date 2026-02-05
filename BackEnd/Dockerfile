# Sử dụng JDK 21 trên nền Alpine để nhẹ hơn
FROM eclipse-temurin:21-jdk-alpine

# Thiết lập thư mục làm việc trong container
WORKDIR /app

# 1. Copy các file cấu hình Maven Wrapper
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# 2. Tải toàn bộ dependencies (Tận dụng Cache Layer)
RUN ./mvnw dependency:go-offline

# 3. Copy toàn bộ mã nguồn vào container
COPY src ./src

# 4. Chạy ứng dụng Spring Boot ở chế độ Development
CMD ["./mvnw", "spring-boot:run"]