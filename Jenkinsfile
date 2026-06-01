pipeline {
    agent any

    environment {
        NOTIFICATION_EMAIL = "abdelrhman20075@gmail.com"
    }

    stages {
        // 1. جلب الكود بالكامل من جيت هاب
        stage('Checkout Monorepo') {
            steps {
                checkout scm
            }
        }

        // 2. مرحلة البحث والبناء الديناميكي لكل الميكروسيرفيسز أوتوماتيكياً
        stage('Dynamic Build & Deploy') {
            steps {
                script {
                    // أمر لينكس للبحث عن كل المجلدات اللي بتبدأ بـ diwan- وجواها ملف Dockerfile
                    def findCommand = "find . -maxdepth 2 -type d -name 'diwan-*' -exec test -f {}/Dockerfile \\; -print"
                    def output = sh(script: findCommand, returnStdout: true).trim()
                    
                    if (output.isEmpty()) {
                        echo "⚠️ No microservices found with prefix 'diwan-' and a Dockerfile!"
                        return
                    }

                    // تقسيم المخرجات إلى قائمة من أسماء المجلدات
                    def microservices = output.split("\n")
                    echo "🔍 Found ${microservices.size()} Microservices to deploy: ${microservices}"

                    // اللفة الذكية لبناء وتشغيل كل خدمة ورا تانية أوتوماتيك
                    for (int i = 0; i < microservices.size(); i++) {
                        // تنظيف المسار (مثال: تحويل ./diwan-smarthome إلى diwan-smarthome)
                        def msDir = microservices[i].replace("./", "").trim()
                        def containerName = "${msDir}-api"

                        echo "🏗️ Starting Deployment for: ${msDir} (Container: ${containerName})"

                        // 1. بناء الـ Docker Image الخاصة بالخدمة الحالية
                        sh "docker build -t ${containerName}:latest ./${msDir}"

                        // 2. مسح الـ Container القديم للخدمة إن وُجد لتفادي تعارض الأسماء
                        sh "docker rm -f ${containerName} || true"

                        // 3. تشغيل الـ Container الجديد بنظام الـ Host Mode المستقر
                        sh "docker run -d --name ${containerName} --network host ${containerName}:latest"
                        
                        echo "✅ Successfully deployed ${containerName}"
                    }
                }
            }
        }
    }

    post {
        always {
            // تنظيف مساحة السيرفر دائماً بعد انتهاء بناء كل الخدمات
            echo "🧹 Cleaning up dangling docker images to save space..."
            sh "docker image prune -f || true"
        }
        success {
            emailext to: "${env.NOTIFICATION_EMAIL}",
                     subject: "✅ SUCCESS: Diwan Monorepo Automated Deployment - Job #${env.BUILD_NUMBER}",
                     body: "يا باشا، الـ Pipeline الديناميكي خلص بنجاح! تم فحص السيرفر وبناء وتشغيل جميع الميكروسيرفيسز التي تبدأ بـ diwan- أوتوماتيكياً بنظام الـ Host Mode.\nرابط جينكينز: ${env.BUILD_URL}"
        }
        failure {
            emailext to: "${env.NOTIFICATION_EMAIL}",
                     subject: "❌ CRITICAL: Diwan Dynamic Pipeline Failed - Job #${env.BUILD_NUMBER}",
                     body: "الحق يا باشا، حصلت مشكلة أثناء البناء الآلي للخدمات والـ Pipeline سقط.\nادخل شوف اللوجز من هنا: ${env.BUILD_URL}"
        }
    }
}
