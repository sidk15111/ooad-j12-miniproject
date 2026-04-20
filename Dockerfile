FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN set -eux; \
		for i in 1 2 3 4 5; do \
			mvn -B -DskipTests \
				-Dmaven.wagon.http.retryHandler.count=5 \
				-Dmaven.wagon.http.retryHandler.requestSentEnabled=true \
				-Dmaven.wagon.http.pool=false \
				-Dhttps.protocols=TLSv1.2 \
				dependency:go-offline && break; \
			echo "dependency:go-offline failed on attempt $i"; \
			if [ "$i" -eq 5 ]; then exit 1; fi; \
			sleep $((i * 3)); \
		done

COPY src ./src
RUN set -eux; \
		for i in 1 2 3; do \
			mvn -B -DskipTests package && break; \
			echo "package failed on attempt $i"; \
			if [ "$i" -eq 3 ]; then exit 1; fi; \
			sleep $((i * 2)); \
		done

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
