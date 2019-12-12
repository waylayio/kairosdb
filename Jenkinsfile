pipeline {
    agent any
    tools {
        maven 'Maven 3.2.1'
        jdk 'Oracle JDK 8u77'
    }
    stages {
        stage ('Initialize') {
            steps {
                sh '''
                    echo "PATH = ${PATH}"
                    echo "M2_HOME = ${M2_HOME}"
                '''
            }
        }

        stage ('Build') {
            steps {
                sh 'mvn -Dmaven.test.failure.ignore=true deploy' 
            }
        }
    }
}
