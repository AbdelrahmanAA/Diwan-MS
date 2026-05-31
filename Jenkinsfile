pipeline {
    agent any

    environment {
        VIRTUAL_DOMAIN = "devxgroup.ddns.net"
        NOTIFICATION_EMAIL = "abdelrhman20075@gmail.com"
    }

    stages {
        stage('Deploy: diwan-smarthome') {
            when {
                changeset "diwan-smarthome/**"
            }
            steps {
                echo "🔄 Detecting changes in diwan-smarthome. Executing Build..."
                script {
                    sh "docker build -t diwan-smarthome-api:new ./diwan-smarthome"
                    
                    sh "docker rm -f diwan-smarthome-api-staging || true"
                    sh """
                    docker run -d \
                      --name diwan-smarthome-api-staging \
                      --network nginx-proxy \
                      -p 8085:8080 \
                      --extra-hosts="host.docker.internal:host-gateway" \
                      diwan-smarthome-api:new
                    """
                    
                    echo "⏳ Waiting for the new container to warm up..."
                    boolean isHealthy = false
                    for (int i = 0; i < 5; i++) {
                        sleep time: 6, unit: 'SECONDS'
                        def responseCode = sh(script: "curl -s -o /dev/null -w '%{http_code}' http://docker.internal || true", returnStdout: true).trim()
                        if (responseCode == "200" || responseCode == "405") {
                            isHealthy = true
                            break
                        }
                        echo "⚠️ Health check attempt ${i+1} failed. Retrying..."
                    }
                    
                    if (isHealthy) {
                        echo "✅ Health Check Passed! Switching traffic..."
                        sh "docker rm -f diwan-smarthome-api || true"
                        sh """
                        docker run -d \
                          --name diwan-smarthome-api \
                          --network nginx-proxy \
                          -e VIRTUAL_HOST=smarthome.${env.VIRTUAL_DOMAIN} \
                          -e VIRTUAL_PORT=8080 \
                          -e LETSENCRYPT_HOST=smarthome.${env.VIRTUAL_DOMAIN} \
                          --extra-hosts="host.docker.internal:host-gateway" \
                          diwan-smarthome-api:new
                        """
                        sh "docker rm -f diwan-smarthome-api-staging || true"
                    } else {
                        sh "docker rm -f diwan-smarthome-api-staging || true"
                        error "❌ Deployment aborted! Failed Health Check."
                    }
                }
            }
        }
    }

    post {
        always {
            echo "🧹 Cleaning up dangling docker images..."
            sh "docker image prune -f || true"
        }
        
        success {
            // [تعديل إجباري للـ Gmail صراحة لتفادي خطأ بورت 25]
            mail to: "${env.NOTIFICATION_EMAIL}",
                 replyTo: "${env.NOTIFICATION_EMAIL}",
                 server: "://gmail.com",
                 port: "465",
                 subject: "✅ SUCCESS: Diwan SmartHome Is Live - Job #${env.BUILD_NUMBER}",
                 body: "يا باشا، التعديل الجديد متاح الآن والخدمة صحية ومستقرة تماماً (Healthy).\nرابط التفاصيل: ${env.BUILD_URL}"
        }
        
        failure {
            mail to: "${env.NOTIFICATION_EMAIL}",
                 replyTo: "${env.NOTIFICATION_EMAIL}",
                 server: "://gmail.com",
                 port: "465",
                 subject: "❌ CRITICAL FAILURE: Jenkins Deployment - Job #${env.BUILD_NUMBER}",
                 body: "الحق يا باشا، الـ Deployment فشل والنسخة الجديدة اترفضت لأنها سقطت في الـ Health Check.\nرابط اللوجز: ${env.BUILD_URL}"
        }
    }
}
