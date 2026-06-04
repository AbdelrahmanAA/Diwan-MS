pipeline {
    agent any

    environment {
        VIRTUAL_DOMAIN = "developerxgroup.ddns.net"
        NOTIFICATION_EMAIL = "abdelrhman20075@gmail.com"
        GATEWAY_DB_URL = "jdbc:mysql://localhost:3306/diwan_gateway?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        GATEWAY_DB_USERNAME = "Dev_user"
        GATEWAY_DB_PASSWORD = "Dev@1234"
        GATEWAY_REDIS_HOST = "localhost"
        GATEWAY_KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
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
                        def externalPort = portMap[msDir] ?: (8090 + i)

                        parallelBranches["Deploy-${msDir}"] = {
                            stage("Sub-Stage: ${msDir}") {
                                echo "🏗️ [Bridge Mode] Deploying ${msDir}..."
                                
                                sh "docker build -t ${containerName}:latest ./${msDir}"
                                sh "docker rm -f ${containerName} || true"

                                if (msDir == "gateway") {
                                    sh """
                                    docker run -d \
                                      --name ${containerName} \
                                      --network nginx-proxy \
                                                                            -p ${externalPort}:8087 \
                                      -e VIRTUAL_HOST=${env.VIRTUAL_DOMAIN} \
                                                                            -e VIRTUAL_PORT=8087 \
                                      -e LETSENCRYPT_HOST=${env.VIRTUAL_DOMAIN} \
                                                                            -e SPRING_PROFILES_ACTIVE=SIT \
                                                                            -e DB_URL=${env.GATEWAY_DB_URL} \
                                                                            -e DB_USERNAME=${env.GATEWAY_DB_USERNAME} \
                                                                            -e DB_PASSWORD=${env.GATEWAY_DB_PASSWORD} \
                                                                            -e REDIS_HOST=${env.GATEWAY_REDIS_HOST} \
                                                                            -e KAFKA_BOOTSTRAP_SERVERS=${env.GATEWAY_KAFKA_BOOTSTRAP_SERVERS} \
                                      --add-host="localhost:host-gateway" \
                                      ${containerName}:latest
                                    """
                                } else {
                                    sh """
                                    docker run -d \
                                      --name ${containerName} \
                                      --network nginx-proxy \
                                      -p ${externalPort}:8080 \
                                      --add-host="localhost:host-gateway" \
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
