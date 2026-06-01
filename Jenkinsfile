pipeline {
    agent any

    environment {
        NOTIFICATION_EMAIL = "abdelrhman20075@gmail.com"
    }

    stages {
        // -----------------------------------------------------------------
        // 1. مرحلة جلب الكود من جيت هاب
        // -----------------------------------------------------------------
        stage('Checkout Monorepo') {
            steps {
                checkout scm
            }
        }

        // -----------------------------------------------------------------
        // 2. بناء وتشغيل خدمة الـ SMART HOME (بورت 8086 الحاري)
        // -----------------------------------------------------------------
        stage('Deploy: diwan-smarthome') {
            steps {
                echo "🚀 Building and Deploying: diwan-smarthome..."
                script {
                    sh "docker build -t diwan-smarthome-api:latest ./diwan-smarthome"
                    sh "docker rm -f diwan-smarthome-api || true"
                    sh "docker run -d --name diwan-smarthome-api --network host diwan-smarthome-api:latest"
                }
            }
        }

        // -----------------------------------------------------------------
        // 3. بناء وتشغيل خدمة الـ USERS (مثال: بورت 8081 أو المتسيت جواه)
        // -----------------------------------------------------------------
        stage('Deploy: diwan-users') {
            steps {
                echo "🚀 Building and Deploying: diwan-users..."
                script {
                    sh "docker build -t diwan-users-api:latest ./diwan-users"
                    sh "docker rm -f diwan-users-api || true"
                    sh "docker run -d --name diwan-users-api --network host diwan-users-api:latest"
                }
            }
        }

        // -----------------------------------------------------------------
        // 4. بناء وتشغيل خدمة الـ GATEWAY (مثال: بورت 8000)
        // -----------------------------------------------------------------
        stage('Deploy: diwan-gateway') {
            steps {
                echo "🚀 Building and Deploying: diwan-gateway..."
                script {
                    sh "docker build -t diwan-gateway:latest ./diwan-gateway"
                    sh "docker rm -f diwan-gateway || true"
                    sh "docker run -d --name diwan-gateway --network host diwan-gateway:latest"
                }
            }
        }
    }

    // -----------------------------------------------------------------
    // الـ Post Actions للتنظيف والتنبيهات النهائية
    // -----------------------------------------------------------------
    post {
        always {
            echo "🧹 Cleaning up dangling docker images to save space..."
            sh "docker image prune -f || true"
        }
        
        success {
            echo "Sending Global Success Email..."
            emailext to: "${env.NOTIFICATION_EMAIL}",
                     subject: "✅ SUCCESS: All Diwan Microservices Are Live - Job #${env.BUILD_NUMBER}",
                     body: """يا باشا، الـ Pipeline الشامل خلص بنجاح!
                     
                     كل الميكروسيرفيسز (smarthome, users, gateway) حصل لها Deploy وهي شغالة ومستقرة دلوقتي على السيرفر بنظام الـ Host Mode.
                     
                     تفاصيل البناء بالكامل: ${env.BUILD_URL}"""
        }
        
        failure {
            echo "Sending Failure Email..."
            emailext to: "${env.NOTIFICATION_EMAIL}",
                     subject: "❌ CRITICAL: Diwan Deployment Pipeline Failed - Job #${env.BUILD_NUMBER}",
                     body: "الحق يا باشا، حصلت مشكلة في بناء أو تشغيل أحد الخدمات والـ Pipeline سقط.\nادخل فوراً شوف اللوجز من هنا: ${env.BUILD_URL}"
        }
    }
}
