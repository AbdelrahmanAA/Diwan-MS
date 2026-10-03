// Build, test, scan and deploy the Diwan services.
//
// Flow: checkout -> work out which services changed -> build images (unit tests run inside the image build)
//       -> integration tests (Testcontainers) -> image scan -> deploy changed services with a health check
//       and automatic rollback.
//
// Jenkins credentials required (see README/CLAUDE.md):
//   diwan-jwt-secret, diwan-alexa-secret, diwan-mqtt-broker-url  (Secret text)
//   diwan-db                                                       (Username with password)

pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    parameters {
        booleanParam(name: 'DEPLOY_ALL', defaultValue: false,
                description: 'Rebuild and redeploy every service, not only the ones whose files changed')
        choice(name: 'INTEGRATION_TESTS', choices: ['warn', 'enforce', 'off'],
                description: 'Testcontainers tests (needs the Docker socket and a Jenkins workspace path the daemon can mount). warn = failures mark the build UNSTABLE, enforce = failures stop the pipeline')
        booleanParam(name: 'SCAN_IMAGES', defaultValue: true, description: 'Scan images with Trivy before deploying')
        string(name: 'SCAN_SEVERITY', defaultValue: 'CRITICAL', description: 'Trivy severities that fail the build, e.g. CRITICAL,HIGH')
        string(name: 'GATEWAY_MEMORY', defaultValue: '512m', description: 'Memory limit of the gateway container (JVM heap = 70%)')
        string(name: 'SERVICE_MEMORY', defaultValue: '384m', description: 'Memory limit of every other service container')
    }

    environment {
        VIRTUAL_DOMAIN = "developerxgroup.ddns.net"
        NOTIFICATION_EMAIL = "abdelrhman20075@gmail.com"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.IMAGE_TAG = sh(script: 'git rev-parse --short=8 HEAD', returnStdout: true).trim()
                    currentBuild.displayName = "#${env.BUILD_NUMBER} ${env.IMAGE_TAG}"
                }
            }
        }

        stage('Detect changes') {
            steps {
                script {
                    def all = buildableModules()
                    def selected = selectModules(all, params.DEPLOY_ALL)
                    env.DEPLOY_MODULES = selected.join(',')
                    echo "Modules: ${all}"
                    echo "To build and deploy (${env.IMAGE_TAG}): ${selected ?: 'nothing, no service files changed'}"
                }
            }
        }

        stage('Build images (unit tests included)') {
            when { expression { env.DEPLOY_MODULES?.trim() } }
            steps {
                script {
                    // All service Dockerfiles share one identical Maven builder stage that builds every module.
                    // The first image runs that Maven build (once, ~1 GB, unit tests included); the others then
                    // find it in Docker's layer cache, so they are cheap and can safely run in parallel.
                    // Starting them all at once would run six Maven builds, which the 8 GB server cannot take.
                    def modules = env.DEPLOY_MODULES.split(',').toList()
                    buildImage(modules[0])
                    def rest = modules.drop(1)
                    if (rest) {
                        def branches = [:]
                        for (m in rest) {
                            def module = m
                            branches["build ${module}"] = { buildImage(module) }
                        }
                        parallel branches
                    }
                }
            }
        }

        stage('Integration tests') {
            when { expression { env.DEPLOY_MODULES?.trim() && params.INTEGRATION_TESTS != 'off' } }
            steps {
                script {
                    try {
                        sh """
                            docker run --rm \\
                              -v "\$WORKSPACE":/build -w /build \\
                              -v "\$HOME/.m2":/root/.m2 \\
                              -v /var/run/docker.sock:/var/run/docker.sock \\
                              --add-host=host.docker.internal:host-gateway \\
                              -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \\
                              maven:3.9-eclipse-temurin-17 \\
                              mvn -B -pl ${env.DEPLOY_MODULES} -am verify -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false
                        """
                    } catch (err) {
                        if (params.INTEGRATION_TESTS == 'enforce') {
                            throw err
                        }
                        unstable("Integration tests failed or could not run: ${err.message}")
                    } finally {
                        junit testResults: '**/target/failsafe-reports/*.xml', allowEmptyResults: true
                    }
                }
            }
        }

        stage('Scan images') {
            when { expression { env.DEPLOY_MODULES?.trim() && params.SCAN_IMAGES } }
            steps {
                script {
                    for (m in env.DEPLOY_MODULES.split(',')) {
                        sh "docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v trivy-cache:/root/.cache/ " +
                           "aquasec/trivy:0.57.1 image --no-progress --ignore-unfixed " +
                           "--severity ${params.SCAN_SEVERITY} --exit-code 1 ${m}:${env.IMAGE_TAG}"
                    }
                }
            }
        }

        stage('Deploy') {
            when { expression { env.DEPLOY_MODULES?.trim() } }
            steps {
                script {
                    def modules = env.DEPLOY_MODULES.split(',').toList()
                    def services = modules.findAll { it != 'diwan-gateway' }
                    if (services) {
                        def branches = [:]
                        for (m in services) {
                            def module = m
                            branches["deploy ${module}"] = { deployService(module, env.IMAGE_TAG) }
                        }
                        parallel branches
                    }
                    // The gateway goes last so the services behind it are already up
                    if (modules.contains('diwan-gateway')) {
                        deployService('diwan-gateway', env.IMAGE_TAG)
                    }
                }
            }
        }
    }

    post {
        always {
            // Keep the 3 newest images per service (rollback targets), drop dangling layers
            script {
                for (m in buildableModules()) {
                    sh "docker images ${m} --format '{{.Tag}}' | tail -n +4 | xargs -r -I{} docker rmi ${m}:{} || true"
                }
                sh 'docker image prune -f || true'
            }
        }
        failure {
            script {
                try {
                    mail to: env.NOTIFICATION_EMAIL,
                         subject: "Diwan pipeline FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                         body: "Commit ${env.IMAGE_TAG}\n${env.BUILD_URL}"
                } catch (e) {
                    echo "Could not send the failure e-mail: ${e.message}"
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------------------------

// Services that are part of the parent pom and have a Dockerfile (diwan-common is a library, diwan-portfolio is not wired in yet)
def buildableModules() {
    def out = sh(script: "grep -o '<module>[^<]*</module>' pom.xml | sed -e 's/<[^>]*>//g'", returnStdout: true).trim()
    return out.readLines().collect { it.trim() }.findAll { it && fileExists("${it}/Dockerfile") }
}

// Only the services whose files changed since the last successful build; shared code changes rebuild everything
def selectModules(List all, boolean deployAll) {
    def base = env.GIT_PREVIOUS_SUCCESSFUL_COMMIT
    if (deployAll || !base) {
        return all
    }
    if (sh(script: "git cat-file -e ${base}^{commit}", returnStatus: true) != 0) {
        return all // previous commit is gone (force push, shallow clone)
    }
    def files = sh(script: "git diff --name-only ${base} HEAD", returnStdout: true).trim().readLines()
    if (files.any { it == 'pom.xml' || it == '.dockerignore' || it.startsWith('diwan-common/') }) {
        return all
    }
    return all.findAll { m -> files.any { it.startsWith("${m}/") } }
}

// Build context is the repo root (parent pom). Unit tests run in the image build, so a failing test stops the
// pipeline before anything is deployed. The --label does not affect Docker's layer cache.
def buildImage(String module) {
    sh "docker build --build-arg SKIP_TESTS=false " +
       "--label org.opencontainers.image.revision=${env.GIT_COMMIT} " +
       "-t ${module}:${env.IMAGE_TAG} -f ./${module}/Dockerfile ."
}

// Start the new image, wait until it reports ready; on failure put the previous image back
def deployService(String module, String tag) {
    def name = "${module}-api"
    def image = "${module}:${tag}"
    def previous = sh(script: "docker inspect --format '{{.Config.Image}}' ${name} 2>/dev/null || true", returnStdout: true).trim()

    withCredentials([
        string(credentialsId: 'diwan-jwt-secret', variable: 'JWT_SECRET'),
        string(credentialsId: 'diwan-alexa-secret', variable: 'ALEXA_LAMBDA_SHARED_SECRET'),
        string(credentialsId: 'diwan-mqtt-broker-url', variable: 'MQTT_BROKER_URL'),
        usernamePassword(credentialsId: 'diwan-db', usernameVariable: 'DB_USERNAME', passwordVariable: 'DB_PASSWORD')
    ]) {
        sh "docker rm -f ${name} || true"
        runContainer(module, name, image)
        if (waitUntilReady(name)) {
            echo "Deployed ${image}"
            return
        }
        sh "docker logs --tail 80 ${name} || true"
        sh "docker rm -f ${name} || true"
        if (previous) {
            echo "${image} did not become ready, rolling back to ${previous}"
            runContainer(module, name, previous)
        }
        error("${image} failed its readiness check" + (previous ? " (rolled back to ${previous})" : ''))
    }
}

def runContainer(String module, String name, String image) {
    def memory = module == 'diwan-gateway' ? params.GATEWAY_MEMORY : params.SERVICE_MEMORY
    def common = "docker run -d --name ${name} --network nginx-proxy --restart unless-stopped " +
                 "--memory ${memory} --memory-swap ${memory} --stop-timeout 30 " +
                 "--add-host=host.docker.internal:host-gateway -e JWT_SECRET -e DB_USERNAME -e DB_PASSWORD"
    if (module == 'diwan-gateway') {
        // Only the gateway is published (8087 -> 8080); the services are reachable on the nginx-proxy network only
        sh """${common} -p 8087:8080 \\
              -e VIRTUAL_HOST=${env.VIRTUAL_DOMAIN} -e VIRTUAL_PORT=8080 -e LETSENCRYPT_HOST=${env.VIRTUAL_DOMAIN} \\
              -e USERS_SERVICE_URL=http://diwan-users-api:8080 \\
              -e TRANSACTIONS_SERVICE_URL=http://diwan-transactions-api:8080 \\
              -e MEDICAL_SERVICE_URL=http://diwan-medical-api:8080 \\
              -e SMARTHOME_SERVICE_URL=http://diwan-smarthome-api:8080 \\
              -e LOGGING_SERVICE_URL=http://diwan-logging-api:8080 \\
              ${image}"""
    } else {
        sh "${common} -e ALEXA_LAMBDA_SHARED_SECRET -e MQTT_BROKER_URL ${image}"
    }
}

// Readiness (DB reachable, app up) on the management port inside the container; up to ~2 minutes
def waitUntilReady(String name) {
    for (int i = 0; i < 40; i++) {
        sleep(time: 3, unit: 'SECONDS')
        def rc = sh(script: "docker exec ${name} wget -qO- http://localhost:8081/actuator/health/readiness | grep -q UP", returnStatus: true)
        if (rc == 0) {
            return true
        }
    }
    return false
}
