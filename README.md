# TravelPal
Plan it, share it, play it with TravelPal
 
* TravelPal is a web application that allows users to create, share and download travel plans for any city
* TravelPal is especially useful for travellers who plan a stay from one day to two weeks or more and want to turn the plan into an adventure by checking in at attractions to earn XP, levels and badges
* To start using TravelPal, user first has to create an account, after that he searches for a city and builds a plan from its attractions, lets the AI suggest one, or downloads a plan made by another user
* For any help you can contact our support and maintainers Bartul Kalinić, Renzo Rell
# Project Specification
 
* Backend: Java + Spring Boot (Maven)
* Frontend: Thymeleaf + Bootstrap
* Database: PostgreSQL (Flyway migrations)
* Security: Spring Security
* AI Integration: Google Gemini API (plan builder) – planned
* Attractions & Cities: OpenStreetMap (Nominatim search)
* Maps & Walking Routes: OpenStreetMap + Leaflet, OSRM walking router
* Communication: REST API (JSON)
* Containers: Docker + Docker Compose
* Version Control: Git + GitHub

# How to run

You only need **[Docker Desktop](https://www.docker.com/products/docker-desktop/)** (running) and an internet connection.
Java and Maven are **not** needed: Docker builds the app itself.

1. Download the project and open a terminal in the project folder:
   ```bash
   git clone https://github.com/Java-Seminar-2025/TravelPal.git
   cd TravelPal
   ```
2. Build and start the database and the app:
   ```bash
   docker compose --profile app up --build
   ```
   The first build takes a few minutes (it downloads Maven dependencies). It is ready when the log shows
   `Started TravelPalApplication`.
3. Open **http://localhost:8080**

Stop it with `Ctrl+C`, or `docker compose --profile app down`. Data is kept in the Docker volume `travelpal-data`;
`docker compose --profile app down -v` deletes it and the next start creates fresh demo data.

> `--profile app` is needed: without it only the database starts (that is what developers use while running the app from IntelliJ).
> Ports 8080 and 5432 must be free.

### Test accounts (password for all: `password`)

| Username | Role |
|---|---|
| `admin` | ADMIN – can edit/delete any plan, sees the Admin page |
| `user` | USER |
| `bkalinic`, `renco13` | ADMIN |
| `sarah` | USER |

Demo plans with map stops and walking routes: **Florence in 3 Days** and **Split Old Town & Marjan** (Plans page).

### For developers

Run `TravelPalApplication` from IntelliJ (or `./mvnw spring-boot:run`) with Docker Desktop running: Spring Boot starts
the database from `compose.yaml` automatically. Tests: `./mvnw test` (no Docker needed).

