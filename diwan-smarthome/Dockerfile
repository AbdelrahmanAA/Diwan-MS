# المرحلة الأولى: بناء وتجميع المشروع
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
# تحميل الـ dependencies كاش منفصل لتسريع البناء القادم
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# المرحلة الثانية: التشغيل الفعلي بحجم صغير جداً وأمان عالٍ
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# تشغيل التطبيق بمستخدم غير روت (Non-root user) لأقصى درجات الأمان في السيرفرات 
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

ENTRYPOINT ["java", "-jar", "app.jar"]
