# CampusHub Architecture Graph

Generated from the current project structure with `/graphify .`.

## System Context

```mermaid
flowchart LR
  user[Student/Admin Browser]
  frontend[Vue 3 + TypeScript Frontend<br/>Vite, Pinia, Vue Router, Axios]
  backend[Spring Boot 3 Backend<br/>Security, MVC, Validation, Mail]
  database[(MySQL 8.0)]
  uploads[(Local Upload Storage<br/>FILE_UPLOAD_PATH)]
  smtp[SMTP Mail Provider]

  user --> frontend
  frontend -->|VITE_API_BASE_URL /api| backend
  frontend -->|VITE_ASSET_BASE_URL /uploads| backend
  backend -->|MyBatis-Plus mappers| database
  backend -->|static resource mapping| uploads
  backend -->|verification email| smtp
```

## Backend Layers

```mermaid
flowchart TB
  subgraph API[Controller Layer]
    AuthController
    UserController
    TaskController
    OrderController
    ReportController
    NotificationController
    FileController
    AnnouncementController
    AdminController
  end

  subgraph Service[Service Layer]
    AuthServiceImpl
    UserServiceImpl
    TaskService
    OrderService
    ReportService
    NotificationService
    FileService
    AdminService
    EmailService
    TokenBlacklistService
    JwtTokenProvider
  end

  subgraph Persistence[Mapper Layer]
    UserMapper
    UserProfileMapper
    VerificationCodeMapper
    TaskMapper
    TaskImageMapper
    FavoriteMapper
    ApplicationMapper
    OrderMapper
    OrderStatusLogMapper
    OrderMessageMapper
    NotificationMapper
    ReviewMapper
    CreditLogMapper
    ReportMapper
    ReportEvidenceMapper
    FileRecordMapper
    AnnouncementMapper
    AdminOperationLogMapper
  end

  AuthController --> AuthServiceImpl
  UserController --> UserServiceImpl
  TaskController --> TaskService
  OrderController --> OrderService
  ReportController --> ReportService
  NotificationController --> NotificationService
  FileController --> FileService
  AnnouncementController --> AdminService
  AdminController --> AdminService
  AdminController --> ReportService

  AuthServiceImpl --> UserMapper
  AuthServiceImpl --> UserProfileMapper
  AuthServiceImpl --> VerificationCodeMapper
  AuthServiceImpl --> JwtTokenProvider
  AuthServiceImpl --> TokenBlacklistService
  AuthServiceImpl --> EmailService

  UserServiceImpl --> UserMapper
  UserServiceImpl --> UserProfileMapper
  UserServiceImpl --> FileService
  UserServiceImpl --> ReviewMapper
  UserServiceImpl --> CreditLogMapper
  UserServiceImpl --> OrderMapper

  TaskService --> TaskMapper
  TaskService --> TaskImageMapper
  TaskService --> ApplicationMapper
  TaskService --> OrderMapper
  TaskService --> OrderStatusLogMapper
  TaskService --> UserProfileMapper
  TaskService --> FavoriteMapper
  TaskService --> CreditLogMapper
  TaskService --> UserMapper
  TaskService --> NotificationService
  TaskService --> FileService

  OrderService --> OrderMapper
  OrderService --> TaskMapper
  OrderService --> UserProfileMapper
  OrderService --> OrderStatusLogMapper
  OrderService --> OrderMessageMapper
  OrderService --> ReviewMapper
  OrderService --> CreditLogMapper
  OrderService --> NotificationService
  OrderService --> FileService

  ReportService --> ReportMapper
  ReportService --> ReportEvidenceMapper
  ReportService --> TaskMapper
  ReportService --> NotificationService
  ReportService --> FileService

  NotificationService --> NotificationMapper
  FileService --> FileRecordMapper

  AdminService --> UserMapper
  AdminService --> UserProfileMapper
  AdminService --> CreditLogMapper
  AdminService --> AdminOperationLogMapper
  AdminService --> AnnouncementMapper
  AdminService --> TaskMapper
  AdminService --> OrderMapper
  AdminService --> ReportMapper
```

## Frontend Routes

```mermaid
flowchart TB
  App[App.vue] --> Router[router/index.ts]
  Router --> Landing["/"]
  Router --> Auth["/login, /register, /verify-email, /forgot-password"]
  Router --> Tasks["/tasks, /tasks/new, /tasks/:id, /tasks/favorites"]
  Router --> Orders["/orders, /orders/:id"]
  Router --> User["/profile, /users/:id"]
  Router --> Notifications["/notifications"]
  Router --> Announcements["/announcements"]
  Router --> Admin["/admin"]

  Auth --> ApiService[services/api.ts]
  Tasks --> ApiService
  Orders --> ApiService
  User --> ApiService
  Notifications --> ApiService
  Announcements --> ApiService
  Admin --> ApiService
  ApiService --> HttpClient[services/http.ts<br/>Axios]
  HttpClient --> BackendApi[Spring Boot /api]
  Router --> AuthStore[stores/auth.ts<br/>Pinia]
```

## Data Model

```mermaid
erDiagram
  USER ||--|| USER_PROFILE : owns
  USER ||--o{ VERIFICATION_CODE : receives
  USER ||--o{ TASK : publishes
  TASK ||--o{ TASK_IMAGE : has
  USER ||--o{ FAVORITE : creates
  TASK ||--o{ FAVORITE : saved_as
  USER ||--o{ APPLICATION : submits
  TASK ||--o{ APPLICATION : receives
  TASK ||--o{ ORDERS : becomes
  USER ||--o{ ORDERS : publisher
  USER ||--o{ ORDERS : provider
  ORDERS ||--o{ ORDER_STATUS_LOG : tracks
  ORDERS ||--o{ ORDER_MESSAGE : contains
  USER ||--o{ NOTIFICATION : receives
  ORDERS ||--o{ REVIEW : reviewed_by
  USER ||--o{ REVIEW : reviewer
  USER ||--o{ CREDIT_LOG : changes
  USER ||--o{ REPORT : reports
  REPORT ||--o{ REPORT_EVIDENCE : has
  FILE_RECORD ||--o{ REPORT_EVIDENCE : attached
  USER ||--o{ FILE_RECORD : uploads
  USER ||--o{ ANNOUNCEMENT : publishes
  USER ||--o{ ADMIN_OPERATION_LOG : writes
```

## High-Traffic Domain Flow

```mermaid
sequenceDiagram
  participant Browser as Frontend View
  participant API as Controller
  participant Service as Domain Service
  participant Mapper as MyBatis Mapper
  participant DB as MySQL
  participant Notify as NotificationService

  Browser->>API: request with JWT
  API->>Service: validate DTO and current user
  Service->>Mapper: read or mutate domain records
  Mapper->>DB: SQL via MyBatis-Plus
  DB-->>Mapper: rows
  Mapper-->>Service: entities
  Service->>Notify: create notification when state changes
  Service-->>API: VO/PageResult
  API-->>Browser: ApiResponse
```

## Source Hotspots

- Backend entrypoint: `backend/src/main/java/com/campushub/CampusHubApplication.java`
- Backend API layer: `backend/src/main/java/com/campushub/controller`
- Backend business layer: `backend/src/main/java/com/campushub/service`
- Backend persistence layer: `backend/src/main/java/com/campushub/mapper`
- Backend database schema: `database/01-schema.sql`
- Frontend entrypoint: `frontend/src/main.ts`
- Frontend routes: `frontend/src/router/index.ts`
- Frontend API client: `frontend/src/services/api.ts` and `frontend/src/services/http.ts`
