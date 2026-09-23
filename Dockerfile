FROM maven:3.9-eclipse-temurin-17

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY GroceryServer.java .

RUN mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
RUN javac -cp "$(cat cp.txt)" GroceryServer.java

CMD ["sh", "-c", "java -cp \".:$(cat cp.txt)\" GroceryServer"]