<h1 align="center">🗺️ 研校地图 (YanXiaoMap)</h1>

<p align="center">
  <strong>Graduate School Selection Platform</strong> — Interactive Map for Comparing Universities
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?logo=springboot" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3.x-4FC08D?logo=vuedotjs" alt="Vue 3">
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/TypeScript-5.x-3178C6?logo=typescript" alt="TypeScript">
  <img src="https://img.shields.io/badge/license-MIT-green" alt="License">
</p>

---

## 📖 Overview

YanXiaoMap is an interactive map-based platform that helps graduate school applicants discover, compare, and select universities across China. Built with Spring Boot + Vue3 + AMap (高德地图), it provides a visually intuitive way to explore admission data.

**Core workflow**: Search schools by province/city/ranking → View on interactive map → Multi-school side-by-side comparison → Admin dashboard for data management.

---

## ✨ Features

### 🗺️ Interactive Map
- AMap (高德地图) JS API integration with custom school markers
- Cluster markers at zoom-out, detail popups at zoom-in
- Province/city/ranking filters

### 📊 Multi-School Comparison
- Side-by-side comparison of admission scores
- Discipline and major association queries
- Historical trends visualization

### 🔐 Authentication & Security
- JWT-based user authentication
- Role-based access control (RBAC)
- XSS filter, rate limiting, CORS configuration

### 🛠️ Admin Dashboard
- CRUD for schools, majors, disciplines
- Data change logging and audit trail
- Swagger API documentation

---

## 🏛️ Architecture

```
┌──────────────────────────────────────┐
│          Vue3 Frontend (Vite)        │
│   AMap Container │ Compare View      │
│   Search Panel   │ Admin Dashboard   │
└────────────┬─────────────────────────┘
             │ REST API (JWT)
┌────────────▼─────────────────────────┐
│      Spring Boot Backend (Java)      │
│   Controllers → Services → Mappers   │
│   Security Filter Chain              │
└────────────┬─────────────────────────┘
             │ MyBatis-Plus
┌────────────▼─────────────────────────┐
│         PostgreSQL Database          │
│   Schools │ Majors │ Admissions      │
└──────────────────────────────────────┘
```

---

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- PostgreSQL 16+
- Maven 3.8+

### Backend

```bash
cd backend
cp .env.example .env      # Configure DB credentials
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Then open `http://localhost:5173`.

### Docker

```bash
docker-compose up -d
```

---

## 📁 Project Structure

```
yanxiaomap/
├── backend/
│   └── src/main/java/com/yanxiaomap/
│       ├── controller/       # REST API endpoints
│       ├── service/          # Business logic
│       ├── mapper/           # MyBatis-Plus data access
│       ├── entity/           # JPA entities
│       ├── config/           # Security, CORS, Swagger
│       └── security/         # JWT, XSS, rate limiting
├── frontend/
│   └── src/
│       ├── views/            # Page components
│       ├── components/map/   # AMap integration
│       ├── stores/           # Pinia state management
│       └── api/              # Axios API clients
├── database/                 # Schema + migration scripts
└── docker-compose.yml
```

---

## 🔧 Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3, MyBatis-Plus, Maven |
| Frontend | Vue 3, TypeScript, Vite, Pinia |
| Map | AMap (高德地图) JS API 2.0 |
| Database | PostgreSQL (prod), H2 (dev) |
| Security | Spring Security, JWT, RBAC |
| API Docs | Swagger / OpenAPI 3 |
| Deployment | Docker, Docker Compose |

---

## 📝 License

MIT
