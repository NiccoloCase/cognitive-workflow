FROM maven:3.9.6-eclipse-temurin-17

# Set working directory
WORKDIR /app
COPY pom.xml .

# Download dependencies 
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

EXPOSE 3001

# Copy .env file if it exists
COPY .env* ./

CMD ["mvn", "spring-boot:run"]
