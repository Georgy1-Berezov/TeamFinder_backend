FROM gradle:8.6-jdk17 AS builder
WORKDIR /app
COPY --chown=gradle:gradle . /app

# Build with shadowJar
RUN gradle --no-daemon shadowJar --warning-mode=none

# Flexible JAR extraction - works for single & multi-module projects
RUN mkdir -p /out && \
    echo "🔍 Searching for output JAR..." && \
    # Prefer shadowJar output (*-all.jar), fallback to any jar
    jarpath=$(find . -type f -path "*/build/libs/*-all.jar" | head -n1) && \
    if [ -z "$jarpath" ]; then \
      jarpath=$(find . -type f -path "*/build/libs/*.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" ! -name "original-*.jar" | head -n1); \
    fi && \
    if [ -n "$jarpath" ]; then \
      echo "✅ Using JAR: $jarpath" && \
      cp "$jarpath" /out/app.jar; \
    else \
      echo "❌ ERROR: No JAR found. Contents of all build/libs/:" && \
      find . -type d -name "libs" -path "*/build/*" -exec ls -la {} \; && \
      exit 1; \
    fi

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /out/app.jar /app/app.jar
ENV JAVA_OPTS=""
EXPOSE 10002
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]