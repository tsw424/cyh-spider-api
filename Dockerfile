FROM maven

COPY . .

RUN mvn spring-boot:run

