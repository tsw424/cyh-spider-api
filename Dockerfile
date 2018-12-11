FROM maven
FROM redis
FROM mysql:5.7.24
RUN mvn package -Dmaven.test.skip=true
COPY ./target/cyh-spider-gaofeifei-version1.0.jar app.jar
RUN java -jar app.jar
