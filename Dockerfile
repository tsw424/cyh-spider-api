FROM maven
ADD . .
RUN mvn package -Dmaven.test.skip=true
RUN mv ./target/cyh-spider-gaofeifei-version1.0.jar app.jar
ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=sit"]