pipeline {
    agent any

    environment {
        VIRTUAL_DOMAIN = "devxgroup.ddns.net"
        NOTIFICATION_EMAIL = "abdelrhman20075@gmail.com"
    }

    stages {
        // 1. جلب الكود بالكامل من جيت هاب لمرة واحدة
        stage('Checkout Monorepo') {
            steps {
                checkout scm
            }
        }

        // 2. مرحلة البناء الإعصاري الموازي
        stage('Parallel Build & Deploy') {
            steps {
                script {
                    def findCommand = "find . -maxdepth 2 -type d \\( -name 'diwan-*' -o -name 'gateway' \\) -exec test -f {}/Dockerfile \\; -print"
                    def output = sh(script: findCommand, returnStdout: true).trim()
                    
                    if (output.isEmpty()) {
                        echo "⚠️ No microservices found with a Dockerfile!"
                        return
                    }

                    def microservices = output.split("\n")
                    echo "🔍 Found ${microservices.size()} Microservices. Preparing parallel build branches..."

                    def parallelBranches = [:]

                    for (int i = 0; i < microservices.size(); i++) {
                        def msDir = microservices[i].replace("./", "").trim()
                        
                        // تخصيص اسم الحاوية
                        def containerName = (msDir == "gateway") ? "diwan-gateway-api" : "${msDir}-api"

                        parallelBranches["Deploy-${msDir}"] = {
                            stage("Sub-Stage: ${msDir}") {
                                echo "🏗️ [Parallel] Starting Build for: ${msDir}"
                                
                                // 1. بناء الـ Image
                                sh "docker build -t ${containerName}:latest ./${msDir}"

                                // 2. مسح الـ Container القديم إن وُجد
                                sh "docker rm -f ${containerName} || true"

                                // 3. التشغيل الذكي بناءً على نوع الخدمة
                                if (msDir == "gateway") {
                                    // 🌟 لو الخدمة هي الـ gateway، بنمرر متغيرات الـ SSL إجبارياً للـ Nginx Companion عشان يولد الشهادة فوراً
                                    sh """
                                    docker run -d \
                                      --name ${containerName} \
                                      --network host \
                                      -e VIRTUAL_HOST=api.${env.VIRTUAL_DOMAIN} \
                                      -e LETSENCRYPT_HOST=api.${env.VIRTUAL_DOMAIN} \
                                      ${containerName}:latest
                                    """
                                } else {
                                    // باقي الخدمات بتقوم Host mode عادي داخلياً
                                    sh "docker run -d --name ${containerName} --network host ${containerName}:latest"
                                }
                                
                                echo "✅ [Parallel] Successfully deployed ${containerName}"
                            }
                        }
                    }

                    // تشغيل الخدمات توازياً كالإعصار ⚡
                    parallel parallelBranches
                }
            }
        }
    }

    post {
        always {
            echo "🧹 Cleaning up dangling docker images to save space..."
            sh "docker image prune -f || true"
        }
    }
}
