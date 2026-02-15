# KYC AI Application

A comprehensive, GDPR-compliant Know Your Customer (KYC) verification system powered by AI agents, built with Spring Boot 3.2+, LangChain4j 1.10.0, Angular 17+, and local LLMs via Ollama.

## Features

### Core Features
- **AI-Powered Document Analysis**: Automatic extraction and verification of identity documents using OCR and LLMs
- **Multi-Agent System**: Supervisor, Document, Risk, and Chatbot agents working together
- **AML Risk Assessment**: Automated risk scoring with PEP checks and adverse media screening
- **RAG-Powered Chatbot**: Intelligent customer support with regulatory knowledge retrieval
- **GDPR Compliance**: Full data subject rights, consent management, and automated data retention

### Security & Compliance
- **Data Privacy**: 100% on-premise AI processing - no data leaves your infrastructure
- **Encryption**: AES-256 encryption at rest, TLS 1.3 in transit
- **Audit Logging**: Immutable audit trails for all data access
- **Access Control**: Role-based (RBAC) and attribute-based (ABAC) authorization

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Angular        │────▶│  Spring Boot    │────▶│  PostgreSQL     │
│  Frontend       │     │  Backend        │     │  + pgvector     │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        ▼                      ▼                      ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Ollama (LLMs)  │     │  MinIO (Docs)   │     │  RabbitMQ       │
│  - llama3.2     │     │  (S3 Storage)   │     │  (Message Queue)│
│  - nomic-embed  │     │                 │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

## Tech Stack

### Backend
- **Framework**: Spring Boot 3.2+ with Java 21
- **AI Framework**: LangChain4j 1.10.0
- **Security**: Spring Security with JWT
- **Database**: PostgreSQL 16 + pgvector extension
- **Document Storage**: MinIO (S3-compatible)
- **Message Queue**: RabbitMQ

### Frontend
- **Framework**: Angular 17+
- **UI Library**: Angular Material
- **State Management**: NgRx
- **HTTP Client**: Angular HttpClient

### AI/ML Infrastructure
- **LLM Runtime**: Ollama 0.3+
- **Models**: llama3.2, nomic-embed-text, llava-phi3
- **Vector Search**: pgvector with HNSW indexing
- **OCR**: Tesseract

### DevOps & Monitoring
- **Containerization**: Docker & Docker Compose
- **Monitoring**: Prometheus + Grafana
- **Logging**: Spring Boot Actuator

## Quick Start

### Prerequisites
- Docker 24.0+
- Docker Compose 2.20+
- 8GB+ RAM (for LLM inference)
- 20GB+ free disk space

### Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd kyc-ai-application
```

2. **Set up environment variables**
```bash
cp .env.example .env
# Edit .env with your configuration
```

3. **Start the infrastructure**
```bash
cd infrastructure/docker
docker-compose up -d
```

4. **Wait for services to start**
```bash
# Check service health
docker-compose ps

# View logs
docker-compose logs -f kyc-service
```

5. **Access the application**
- Frontend: http://localhost:4200
- Backend API: http://localhost:8080/api/v1
- Swagger UI: http://localhost:8080/swagger-ui.html
- Grafana: http://localhost:3000 (admin/admin)
- MinIO Console: http://localhost:9001

### Default Credentials
- **Admin User**: admin / admin123 (change in production!)
- **Grafana**: admin / admin (or value from GRAFANA_PASSWORD env var)
- **MinIO**: minioadmin / minioadmin

## API Documentation

### Authentication
```bash
POST /api/v1/auth/login
{
  "username": "admin",
  "password": "admin123"
}
```

### KYC Operations
```bash
# Submit KYC document
POST /api/v1/kyc/submit
Headers: X-Customer-Id, X-Consent-Token
Content-Type: multipart/form-data

# Get KYC status
GET /api/v1/kyc/status/{customerId}

# Chat with support
POST /api/v1/chat/message
```

### GDPR Endpoints
```bash
# Export personal data
GET /api/v1/gdpr/export-data?customerId={id}

# Delete personal data
DELETE /api/v1/gdpr/delete-data?customerId={id}

# Record consent
POST /api/v1/gdpr/consent
```

## Multi-Agent System

### Supervisor Agent
- Routes tasks to appropriate specialized agents
- Validates GDPR compliance before processing
- Coordinates multi-step KYC workflows

### Document Agent
- Analyzes ID documents, passports, proof of address
- Extracts structured data with confidence scores
- Applies data minimization principles

### Risk Agent
- Performs AML risk scoring
- Checks PEP status and adverse media
- Provides human-review flags for high-risk cases

### Chatbot Agent
- Handles customer inquiries about KYC status
- Uses RAG to retrieve regulatory information
- Escalates complex requests to human agents

## GDPR Compliance

### Data Subject Rights
| Right | Endpoint | Description |
|-------|----------|-------------|
| Access | GET /api/v1/gdpr/export-data | Export personal data |
| Erasure | DELETE /api/v1/gdpr/delete-data | Delete personal data |
| Rectification | PUT /api/v1/gdpr/update-data | Update personal data |
| Portability | GET /api/v1/gdpr/export-data | JSON/XML export |
| Object | POST /api/v1/gdpr/withdraw-consent | Opt-out |

### Data Retention
- Default retention: 90 days
- Automatic purging via scheduled jobs
- Audit logs retained for 7 years

## Development

### Backend Development
```bash
cd kyc-service
./mvnw spring-boot:run
```

### Frontend Development
```bash
cd kyc-frontend
npm install
npm start
```

### Running Tests
```bash
# Backend tests
cd kyc-service
./mvnw test

# Frontend tests
cd kyc-frontend
npm test
```

## Deployment

### Production Considerations
1. Change all default passwords
2. Use strong JWT secret
3. Enable HTTPS/TLS
4. Configure proper firewall rules
5. Set up backup procedures
6. Monitor resource usage

### Kubernetes Deployment
```bash
kubectl apply -f infrastructure/kubernetes/
```

## Monitoring

### Prometheus Metrics
- JVM metrics
- HTTP request metrics
- Custom KYC metrics
- Database connection pool

### Grafana Dashboards
- System Overview
- KYC Processing Metrics
- GDPR Compliance Metrics
- AI Agent Performance

## Troubleshooting

### Common Issues

**Ollama models not loading**
```bash
docker-compose logs ollama-pull
docker-compose exec ollama ollama list
```

**Database connection errors**
```bash
docker-compose exec postgres pg_isready -U kyc_user
docker-compose logs postgres
```

**Frontend build issues**
```bash
cd kyc-frontend
docker build --no-cache .
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see LICENSE file for details.

## Support

For support, email support@kyc-ai.example.com or open an issue on GitHub.

## Acknowledgments

- LangChain4j team for the excellent AI framework
- Ollama team for local LLM inference
- Spring Boot team for the robust backend framework
