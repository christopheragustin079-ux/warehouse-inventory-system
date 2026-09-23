FROM maven:3.9-eclipse-temurin-17

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY . .

RUN mvn package -DskipTests

CMD ["java", "-cp", "target/classes:target/dependency/*", "GroceryServer"]
