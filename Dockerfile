#Informing docker to use maven Image
FROM maven:3.8.3-openjdk-17 AS MAVEN_BUILD
#Setting the working directory in image filesystem
WORKDIR /build/
#Copy the pom.xml into working directory
COPY pom.xml /build/pom.xml
#Copy the source folder (src) into the working directory
COPY src /build/src
#Now we have src (source Code) & pom.xml in working directory. Run mvn package
RUN mvn package -DskipTests
#Inform Docker to Use JRE image to execute the package generated in previous stage
FROM eclipse-temurin:17-jdk-jammy
#Setting the Working directory
WORKDIR /app
#Copy the artifact generated from previous mvn build to working directory
COPY --from=MAVEN_BUILD /build/target/*.jar /app/doucmentassistance.jar
#Define the ENTRY point 
ENTRYPOINT ["java","-jar", "doucmentassistance.jar"]