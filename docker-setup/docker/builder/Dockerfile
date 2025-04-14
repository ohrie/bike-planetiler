FROM maven:3.9-eclipse-temurin-21-alpine AS compiler

# Set the working directory
WORKDIR /app

# Install Git
RUN apk add --no-cache git

# Clone the specified Git repository
RUN git clone https://github.com/ohrie/bike-planetiler.git .

# Build the project using Maven
RUN mvn clean package --file planetiler-examples/standalone.pom.xml

# The JAR file will be located in the target directory
# The final image will be built in the app Dockerfile
