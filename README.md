# Pricing Engine Service

## Overview

The Pricing Engine is a **Spring Boot 3.x** application that provides a service to store and retrieve **financial instrument prices** in a **batch-safe, atomic, and concurrent-safe manner**. It uses **JPA/Hibernate** with an **H2 in-memory database** for development/testing purposes.

This service supports:

- Producers uploading prices in **batches**.
- Consumers querying the **latest price per instrument**.
- Concurrency-safe operations for batch start, upload, completion, and cancellation.
- Atomic visibility: consumers only see completed batches.
- Resilience against incorrect producer or consumer calls.

---

## Architecture & Design

The project follows a **layered architecture**:


### Layers

- **Controller Layer**: Exposes REST APIs for starting batches, uploading CSV files, completing/cancelling batches, and fetching prices.
- **Service Layer**: Contains business logic for batch lifecycle management and price storage.
- **Repository Layer**: JPA repositories (`BatchRepository`, `PriceRepository`) for DB operations.
- **LockManager**: `BatchLockManager` ensures **per-batch concurrency safety** in a single JVM environment.
- **Entities/Records**:
    - `BatchEntity` tracks batch status.
    - `PriceEntity` stores individual price records.
    - `PriceRecord` DTO/record for producer input.

### Key Design Patterns

- **Repository Pattern**: For clean DB access.
- **Locking & Synchronization**: Per-batch `ReentrantLock` for concurrency safety.
- **Transactional Methods**: `@Transactional` ensures atomic writes.
- **Batch Processing**: Producers can upload in chunks.
- **Exception Handling**: Clear runtime exceptions for invalid operations (batch not started, batch cancelled, etc.).
- **Logging**: SLF4J logs for monitoring batch operations.

---

## Tech Stack

| Component             | Technology                              |
|-----------------------| --------------------------------------- |
| **Language**          | Java 21                                 |
| **Framework**         | Spring Boot 3.x                         |
| **Build Tool**        | Maven                                   |
| **Database**          | H2 (in-memory)                          |
| **API Documentation** | Swagger / OpenAPI 3                     |

---

### Testing

- Unit tests use H2 in-memory database.

- Concurrent uploads are tested using ExecutorService.

- **Tests ensure:**

  - Consumers cannot see incomplete batches.

  - Multiple threads uploading the same batch are thread-safe.

  - Batch cancellations remove all uploaded prices.


## Build & Run

#### Build
mvn clean install

#### Run
mvn spring-boot:run

#### Verify Swagger
> Visit 👉 http://localhost:8080/swagger-ui/index.html


## Usage Flow

#### Endpoints:
### 1. Start a Batch
##### Description:
> Starts a new batch for price uploads.

**Request:** `POST /api/batch/start/{batchId}`


| Name    | Type   | Description            |
| ------- | ------ | ---------------------- |
| batchId | String | Unique ID of the batch |

**Response:**

```
200 OK – Batch started successfully

400 Bad Request – Batch already exists
```

### 2. Upload Prices (CSV)
##### Description:
 > Upload prices for a batch. Prices are only visible to consumers after the batch is completed.

**Request:**`POST /api/batch/upload/{batchId}`
`Content-Type: multipart/form-data
Body: CSV file or JSON array` 

| Name    | Type   | Description                       |
| ------- | ------ | --------------------------------- |
| batchId | String | ID of the batch to upload prices  |
| file    | File   | CSV file containing price records |

CSV file Format : 
```
GOOG,2025-12-15T10:00:00Z,{"price":143.07}
AAPL,2025-12-15T10:00:01Z,{"price":182.45}
MSFT,2025-12-15T10:00:02Z,{"price":315.20}
```
`Response (200 OK)`
```
Upload accepted for batch batch-2. Call /complete to make data visible.
```

### 2. Complete a Batch
##### Description:
> Marks a batch as completed. Prices become visible to consumers.

#### Endpoint:
**REQUEST:** `POST /api/batch/complete/{batchId}`

**Responses:**

```
200 OK – Batch completed successfully
404 Not Found – Batch not found
400 Bad Request – Batch not in STARTED state
```

### 2. cancel a Batch
##### Description:
> Cancels a batch. All uploaded prices for the batch are removed.

#### Endpoint:
**REQUEST:** `POST /api/batch/cancel/{batchId}`

**Responses:**

```
200 OK – Batch cancelled successfully
404 Not Found – Batch not found
```
### 4. Fetch Last Prices
##### Description:
> Fetches the latest prices for a given list of instrument IDs. Only considers completed batches.

**Request:**`GET /api/prices?ids=GOOG,AAPL,MSFT`

| Name | Type   | Description                            |
| ---- | ------ | -------------------------------------- |
| ids  | String | Comma-separated list of instrument IDs |

`Response (200 OK)`
```json
[
  {"instrumentId":"GOOG","asOf":"2025-12-15T10:00:00Z","payloadJson":"{\"price\":143.07}"},
  {"instrumentId":"AAPL","asOf":"2025-12-15T10:00:01Z","payloadJson":"{\"price\":182.45}"}
]
```