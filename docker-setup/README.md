# Java Docker Setup

*This folder and files was created with GitHub Copilot.*

This project provides a Docker-based setup for building and running a Java application using Maven. It consists of two main components: a compiler container that compiles the Java application and an application container that runs the compiled JAR file.

## Project Structure

```
docker-setup
├── docker
│   ├── app
│   │   └── Dockerfile
│   └── compiler
│       └── Dockerfile
├── docker-compose.yml
└── README.md
```

## Prerequisites

- Docker installed on your machine.
- Docker Compose installed (usually included with Docker Desktop).

## Building the Application

To build the application, run the following command in the root of the project directory:

```bash
docker-compose build
```

This command will create the builder container, clone the specified Git repository, and build the Java application using Maven.

## Running the Application

After building the application, you can run it using:

```bash
docker-compose up
```

This command will start the application container, which will execute the generated JAR file.

## Stopping the Application

To stop the running application, you can use:

```bash
docker-compose down
```

This command will stop and remove the containers defined in the `docker-compose.yml` file.

## Customization

You can customize the Dockerfiles and the `docker-compose.yml` file to suit your specific needs, such as changing the base image, modifying build commands, or adjusting environment variables.

## Notes

- Ensure that the specified Git repository in the builder Dockerfile is accessible and contains a valid Java project with a `pom.xml` file.
- The generated JAR file will be available in the app container, and you can modify the application code as needed in the Git repository.
