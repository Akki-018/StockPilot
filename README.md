# StockPilot

A desktop inventory management application built with **Java 25**, **JavaFX**, and **SQLite**. StockPilot provides a product catalogue, stock movement tracking, and an analytics dashboard in a clean, industrial-style interface.

## Features

- **Inventory management** — add, edit, delete, search, and filter products.
- **Stock monitoring** — track available quantities and minimum-stock thresholds.
- **Stock movements** — record Stock In and Stock Out operations with quantity validation.
- **Transaction history** — retain movement records with previous and resulting stock balances, timestamps, and notes.
- **Data integrity** — inventory updates and their transaction records are committed together using JDBC transactions; failed operations are rolled back.
- **Dashboard** — view inventory metrics, stock-condition indicators, category charts, stock movement charts, and recent activity.
- **Light and dark themes** — switch themes while using the application; the selected preference is saved.
- **Local persistence** — product and transaction data are stored in a SQLite database.

## Tech stack

| Technology | Use |
|---|---|
| Java 25 | Application language |
| JavaFX 25 | Desktop user interface |
| SQLite | Local relational database |
| SQLite JDBC 3.47.2.0 | Java-to-SQLite connectivity |
| Maven | Dependency and build management |

## Getting started

### Prerequisites

- JDK 25
- Git (to clone the repository)

The project includes the Maven Wrapper, so a separate Maven installation is not required.

### 1. Clone the repository

~~~bash
git clone https://github.com/Akki-018/StockPilot.git
cd StockPilot
~~~

### 2. Run the application

**Windows (Command Prompt):**

~~~cmd
mvnw.cmd javafx:run
~~~

**macOS / Linux:**

~~~bash
./mvnw javafx:run
~~~

Make sure Java 25 is available to the terminal through JAVA_HOME or PATH.

## Database

On first launch, StockPilot initializes its SQLite database at:

~~~text
data/stockpilot.db
~~~

The data directory and database file are local application data and are excluded from Git. Each local checkout maintains its own database.

You can optionally choose a different data directory by setting the JVM system property stockpilot.data.dir.

## Project structure

The application is organized into separate packages:

~~~text
src/
└── main/
    ├── java/com/stockpilot/
    │   ├── controller/   # JavaFX views and UI interaction
    │   ├── dao/          # Database queries and persistence
    │   ├── database/     # SQLite configuration and initialization
    │   ├── model/        # Application data models
    │   ├── service/      # Business logic and inventory operations
    │   ├── util/         # Theme and UI helper utilities
    │   └── MainApp.java  # Application entry point
    └── resources/
        └── styles/       # Base, light, and dark CSS themes
~~~

## Notes

- StockPilot is a locally run desktop application; it does not require a hosted database or backend service.
- The SQLite database is created on the machine where the application is run. Existing local inventory data is not included in this repository.
- A packaged installer or standalone release is not currently provided; run the project through the Maven Wrapper.

## License

No license has been specified for this repository yet. Until one is added, all rights remain with the copyright holder.
