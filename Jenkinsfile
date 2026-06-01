pipeline {
    agent any

    environment {
        VIRTUAL_DOMAIN = "devxgroup.ddns.net"
        NOTIFICATION_EMAIL = "abdelrhman20075@gmail.com"
        IMAGE_NAME = "diwan-smarthome-api"
    }

    stages {
        // 1. سحب الكود دائماً بدون شروط
        stage('Checkout Code') {
            steps {
                checkout scm
            }
        }

        // 2. البناء والتشغيل الفوري للميكروسيرفيس
        stage('Deploy SmartHome to Production') {
            steps {
                echo "🚀 Starting Full Automated Build for diwan-smarthome..."
                script {
                    // بناء الـ Image
                    sh "docker build -t ${env.IMAGE_NAME}:latest ./diwan-smarthome"
                    
                    // مسح الـ Container القديم إن وُجد
                    sh "docker rm -f ${env.IMAGE_NAME} || true"
                    
                    // تشغيل الـ Container الرسمي للإنتاج مباشرة
                    sh """
			docker run -d \
			  --name ${env.IMAGE_NAME} \
			  --network nginx-proxy \
			  -e VIRTUAL_HOST=smarthome.${env.VIRTUAL_DOMAIN} \
			  -e VIRTUAL_PORT=8080 \
			  -e LETSENCRYPT_HOST=smarthome.${env.VIRTUAL_DOMAIN} \
			  --add-host=database-host:host-gateway \
			  ${env.IMAGE_NAME}:latest
                    """
                    
                    echo "⏳ Giving the application 30 seconds to fully initialize and connect to DB..."
                    sleep time: 30, unit: 'SECONDS'
                }
            }
        }
    }

    post {
        always {
            // تنظيف مساحة السيرفر دائماً بعد البناء
            echo "🧹 Cleaning up dangling docker images..."
            sh "docker image prune -f || true"
        }
        success {
            echo "Sending Success Email..."
            emailext to: "${env.NOTIFICATION_EMAIL}",
                     subject: "✅ SUCCESS: Diwan SmartHome Automated Deployment - Job #${env.BUILD_NUMBER}",
                     body: "يا باشا، الـ Deployment الآلي خلص بنجاح والتطبيق شغال دلوقتي خلف البروكسي!\nرابط جينكينز: ${env.BUILD_URL}"
        }
        failure {
            echo "Sending Failure Email..."
            emailext to: "${env.NOTIFICATION_EMAIL}",
                     subject: "❌ FAILURE: Diwan SmartHome Deployment Failed - Job #${env.BUILD_NUMBER}",
                     body: "الحق يا باشا، حصلت مشكلة أثناء الـ Docker Build والـ Pipeline فشل.\nرابط اللوجز: ${env.BUILD_URL}"
        }
    }
}
