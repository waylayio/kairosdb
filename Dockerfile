ARG KAIROSDB_VERSION=1.3.0-waylay+8-SNAPSHOT

FROM maven:3.9-eclipse-temurin-21 AS build
ARG KAIROSDB_VERSION

WORKDIR /home/kairosdb/git

COPY pom.xml .
RUN mvn dependency:resolve dependency:resolve-plugins -B || true

COPY . .
RUN mvn clean package -DskipTests -B

RUN tar -xzvf "target/kairosdb-${KAIROSDB_VERSION}.tar.gz"

FROM eclipse-temurin:21-jre
ARG KAIROSDB_VERSION
ENV KAIROSDB_HOME=/opt/kairosdb-${KAIROSDB_VERSION}
ENV CLASSPATH=${KAIROSDB_HOME}/lib/*

COPY --from=build /home/kairosdb/git/kairosdb /opt/kairosdb-${KAIROSDB_VERSION}

RUN ln -s ${KAIROSDB_HOME}/conf /etc/kairosdb && \
    echo 'export PATH=${KAIROSDB_HOME}/bin:${PATH}' >> /root/.bashrc

EXPOSE 8080 4242

WORKDIR /opt/kairosdb-${KAIROSDB_VERSION}/bin
ENTRYPOINT ["sh", "-c", ". ~/.bashrc && kairosdb.sh run"]
