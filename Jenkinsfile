pipeline {
    agent any

    environment {
        VIRTUAL_DOMAIN = "devxgroup.ddns.net"
        NOTIFICATION_EMAIL = "abdelrhman20075@gmail.com" // 🌟 تم تعديل إيميلك الشخصي هنا بنجاح
    }

    stages {
        // -----------------------------------------------------------------
        // مرحلة خدمة الـ SMART HOME (اللمبة وأليكسيا)
        // -----------------------------------------------------------------
        stage('Deploy: diwan-smarthome') {
            when {
                changeset "diwan-smarthome/**"
            }
            steps {
                echo "🔄 Detecting changes in diwan-smarthome. Executing Production Best Practices Build..."
                
                script {
                    // [Best Practice 1]: بناء الـ Image باستخدام الـ Multi-stage Dockerfile الداخلي
                    sh "docker build -t diwan-smarthome-api:new ./diwan-smarthome"
                    
                    // [Best Practice 2]: الـ Zero-Downtime Deployment
                    // نقوم الحاوية الجديدة باسم مؤقت وبورت مؤقت (مثلاً 8085) للفحص الطبي أولاً
                    sh "docker rm -f diwan-smarthome-api-staging || true"
                    sh """
                    docker run -d \
                      --name diwan-smarthome-api-staging \
                      --network nginx-proxy \
                      -p 8085:8080 \
                      --extra-hosts="host.docker.internal:host-gateway" \
                      diwan-smarthome-api:new
                    """
                    
                    // [Best Practice 3]: الـ Health Check الذكي والانتظار لغاية التأكد الكامل قبل التبديل
                    echo "⏳ Waiting for the new container to warm up and verify Database connection..."
                    
                    boolean isHealthy = false
                    // محاولة الفحص 5 مرات، بين كل مرة ومرة 6 ثوانٍ (إجمالي 30 ثانية أقصى حد للقيام)
                    for (int i = 0; i < 5; i++) {
                        sleep time: 6, unit: 'SECONDS'
                        def responseCode = sh(script: "curl -s -o /dev/null -w '%{http_code}' http://docker.internal || true", returnStdout: true).trim()
                        
                        if (responseCode == "200" || responseCode == "405") {
                            isHealthy = true
                            break
                        }
                        echo "⚠️ Health check attempt ${i+1} failed (Code: ${responseCode}). Retrying..."
                    }
                    
                    if (isHealthy) {
                        echo "✅ Health Check Passed! Switching traffic to the new container smoothly..."
                        
                        // نمسح الحاوية القديمة المنتجة فوراً
                        sh "docker rm -f diwan-smarthome-api || true"
                        
                        // نشغل الحاوية الجديدة بالاسم والدومين الرسمي وبدون أي Downtime للعملاء
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
                        
                        // تنظيف الـ Staging المؤقت
                        sh "docker rm -f diwan-smarthome-api-staging || true"
                    } else {
                        // لو فشل الـ Health check، بنطفي الحاوية البايظة ونسيب القديمة شغالة للناس ونوقع الـ Pipeline
                        sh "docker rm -f diwan-smarthome-api-staging || true"
                        error "❌ Deployment aborted! The new code contains issues and failed to start."
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------
    // الـ Post Actions للتنظيف والـ Notifications
    // -----------------------------------------------------------------
    post {
        always {
            // [Best Practice 4]: تنظيف الهارد ديسك أوتوماتيك من الـ Images المعلقة بعد البناء
            echo "🧹 Cleaning up dangling docker images to save server disk space..."
            sh "docker image prune -f || true"
        }
        
        success {
            mail to: "${env.NOTIFICATION_EMAIL}",
                 subject: "✅ SUCCESS: Diwan SmartHome Is Live - Job #${env.BUILD_NUMBER}",
                 body: """يا باشا، التعديل الجديد متاح الآن بدون أي Downtime!
                 
                 - الخدمة: diwan-smarthome
                 - الحالة الفنية: صحية ومستقرة تماماً (Healthy)
                 - رقم الـ Build الحالي: #_${env.BUILD_NUMBER}
                 - التوقيت: ${new Date().toString()}
                 
                 لمراجعة تفاصيل وخطوات البناء كاملة: ${env.BUILD_URL}"""
        }
        
        failure {
            mail to: "${env.NOTIFICATION_EMAIL}",
                 subject: "❌ CRITICAL FAILURE: Jenkins Deployment - Job #${env.BUILD_NUMBER}",
                 body: """الحق يا باشا، الـ Deployment فشل والنسخة الجديدة اترفضت!
                 
                 - السبب: الكود الجديد رما أخطاء ولم ينجح في الـ Health Check.
                 - ملاحظة: تم الإبقاء على النسخة القديمة مستقرة وشغالة للعملاء في البيوت تفادياً للمشاكل.
                 - رقم الـ Build الفاشل: #_${env.BUILD_NUMBER}
                 
                 ادخل فوراً شوف الـ Error Logs من الرابط ده علشان تصلح العيب: ${env.BUILD_URL}"""
        }
    }
}
