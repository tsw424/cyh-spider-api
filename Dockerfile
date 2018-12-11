FROM maven
RUN "mvn package"
COPY ./target/cyh-spider-gaofeifei-version1.0.jar app.jar
RUN "java -jar app.jar"
