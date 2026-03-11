# Use official Java image
FROM eclipse-temurin:17-jdk
# Set working directory
WORKDIR /app

# Copy jar file
COPY target/movietracker-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]