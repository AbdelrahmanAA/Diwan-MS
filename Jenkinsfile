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
                echo "🔄 Detecting changes in diwan-smarthome. Executing Clean Build..."
                script {
                    // بناء الـ Image بنجاح
                    sh "docker build -t diwan-smarthome-api:new ./diwan-smarthome"
                    
                    // تشغيل الـ Staging بدون أي فلاجات إضافية معقدة
                    sh "docker rm -f diwan-smarthome-api-staging || true"
                    sh "docker run -d --name diwan-smarthome-api-staging --network nginx-proxy -p 8085:8080 diwan-smarthome-api:new"
                    
                    echo "⏳ Waiting for the new container to warm up..."
                    sleep time: 15, unit: 'SECONDS' // وقت كافٍ لقيام الجافا
                    
                    // فحص الحالة مباشرة داخلياً عبر شبكة الـ Proxy
                    def responseCode = sh(script: "curl -s -o /dev/null -w '%{http_code}' http://diwan-smarthome-api-staging:8080/lamp || true", returnStdout: true).trim()
                    
                    if (responseCode == "200" || responseCode == "405") {
                        echo "✅ Health Check Passed! Code: ${responseCode}. Deploying to Production..."
                        
                        sh "docker rm -f diwan-smarthome-api || true"
                        sh """
                        docker run -d \
                          --name diwan-smarthome-api \
                          --network nginx-proxy \
                          -e VIRTUAL_HOST=smarthome.${env.VIRTUAL_DOMAIN} \
                          -e VIRTUAL_PORT=8080 \
                          -e LETSENCRYPT_HOST=smarthome.${env.VIRTUAL_DOMAIN} \
                          diwan-smarthome-api:new
                        """
                        sh "docker rm -f diwan-smarthome-api-staging || true"
                    } else {
                        sh "docker rm -f diwan-smarthome-api-staging || true"
                        error "❌ Deployment aborted! Failed Health Check with code: ${responseCode}"
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
            echo "Pipeline finished successfully!"
        }
    }
}
