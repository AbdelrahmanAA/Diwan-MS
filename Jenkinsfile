pipeline {
    agent any

    environment {
        VIRTUAL_DOMAIN = "developerxgroup.ddns.net"
        NOTIFICATION_EMAIL = "abdelrhman20075@gmail.com"
    }

    stages {
        // 1. جلب الكود بالكامل من جيت هاب
        stage('Checkout Monorepo') {
            steps {
                checkout scm
            }
        }

        // 2. 🌟 المرحلة السحرية: بناء ملفات الـ JAR لجميع الخدمات لمنع سقوط الـ COPY
        stage('Maven Compile & Package') {
            steps {
                echo "📦 Compiling and Packaging Spring Boot apps..."
                // تنفيذ بناء المافن وتخطي الـ Unit Tests لتوفير الوقت والرام
                sh "mvn clean package -DskipTests"
            }
        }

        // 3. مرحلة البناء الإعصاري الموازي للـ 6 خدمات والـ Gateway ⚡
        stage('Parallel Build & Deploy') {
            steps {
                script {
                    def findCommand = "find . -maxdepth 2 -type d -name 'diwan-*' -exec test -f {}/Dockerfile \\; -print"
                    def output = sh(script: findCommand, returnStdout: true).trim()
                    
                    if (output.isEmpty()) {
                        echo "⚠️ No microservices found with a Dockerfile!"
                        return
                    }

                    def microservices = output.split("\n")
                    echo "🔍 Found ${microservices.size()} Microservices. Preparing parallel build branches..."

                    def parallelBranches = [:]

                    // خريطة توزيع البورتات الخارجية لبيئة الـ Bridge المستقرة
                    def portMap = [
                        "diwan-gateway": 8087,
                        "diwan-users": 8081,
                        "diwan-medical": 8082,
                        "diwan-transactions": 8083,
                        "diwan-logging": 8084,
                        "diwan-smarthome": 8085
                    ]

                    for (int i = 0; i < microservices.size(); i++) {
                        def msDir = microservices[i].replace("./", "").trim()
                        def containerName = "${msDir}-api"
                        def externalPort = portMap[msDir] ?: (8090 + i)

                        parallelBranches["Deploy-${msDir}"] = {
                            stage("Sub-Stage: ${msDir}") {
                                echo "🏗️ [Parallel] Deploying ${containerName} on Port ${externalPort}..."
                                
                                // بناء الـ Image بعد نجاح المافن
                                sh "docker build -t ${containerName}:latest ./${msDir}"
                                sh "docker rm -f ${containerName} || true"

                                if (msDir == "diwan-gateway") {
                                    sh """
                                    docker run -d \
                                      --name ${containerName} \
                                      --network nginx-proxy \
                                      -p ${externalPort}:8087 \
                                      -e VIRTUAL_HOST=${env.VIRTUAL_DOMAIN} \
                                      -e VIRTUAL_PORT=8087 \
                                      -e LETSENCRYPT_HOST=${env.VIRTUAL_DOMAIN} \
                                      -e JAVA_OPTS="-Xmx156m -Xms64m" \
                                      --add-host="host.docker.internal:host-gateway" \
                                      ${containerName}:latest
                                    """
                                } else {
                                    sh """
                                    docker run -d \
                                      --name ${containerName} \
                                      --network nginx-proxy \
                                      -p ${externalPort}:8080 \
                                      -e JAVA_OPTS="-Xmx156m -Xms64m" \
                                      --add-host="host.docker.internal:host-gateway" \
                                      ${containerName}:latest
                                    """
                                }
                                echo "✅ Successfully deployed ${containerName}"
                            }
                        }
                    }
                    parallel parallelBranches
                }
            }
        }
    }

    post {
        always {
            sh "docker image prune -f || true"
        }
    }
}
