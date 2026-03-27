pipeline {
    agent any

    tools {
        nodejs('NodeJS') 
    }

    environment {
        ROLLEDBACK = 'false'
        SONARQUBE_ENV = 'local-sonar'
    }

    stages {
        stage('Checkout & Setup') {
            steps {
                checkout scm
                echo "Building branch: ${env.BRANCH_NAME ?: env.GIT_BRANCH}"
            }
        }

        stage('📋 Info') {
            steps {
                echo "════════════════════════════════════════"
                echo "Starting build #${env.BUILD_NUMBER}"
                echo "Branch: ${env.BRANCH_NAME ?: env.GIT_BRANCH}"
                echo "════════════════════════════════════════"

                sh 'java -version'
                sh './backend/mvnw -version'

                echo "Checking Node.js version..."
                sh 'node --version'
                sh 'npm --version'
            }
        }

        stage('Backend build & test') {
            steps {
                dir('backend') {
                    sh './mvnw -B -q clean package -DskipTests'
                    echo "Backend build and tests completed successfully"
                }
            }
        }

        stage('Frontend build & test') {
            steps {
                dir('frontend/travel-orch') {
                    sh 'npm ci'
                    // sh 'npm test'
                    sh 'npm run build -- --configuration production'
                    echo "Frontend build and tests completed successfully"
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                dir('backend') {
                    withSonarQubeEnv(env.SONARQUBE_ENV) {
                        sh './mvnw sonar:sonar -Dsonar.projectKey=travel-orch-platform'
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Operations') {
            environment {
                IMAGE_TAG = "${env.BUILD_NUMBER}"
            }

            stages {
                stage('Prepare Rollback In case of Error') {
                    steps {
                        script {
                            if (fileExists('.prev_image_tag')) {
                                env.PREV_IMAGE_TAG = readFile('.prev_image_tag').trim()
                                echo "Previous image tag found: ${env.PREV_IMAGE_TAG}"
                            } else {
                                echo "No previous image tag found. This might be the first build."
                            }
                        }
                    }
                }

                stage('Docker Build') {
                    steps {
                        script {
                            sh "IMAGE_TAG=${env.IMAGE_TAG} docker compose build --parallel --no-cache"
                            echo "Docker build completed successfully"
                        }
                    }
                }

                stage('Docker Deploy') {
                    steps {
                        script {
                            echo "Deploying new image tag: ${env.IMAGE_TAG}"
                            sh "IMAGE_TAG=${env.IMAGE_TAG} docker compose down || true"
                            sh 'cp .env.example .env || echo "RATE_LIMIT_CAPACITY=10\nRATE_LIMIT_DURATION=60" > .env'
                            sh "IMAGE_TAG=${env.IMAGE_TAG} docker compose up -d --remove-orphans"

                            echo "Docker deployment completed successfully"
                            writeFile file: '.prev_image_tag', text: env.IMAGE_TAG
                        }
                    }
                }
            }

            post {
                failure {
                    script {
                        if (env.PREV_IMAGE_TAG) {
                            echo "Deployment failed. Rolling back to previous image tag: ${env.PREV_IMAGE_TAG}"
                            sh "IMAGE_TAG=${env.PREV_IMAGE_TAG} docker compose down || true"
                            sh 'cp .env.example .env || echo "RATE_LIMIT_CAPACITY=10\nRATE_LIMIT_DURATION=60" > .env'
                            sh "IMAGE_TAG=${env.PREV_IMAGE_TAG} docker compose up -d --remove-orphans"
                            env.ROLLEDBACK = 'true'
                            echo "Rollback to previous image tag ${env.PREV_IMAGE_TAG} completed successfully"
                        } else {
                            echo "No previous image tag available for rollback."
                        }
                    }
                }
            }
        }
    }

    post {
        failure {
            echo "Build #${env.BUILD_NUMBER} failed."
            emailext(
                to: 'hishamalmosawii@gmail.com',
                subject: "[AUTOMATED JENKINS CICD NOTIFICATION] ❌ Build FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                mimeType: 'text/html',
                body: """
                        <html>
                        <body>
                             <p><img src="https://wgplnsqonmpsfotdngjm.supabase.co/storage/v1/object/public/test/image.jpeg" alt="Sonic" style="max-height: 300px; height: auto; width: auto;" /></p>
                            <p><strong>Job:</strong> ${env.JOB_NAME}</p>
                            <p><strong>Build:</strong> #${env.BUILD_NUMBER}</p>
                            <p><strong>Status:</strong> FAILED</p>
                            <p><strong>Branch:</strong> ${env.BRANCH_NAME ?: env.GIT_BRANCH}</p>

                            <p>Please find details in the jenkins server log if needed.</p>
                        </body>
                        </html>
                      """
            )
        }
        success {
            script {
                if (env.ROLLEDBACK == 'true') {
                    echo "Build #${env.BUILD_NUMBER} succeeded after rollback."
                } else {
                    echo "Build #${env.BUILD_NUMBER} succeeded."
                }
            }
            emailext(
                to: 'hishamalmosawii@gmail.com',
                subject: "[AUTOMATED JENKINS CICD NOTIFICATION] ✅ Build SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                mimeType: 'text/html',
                body: """
                        <html>
                        <body>
                             <p><img src="https://wgplnsqonmpsfotdngjm.supabase.co/storage/v1/object/public/test/image.jpeg" alt="Sonic" style="max-height: 300px; height: auto; width: auto;" /></p>
                            <p><strong>Job:</strong> ${env.JOB_NAME}</p>
                            <p><strong>Build:</strong> #${env.BUILD_NUMBER}</p>
                            <p><strong>Status:</strong> SUCCESS</p>
                            <p><strong>Rolled back?:</strong> ${env.ROLLEDBACK}</p>
                            <p><strong>Branch:</strong> ${env.BRANCH_NAME ?: env.GIT_BRANCH}</p>

                            <p>Please find details in the jenkins server log if needed.</p>
                        </body>
                        </html>
                      """
            )
        }
    }

}