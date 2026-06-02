pipeline {
    agent any

    environment {
        NOTIFICATION_EMAIL = "abdelrhman20075@gmail.com"
    }

    stages {
        // 1. جلب الكود بالكامل من جيت هاب لمرة واحدة
        stage('Checkout Monorepo') {
            steps {
                checkout scm
            }
        }

        // 2. مرحلة البناء الإعصاري الموازي ⚡
        stage('Parallel Build & Deploy') {
            steps {
                script {
                    // أمر لينكس الذكي للبحث عن المجلدات (diwan-* أو gateway) بشرط وجود Dockerfile
                    def findCommand = "find . -maxdepth 2 -type d \\( -name 'diwan-*' -o -name 'gateway' \\) -exec test -f {}/Dockerfile \\; -print"
                    def output = sh(script: findCommand, returnStdout: true).trim()
                    
                    if (output.isEmpty()) {
                        echo "⚠️ No microservices found with a Dockerfile!"
                        return
                    }

                    def microservices = output.split("\n")
                    echo "🔍 Found ${microservices.size()} Microservices. Preparing parallel build branches..."

                    // خريطة (Map) لحفظ خطوات بناء كل خدمة على حدة
                    def parallelBranches = [:]

                    // توليد خطوات البناء لكل خدمة ديناميكياً
                    for (int i = 0; i < microservices.size(); i++) {
                        def msDir = microservices[i].replace("./", "").trim()
                        def containerName = (msDir == "gateway") ? "diwan-gateway-api" : "${msDir}-api"

                        // إنشاء فرع موازي مستقل لكل خدمة داخل الخريطة
                        parallelBranches["Deploy-${msDir}"] = {
                            stage("Sub-Stage: ${msDir}") {
                                echo "🏗️ [Parallel] Starting Build for: ${msDir}"
                                
                                // 1. بناء الـ Image جوه الدوكر
                                sh "docker build -t ${containerName}:latest ./${msDir}"

                                // 2. مسح الـ Container القديم إن وُجد
                                sh "docker rm -f ${containerName} || true"

                                // 3. تشغيل الـ Container بنظام الـ Host Mode المستقر
                                sh "docker run -d --name ${containerName} --network host ${containerName}:latest"
                                
                                echo "✅ [Parallel] Successfully deployed ${containerName}"
                            }
                        }
                    }

                    // 🔥 السطر السحري: تشغيل كل الفروع التي تم توليدها في نفس اللحظة توازياً!
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
        success {
            emailext to: "${env.NOTIFICATION_EMAIL}",
                     subject: "✅ SUCCESS: All Diwan Microservices Deployed Parallelly - Job #${env.BUILD_NUMBER}",
                     body: "يا باشا، الـ Parallel Pipeline اشتغل كالإعصار وخلص في وقت قياسي! تم بناء وتشغيل الـ 6 ميكروسيرفيسز والـ Gateway في نفس اللحظة بنظام الـ Host Mode.\nرابط جينكينز: ${env.BUILD_URL}"
        }
        failure {
            emailext to: "${env.NOTIFICATION_EMAIL}",
                     subject: "❌ CRITICAL: Parallel Pipeline Failed - Job #${env.BUILD_NUMBER}",
                     body: "الحق يا باشا، حصلت مشكلة أثناء البناء الموازي وأحد الخدمات سقطت.\nادخل شوف اللوجز: ${env.BUILD_URL}"
        }
    }
}
