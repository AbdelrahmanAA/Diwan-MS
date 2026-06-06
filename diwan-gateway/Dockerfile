FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/*.jar app.jar

# 🌟 التعديل الحاسم: إعلام الدوكر بأن البوابة تفتح داخلياً على بورت 8087 للـ Netty
EXPOSE 8087

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
