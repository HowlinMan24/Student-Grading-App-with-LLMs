# Student Grading App with LLMs

The **Student Grading App with LLMs** is a web application designed to automate student grading using Large Language Models (LLMs). It features a Spring Boot backend for handling API requests, an Angular frontend for the user interface, and a MySQL database for persistent storage, all orchestrated with Docker Compose.

## Features
- **Automated Grading**: Leverages LLMs (via OpenAI API) to evaluate and grade student submissions.
- **User Authentication**: Secure JWT-based authentication for users (e.g., teachers, students).
- **Database Management**: Stores user data, submissions, and grades in a MySQL database.
- **Responsive UI**: Angular-based frontend for an intuitive user experience.
- **Containerized Deployment**: Uses Docker Compose for easy setup and deployment.

## Technologies
- **Backend**: Spring Boot 3.4.4, Java 21, Spring Data JPA, Spring Security
- **Frontend**: Angular, TypeScript
- **Database**: MySQL 8.0
- **Containerization**: Docker, Docker Compose
- **External API**: OpenAI API for LLM-based grading
- **Build Tools**: Maven (backend), npm (frontend)

## Prerequisites
To run this project, ensure you have the following installed:
- [Docker](https://www.docker.com/get-started) (with Docker Compose)
- [Git](https://git-scm.com/downloads)
- An [OpenAI API key](https://platform.openai.com/account/api-keys) for LLM functionality

## Setup and Installation

### 1. Clone the Repository
```bash
git clone https://github.com/HowlinMan24/Student-Grading-App-with-LLMs.git
cd Student-Grading-App-with-LLMs
```

### 2. Configure Environment Variables
Create a `.env` file in the project root with the following content:
```env
DB_NAME=student_db
DB_USERNAME=<your_username>
DB_PASSWORD=<your_password>
DDL_AUTO=update
SPRING_SECURITY=DEBUG
WEBAPP_BACKEND=DEBUG
JWT_SECRET=<your_key>
OPENAI_API_KEY=<your_key>
OPENAI_URL=https://api.openai.com/v1/chat/completions
```

**Note**: Do not commit the `.env` file to version control. It’s included in `.gitignore` for security.

### 3. Build and Run with Docker Compose
Start the application using Docker Compose:
```bash
docker-compose up --build -d
```
This command:
- Builds the backend and frontend Docker images.
- Starts the MySQL database, backend (port 8080), and frontend (port 4200) services.

### 4. Verify the Application
- **Frontend**: Open `http://localhost:4200` in your browser to access the Angular UI.
- **Backend API**: Access `http://localhost:8080` (e.g., use Postman or curl to test API endpoints).
- **MySQL**: Connect to the database for debugging:
  ```bash
  docker exec -it database mysql -u root -pMakedonija.2023
  ```

### 5. Stop the Application
To stop the services:
```bash
docker-compose down
```
To clear database data:
```bash
docker-compose down -v
```

## Usage
1. **Register/Login**: Use the frontend UI to register or log in (JWT authentication).
2. **Submit Assignments**: Upload student assignments via the frontend.
3. **Automated Grading**: The backend processes submissions using the OpenAI API and stores grades in the database.
4. **View Grades**: Access grades and feedback through the UI.

For detailed API documentation, refer to the backend’s Swagger UI (if enabled) or check the `backend/src/main/java/com/webapp/backend` package for endpoint details.

## Troubleshooting
- **MySQL Connection Issues**: Ensure the `db` service is running (`docker-compose logs db`) and the `backend` service waits for it (health check in `docker-compose.yml`).
- **API Key Errors**: Verify the `OPENAI_API_KEY` in the `.env` file is valid.
- **Build Failures**: Check `Dockerfile-backend` and `Dockerfile-frontend` for correct paths to JAR and build artifacts.
- **Logs**: View logs for debugging:
  ```bash
  docker-compose logs backend
  docker-compose logs frontend
  docker-compose logs db
  ```

## Contributing
Contributions are welcome! To contribute:
1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/your-feature`).
3. Commit your changes (`git commit -m "Add your feature"`).
4. Push to the branch (`git push origin feature/your-feature`).
5. Open a Pull Request.

Please follow the [Code of Conduct](CODE_OF_CONDUCT.md) and ensure tests pass before submitting.

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contact
For questions or issues, open a GitHub issue or contact the maintainer at [your-email@example.com](mailto:your-email@example.com).
