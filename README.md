# 🔨 Online Auction Platform

![Project Hero](file:///c:/Users/pzont/.gemini/antigravity/brain/a3869110-6488-49a6-bc89-c77fbf7aa63a/auction_platform_hero_1778084790517.png)

A high-performance, real-time online auction platform built with a robust multi-module Java architecture. This project features a modern JavaFX client with a premium aesthetic and a high-concurrency server backend.

---

## Key Features

- **Real-time Bidding**: Instant bid updates and auction synchronization.
- **Auto-Bidding Engine**: Intelligent automated bidding based on user-defined limits.
- **Premium UI**: Modern desktop experience using **AtlantaFX** and **Ikonli**.
- **Secure Auth**: Integrated user registration and login session management.
- **Multi-module Architecture**: Clean separation of concerns between Client, Server, and Shared modules.
- **Local Persistence**: Reliable data storage using SQLite.

---

## Tech Stack

### Core
- **Language**: Java 21
- **Build System**: Maven 3.x
- **Framework**: JavaFX 17 (UI Framework)

### Libraries & Tools
- **UI Styling**: [AtlantaFX](https://github.com/mkpaz/atlantafx) (Modern CSS theme)
- **Icons**: [Ikonli](https://github.com/kordamp/ikonli) (FontAwesome integration)
- **Database**: SQLite JDBC (Local storage)
- **Data Handling**: Google Gson (JSON serialization)
- **Logging**: SLF4J with Simple Logger

---

## Project Structure

```text
Online-auction-platform/
├── pom.xml                       # Root Maven configuration
├── shared/                       # Shared models and utilities
│   └── src/main/java/com/auction/shared/
│       ├── models/               # Core entities (User, Item, Bid)
│       └── utils/                # Enums and Constants
├── server/                       # Backend logic and API
│   └── src/main/java/com/auction/server/
│       ├── controllers/          # Request handlers
│       ├── services/             # Business logic (Anti-sniping, Logic)
│       └── dao/                  # Database Access Objects
└── client/                       # JavaFX Desktop Application
    ├── src/main/java/com/auction/client/
    │   ├── controllers/          # FXML Controller logic
    │   ├── services/             # Service layer (AuthService, AuctionService)
    │   └── utils/                # Navigation and Session management
    └── src/main/resources/
        ├── views/                # FXML Layouts
        └── styles/               # Custom CSS Design
```

---

## Getting Started

### Prerequisites
- **JDK 21** or higher
- **Maven 3.8+**

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Uet-Retake-Club/Online-auction-platform.git
   cd Online-auction-platform
   ```

2. **Build the project:**
   ```bash
   mvn clean install
   ```

### Running the Application

#### Launch the Client
The client is the primary user interface. Launch it using:
```bash
mvn -pl client javafx:run
```

#### Launch the Server
Ensure the backend is running to handle data and concurrency:
```bash
mvn -pl server exec:java
```

---

## Design Philosophy

This application prioritizes **Visual Excellence**. By utilizing the **AtlantaFX** library, we provide a premium, system-native feel with glassmorphism elements, subtle micro-animations, and a responsive layout that adapts to various window sizes.

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.