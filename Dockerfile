FROM adoptopenjdk/openjdk11:jdk-11.0.4_11 as builder

RUN jlink \
    --add-modules java.security.jgss,java.rmi,java.sql,java.desktop,jdk.crypto.ec,jdk.unsupported \
    --verbose \
    --strip-debug \
    --compress 2 \
    --no-header-files \
    --no-man-pages \
    --output /opt/jre-minimal


FROM debian:stable-slim
MAINTAINER ekino

COPY --from=builder /opt/jre-minimal /opt/jre-minimal

ENV PATH=${PATH}:/opt/jre-minimal/bin

ADD build/distributions/ekino-tools-version.tar /play/

EXPOSE 8080

ARG JAVA_OPTS
ENV JAVA_OPTS=${JAVA_OPTS}

CMD java ${JAVA_OPTS} \
        -Dhttp.port=8080 \
        -classpath /play/ekino-tools-version/lib/ekino-tools-version.jar play.core.server.ProdServerStart
