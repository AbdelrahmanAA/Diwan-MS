# Stage 1: طبخ وبناء ملف الـ JAR داخلياً بالمافن المعزول النظيف
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
# نسخ ملف الـ pom وكود الخدمة بالكامل جوه مرحلة البناء
COPY pom.xml .
COPY src ./src
# عمل الـ package وتخطي الـ Tests لتوفير الرام والوقت
RUN mvn clean package -DskipTests

# Stage 2: تشغيل الحاوية الخفيفة المستقرة
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
# 🌟 لقطة العبقرية: نسخ الـ jar اللي اتطبخ فوق فوراً بدون الاعتماد على ملفات بره
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8087
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
