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
                        def externalPort = portMap[msDir] ?: (8090 + i)

                        parallelBranches["Deploy-${msDir}"] = {
                            stage("Sub-Stage: ${msDir}") {
                                echo "🏗️ Deploying ${msDir} on network proxy..."
                                
                                sh "docker build -t ${containerName}:latest ./${msDir}"
                                sh "docker rm -f ${containerName} || true"

                                if (msDir == "gateway") {
                                    sh """
                                    docker run -d \
                                      --name ${containerName} \
                                      --network nginx-proxy \
                                      -p ${externalPort}:8080 \
                                      -e VIRTUAL_HOST=${env.VIRTUAL_DOMAIN} \
                                      -e VIRTUAL_PORT=8080 \
                                      -e LETSENCRYPT_HOST=${env.VIRTUAL_DOMAIN} \
                                      --add-host="host.docker.internal:host-gateway" \
                                      ${containerName}:latest
                                    """
                                } else {
                                    sh """
                                    docker run -d \
                                      --name ${containerName} \
                                      --network nginx-proxy \
                                      -p ${externalPort}:8080 \
                                      --add-host="host.docker.internal:host-gateway" \
                                      ${containerName}:latest
                                    """
                                }
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
