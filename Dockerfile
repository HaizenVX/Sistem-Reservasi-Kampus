FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
ADD https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/9.2.0/mysql-connector-j-9.2.0.jar /app/lib/mysql.jar
ADD https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.12.1/gson-2.12.1.jar /app/lib/gson.jar
COPY *.java ./
RUN mkdir build && javac -encoding UTF-8 -cp 'lib/*' -d build *.java

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build ./build
COPY --from=build /app/lib ./lib
COPY *.html *.css *.js ./
COPY sql ./sql
RUN chmod -R a+rX /app
USER 10001
ENV BIND_ADDRESS=0.0.0.0 PORT=10000
EXPOSE 10000
CMD ["java", "-XX:MaxRAMPercentage=65.0", "-cp", "build:lib/*", "ApiServer"]
