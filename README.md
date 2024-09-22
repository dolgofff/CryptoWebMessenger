# CryptoWebMessenger | 3rd year course project in Moscow Aviation Institute

# Messenger Application

## Overview

This project is a secure messenger application developed in **Java** using **Vaadin** for the UI, and **Spring Boot** for the backend. The application is designed with a strong emphasis on security, employing cryptographic algorithms such as **Twofish** and **RC5** to encrypt communications between users. It utilizes **Kafka** for real-time message streaming and **PostgreSQL** as the database for storing user data and chat histories.

## Features

- **Real-time Messaging**: Users can engage in real-time conversations, with messages sent and received instantly via Kafka.
- **Secure Communication**: 
  - All messages between users are encrypted using the **Diffie-Hellman** key exchange protocol, generating session keys.
  - Encrypted with **RC5** and **Twofish** algorithms for strong confidentiality.
- **User Authentication**: Built-in user registration and login system powered by **Spring Security**.
- **Multiple Chat Rooms**: Users can create or join multiple chat rooms, each securely separated from the others.
- **Context Menu Actions**: Right-click on messages for options like deleting or editing, with real-time updates for both participants.
- **Persistent Data Storage**: User data, message logs, and room configurations are stored securely in a **PostgreSQL** database.

## Tech Stack

- **Java 17**: The core programming language used for business logic.
- **Vaadin**: A Java-based web application framework used to create a dynamic and modern UI.
- **Spring Boot**: Provides the foundation for the backend, making it easy to build a scalable and maintainable RESTful service.
- **PostgreSQL**: For robust and secure data storage.
- **Kafka**: Used for handling real-time message streaming, ensuring messages are delivered instantly and reliably.
- **Jackson**: For efficient serialization and deserialization of JSON objects in the communication layer.
- **Twofish & RC5**: Encryption algorithms used to secure communication, along with the Diffie-Hellman protocol for key exchange.

## Cryptography

The application uses the following cryptographic methods to ensure data security:

- **Diffie-Hellman Protocol**: Used to securely exchange keys between users. The keys are generated dynamically for each session.
- **RC5 Encryption**: A symmetric-key block cipher that provides fast and secure encryption for chat messages.
- **Twofish Encryption**: Another symmetric key block cipher used for additional layers of security.

These algorithms ensure that even if an attacker gains access to the message data, they won't be able to decipher the content without the proper session key.

## Installation and Setup

### Prerequisites

- **Java 17** or later
- **PostgreSQL** (ensure the database is running and accessible)
- **Apache Kafka** (for message streaming)
- **Docker** (optional, for running Kafka and PostgreSQL containers)

### Steps to Run

1. Clone the repository:

   ```bash
   git clone https://github.com/yourusername/messenger-app.git
   cd messenger-app
   ```

2. Set up the PostgreSQL database:

   - Create a new database for the messenger.
   - Update the `application.properties` or `application.yml` with your PostgreSQL credentials.

3. Run Kafka (if not already running):

   - Start Kafka and Zookeeper using Docker Compose or manually.

4. Build and run the application:

   ```bash
   ./mvnw spring-boot:run
   ```

5. Access the application:

   - Open your browser and navigate to `http://localhost:8080` to start using the messenger.

### Docker Setup

If you prefer to run the entire stack using Docker, use the provided `docker-compose.yml` file to spin up the necessary services (Kafka, PostgreSQL, etc.):

```bash
docker-compose up --build
```

## Usage

- Register an account or log in with an existing one.
- Create or join a chat room.
- Start messaging securely with other users in real time.
- Messages are automatically encrypted and decrypted between users.
