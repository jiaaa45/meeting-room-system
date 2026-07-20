# 會議室預約系統 - 專案代辦清單

> 使用方式:每完成一項,把 `[ ]` 改成 `[x]`。在 VS Code 或 GitHub 上檢視這份檔案時,打勾的項目會自動顯示成勾選框。

---

## 階段 0:環境與專案骨架

- [x] Java 17 / Docker / Git 環境確認
- [x] 專案資料夾建立於不受雲端同步干擾的路徑
- [x] Git 初始化 + 連上 GitHub(jiaaa45/meeting-room-system)
- [x] `.gitignore` 建立並合併
- [x] Spring Boot 4.0.7 專案骨架(Web / JPA / Validation / Postgres / Lombok)
- [x] package 命名修正為合法格式(`com.company.meetingroom`)
- [x] `docker-compose.yml`(PostgreSQL 服務)
- [x] 第一次成功編譯 `mvnw.cmd compile`

## 階段 1:資料庫設計(Flyway Migration)

- [x] V1:`rooms` 表(含 is_active 軟刪除欄位)
- [x] V2:`users` 表(email UNIQUE、role CHECK 限制)
- [x] V3:`reservations` 表(外鍵、status CHECK、時間順序 CHECK、三個複合 Index)
- [x] V4:`reservation_reviews` 表(雙外鍵、action CHECK、Index)
- [x] V5:初始化假資料 SQL(5 位使用者、5 間會議室、10+ 筆預約)

## 階段 2:Java Entity 與基礎架構

- [x] `Room` Entity
- [x] `User` Entity + `Role` Enum
- [x] `Reservation` Entity + `ReservationStatus` Enum
- [x] `ReservationReview` Entity + `ReviewAction` Enum
- [x] Entity 關聯設定(多對一,避免不必要的 EAGER loading)
- [ ] 確認無循環序列化問題

## 階段 3:Room 模組(建立 CRUD 開發模式) 2-3hr

- [x] Room Repository
- [ ] Room DTO(Request / Response 分開)
- [ ] Room Mapper(Entity ↔ DTO 轉換)
- [ ] Room Service
- [ ] Room Controller
  - [ ] `POST /api/rooms`
  - [ ] `GET /api/rooms`
  - [ ] `GET /api/rooms/{id}`
  - [ ] `PUT /api/rooms/{id}`
  - [ ] `DELETE /api/rooms/{id}`(軟刪除,設 is_active = false)

## 階段 4:User 模組 1hr

- [x] User Repository
- [ ] User DTO
- [ ] User Mapper
- [ ] User Service(含 email 重複檢查)
- [ ] User Controller
  - [ ] `POST /api/users`
  - [ ] `GET /api/users`
  - [ ] `GET /api/users/{id}`

## 階段 5:Reservation 核心邏輯(整題靈魂) 4-6hr

- [ ] Reservation DTO(Request / Response)
- [x] Reservation Repository
  - [x] 時間重疊衝突查詢(JPQL / Native Query)
- [ ] Reservation Service - 建立預約驗證規則
  - [ ] roomId / userId 必須存在
  - [ ] startTime < endTime
  - [ ] 不可預約過去時間
  - [ ] 時間需以 30 分鐘為單位
  - [ ] attendeeCount 不可超過會議室容量
  - [ ] 同會議室同時段不可重複預約(時間區間重疊判斷)
  - [ ] rejected / cancelled 不視為佔用
  - [ ] processing / approved 視為佔用
- [ ] Transaction + 併發鎖(Pessimistic Lock 或選定方案)實作
- [ ] Reservation Controller
  - [ ] `POST /api/reservations`
  - [ ] 衝突時回傳 409 Conflict

## 階段 6:退回與審核流程 2-3hr

- [ ] `POST /api/reservations/{id}/cancel-request`
  - [ ] 僅本人可申請
  - [ ] rejected/cancelled 不可再申請
  - [ ] 狀態轉為 cancel_requested
- [ ] `POST /api/reservations/{id}/review`
  - [ ] 僅 REVIEWER/ADMIN 可審核
  - [ ] 寫入 ReservationReview 紀錄
  - [ ] approved → 狀態變 cancelled;rejected → 狀態還原

## 階段 7:查詢類 API 3-4hr

- [ ] `GET /api/reservations`(分頁、排序、多條件篩選)
- [ ] `GET /api/rooms/{roomId}/reservations`
- [ ] `GET /api/reservations/timeline?date=`(避免 N+1)
- [ ] `GET /api/reservations/monthly-summary?year=&month=`
- [ ] `GET /api/rooms/top-used?year=&month=`

## 階段 8:Validation 與錯誤處理 2-3hr

- [ ] Bean Validation 註解(@NotNull、@Email 等)
- [ ] `GlobalExceptionHandler`(@RestControllerAdvice)
  - [ ] 400 / 403 / 404 / 409 / 500 統一格式

## 階段 9:測試 5-8hr

- [ ] Unit Test(JUnit5 + Mockito)- 至少涵蓋 16 項情境
- [ ] Repository Test(@DataJpaTest)
- [ ] Controller Test(@WebMvcTest + MockMvc)
- [ ] Integration Test(Testcontainers)〔加分〕
- [ ] Concurrency Test(模擬雙執行緒搶同時段)〔加分〕
- [ ] JaCoCo 覆蓋率報告〔加分〕

## 階段 10:文件 2-3hr

- [ ] README.md(啟動方式、API 清單、schema 說明、衝突邏輯、transaction/lock、index、測試方式、已完成/未完成、可改善之處)
- [ ] README - Design Decisions
- [ ] README - Requirement Clarification(4 個情境)
- [ ] README - Database Reasoning
- [ ] README - AI Usage Statement
- [ ] DEBUG_NOTES.md(3 個 debug 情境)
- [ ] CODE_REVIEW.md(至少指出 10 個問題)

## 加分項目(選做)

- [ ] Swagger / OpenAPI
- [ ] Spring Security / JWT
- [ ] Audit Log
- [ ] 匯出報表(CSV/Excel)

---
