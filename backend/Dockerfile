# syntax=docker/dockerfile:1.7

# ===== build =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY backend/ ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -Dmaven.test.skip=true -B

# ===== custom JRE via jlink =====
FROM eclipse-temurin:21-jdk AS jre
WORKDIR /jre
COPY --from=build /workspace/scaffold-admin/target/scaffold-admin.jar /tmp/app.jar
RUN --mount=type=cache,target=/root/.m2 \
    set -eux; \
    # 列出 fat jar 实际使用的模块；离线 / 无 jdeps 模块支持的环境会回退为完整模块集。
    JAVA_MODULES="$( \
      jdeps --ignore-missing-deps --multi-release 21 --print-module-deps --class-path '/tmp/app.jar' /tmp/app.jar 2>/dev/null \
      || true)"; \
    if [ -z "$JAVA_MODULES" ]; then \
        JAVA_MODULES="java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.management,jdk.unsupported,jdk.zipfs"; \
    fi; \
    echo "Using modules: $JAVA_MODULES"; \
    jlink --add-modules "$JAVA_MODULES" \
        --strip-debug --no-man-pages --no-header-files \
        --compress=zip-9 \
        --output /jre/runtime

# ===== runtime =====
FROM debian:bookworm-slim
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl tini \
    && rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME=/opt/java
ENV PATH="$JAVA_HOME/bin:$PATH"
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75"
WORKDIR /app
COPY --from=jre /jre/runtime $JAVA_HOME
COPY --from=build /workspace/scaffold-admin/target/scaffold-admin.jar /app/scaffold-admin.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["/usr/bin/tini","-g","--"]
CMD ["sh","-c","java $JAVA_OPTS -jar /app/scaffold-admin.jar"]
