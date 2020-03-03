FROM quay.io/quarkus/centos-quarkus-maven:19.1.1
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
USER root
RUN mvn -f /usr/src/app/pom.xml dependency:resolve
RUN mvn -f /usr/src/app/pom.xml -Pnative clean package -DskipTests=true
