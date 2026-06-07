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

        // 2. مرحلة البناء والتوزيع بالتوازي للـ 6 خدمات 🔥
        stage('Parallel Docker Deploy') {
            steps {
                script {
                    def findCommand = "find . -maxdepth 2 -type d -name 'diwan-*' -exec test -f {}/Dockerfile \\; -print"
                    def output = sh(script: findCommand, returnStdout: true).trim()
                    
                    if (output.isEmpty()) {
                        echo "⚠️ No microservices found with a Dockerfile!"
                        return
                    }

                    def microservices = output.split("\n")
                    def parallelBranches = [:]

                    // خريطة توزيع البورتات الحقيقية لكل خدمة بره وجوه (تفادياً للتعارض 8087 للـ gateway وباقي الخدمات 8080)
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
                                echo "🏗️ Building and Deploying: ${containerName}..."
                                
                                // بناء الـ Image باستخدام الـ Dockerfile المحلي لكل خدمة
                                sh "docker build -t ${containerName}:latest ./${msDir}"
                                
                                // مسح الحاوية القديمة لضمان تفريغ البورت والاسم
                                sh "docker rm -f ${containerName} || true"

                                // 🌟 تنفيذ الـ Docker Run Command المطلوب بالملّي
                                if (msDir == "diwan-gateway") {
                                    // الـ Gateway بالبورت الموحد 8087 بره وجوه طبقاً لأمرك الصريح
                                    sh """
                                    docker run -d \
                                      --name ${containerName} \
                                      --network nginx-proxy \
                                      -p 8087:8087 \
                                      -e VIRTUAL_HOST=${env.VIRTUAL_DOMAIN} \
                                      -e VIRTUAL_PORT=8087 \
                                      -e LETSENCRYPT_HOST=${env.VIRTUAL_DOMAIN} \
                                      -e JAVA_OPTS="-Xmx156m -Xms64m" \
                                      --add-host="host.docker.internal:host-gateway" \
                                      ${containerName}:latest
                                    """
                                } else {
                                    // باقي الـ 5 ميكروسيرفيسز يشتغلوا بالبورتات الموزونة (8081، 8085 إلخ) لـ 8080 الداخلي الافتراضي
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
                    // انطلاق السيمفونية الموازية لبناء الـ 6 خدمات في نفس اللحظة ⚡
                    parallel parallelBranches
                }
            }
        }
    }

    post {
        always {
            // تنظيف مساحة السيرفر من الـ dangling images القديمة دائماً
            sh "docker image prune -f || true"
        }
    }
}
