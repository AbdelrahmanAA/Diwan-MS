pipeline {
    agent any

    environment {
        VIRTUAL_DOMAIN = "developerxgroup.ddns.net"
        NOTIFICATION_EMAIL = "abdelrhman20075@gmail.com"
    }

    stages {
        stage('Checkout Monorepo') {
            steps {
                checkout scm
            }
        }

        stage('Parallel Bridge Deploy') {
            steps {
                script {
                    def findCommand = "find . -maxdepth 2 -type d \\( -name 'diwan-*' -o -name 'gateway' \\) -exec test -f {}/Dockerfile \\; -print"
                    def output = sh(script: findCommand, returnStdout: true).trim()
                    
                    if (output.isEmpty()) {
                        echo "⚠️ No microservices found with a Dockerfile!"
                        return
                    }

                    def microservices = output.split("\n")
                    def parallelBranches = [:]

                    // مصفوفة لتوزيع البورتات الخارجية على الخدمات لتفادي التعارض بره بره
                    def portMap = [
                        "gateway": 8087,
                        "diwan-users": 8081,
                        "diwan-medical": 8082,
                        "diwan-transactions": 8083,
                        "diwan-logging": 8084,
                        "diwan-smarthome": 8085
                    ]

                    for (int i = 0; i < microservices.size(); i++) {
                        def msDir = microservices[i].replace("./", "").trim()
                        def containerName = (msDir == "gateway") ? "diwan-gateway-api" : "${msDir}-api"
                        
                        // تحديد بورت الخرج؛ لو مش متسجل فوق يدي بورت تلقائي فوق الـ 8090
                        def externalPort = portMap[msDir] ?: (8090 + i)

                        parallelBranches["Deploy-${msDir}"] = {
                            stage("Sub-Stage: ${msDir}") {
                                echo "🏗️ [Bridge Mode] Deploying ${msDir} on external port ${externalPort}..."
                                
                                // 1. بناء الـ Image
                                sh "docker build -t ${containerName}:latest ./${msDir}"

                                // 2. مسح الحاوية القديمة
                                sh "docker rm -f ${containerName} || true"

                                // 3. التشغيل الاحترافي بنظام الـ Bridge + Port Mapping + Network 🐳
                                if (msDir == "gateway") {
                                    // الـ Gateway تربط بالـ nginx-proxy مباشرة وتستقبل الشهادة أوتوماتيك
                                    sh """
                                    docker run -d \
                                      --name ${containerName} \
                                      --network nginx-proxy \
                                      -p ${externalPort}:8080 \
                                      -e VIRTUAL_HOST=${env.VIRTUAL_DOMAIN} \
                                      -e VIRTUAL_PORT=8080 \
                                      -e LETSENCRYPT_HOST=${env.VIRTUAL_DOMAIN} \
                                      --add-host=host.docker.internal:host-gateway \
                                      ${containerName}:latest
                                    """
                                } else {
                                    // باقي الـ 5 ميكروسيرفيسز يقوموا Bridge ومحميين وجوا شبكة الـ proxy برضه
                                    sh """
                                    docker run -d \
                                      --name ${containerName} \
                                      --network nginx-proxy \
                                      -p ${externalPort}:8080 \
                                      --add-host=host.docker.internal:host-gateway \
                                      ${containerName}:latest
                                    """
                                }
                                echo "✅ [Bridge Mode] Successfully deployed ${containerName}"
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
