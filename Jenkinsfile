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
    
    parallelBranches["Deploy-${msDir}"] = {
        stage("Sub-Stage: ${msDir}") {
            echo "🏗️ [Host Mode] Deploying ${msDir}..."
            
            sh "docker build -t ${containerName}:latest ./${msDir}"
            sh "docker rm -f ${containerName} || true"

            // 🌟 التشغيل بنظام الـ Host Mode لتقرأ الـ localhost لكل الخدمات بره وجوه
            sh "docker run -d --name ${containerName} --network host ${containerName}:latest"
            
            echo "✅ [Host Mode] Successfully deployed ${containerName}"
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
