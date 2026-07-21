# 會議室預約系統 Backend Demo


## 技術棧

- Java 17
- Spring Boot 4.0.7
- Spring Data JPA / Hibernate
- PostgreSQL 16
- Flyway(資料庫版本管理)
- Docker / Docker Compose
- Maven
- JUnit 5 / Mockito / AssertJ / MockMvc

---

## 一、專案啟動方式

### 1. 前置需求

- Java 17 以上
- Docker / Docker Compose
- Git

### 2. Clone 專案

```bash
git clone https://github.com/jiaaa45/meeting-room-system.git
cd meeting-room-system
```

### 3. 啟動 PostgreSQL

```bash
docker compose up -d
```

### 4. 啟動應用程式

```bash
mvnw.cmd spring-boot:run
```

（Mac / Linux 使用 `./mvnw spring-boot:run`）

應用程式啟動後,Flyway 會自動執行資料庫 migration 並建立初始假資料(5 位使用者、5 間會議室、12 筆預約)。

服務啟動於:`http://localhost:8081`

---

## 二、Docker Compose 使用方式

`docker-compose.yml` 目前僅包含 PostgreSQL 服務,方便開發階段快速起停資料庫,不用每次修改程式碼都重建整個 image。

```bash
docker compose up -d      # 啟動資料庫(背景執行)
docker compose down       # 停止並移除容器(資料保留在 volume 中)
docker compose down -v    # 停止並清空所有資料(重新來過)
```

資料庫連線資訊:
- Host: `localhost:5432`
- Database: `meeting_room`
- User: `meeting_room_user`
- Password: `meeting_room_pass`

---

## 三、API 清單

### 會議室管理

| Method | Path | 說明 |
|---|---|---|
| POST | `/api/rooms` | 新增會議室 |
| GET | `/api/rooms` | 查詢會議室列表 |
| GET | `/api/rooms/{id}` | 查詢單一會議室 |
| PUT | `/api/rooms/{id}` | 修改會議室資料 |
| DELETE | `/api/rooms/{id}` | 停用會議室(軟刪除) |
| GET | `/api/rooms/{roomId}/reservations` | 查詢該會議室所有預約 |
| GET | `/api/rooms/top-used?year=&month=` | 查詢使用率最高的前三間會議室 |

### 使用者管理

| Method | Path | 說明 |
|---|---|---|
| POST | `/api/users` | 新增使用者 |
| GET | `/api/users` | 查詢使用者列表 |
| GET | `/api/users/{id}` | 查詢單一使用者 |

### 預約管理

| Method | Path | 說明 |
|---|---|---|
| POST | `/api/reservations` | 建立預約 |
| GET | `/api/reservations` | 預約總覽(分頁、排序、多條件篩選) |
| POST | `/api/reservations/{id}/cancel-request` | 申請退回預約 |
| POST | `/api/reservations/{id}/review` | 審核退回申請 |
| GET | `/api/reservations/timeline?date=` | 每日時段表 |
| GET | `/api/reservations/monthly-summary?year=&month=` | 每月狀態統計 |

---

## 四、Database Schema 說明

### rooms(會議室)

| 欄位 | 型別 | 說明 |
|---|---|---|
| id | BIGSERIAL PK | |
| name | VARCHAR(100) NOT NULL | |
| capacity | INTEGER NOT NULL | |
| floor | VARCHAR(20) | |
| location | VARCHAR(200) | |
| is_active | BOOLEAN NOT NULL DEFAULT TRUE | 軟刪除標記 |
| created_at / updated_at | TIMESTAMP | |

### users(使用者)

| 欄位 | 型別 | 說明 |
|---|---|---|
| id | BIGSERIAL PK | |
| username | VARCHAR(100) NOT NULL | |
| email | VARCHAR(150) NOT NULL UNIQUE | |
| department | VARCHAR(100) | |
| role | VARCHAR(20) NOT NULL,CHECK(USER/REVIEWER/ADMIN) | |
| created_at / updated_at | TIMESTAMP | |

### reservations(預約)

| 欄位 | 型別 | 說明 |
|---|---|---|
| id | BIGSERIAL PK | |
| room_id | BIGINT NOT NULL,FK → rooms.id | |
| user_id | BIGINT NOT NULL,FK → users.id | |
| start_time / end_time | TIMESTAMP NOT NULL,CHECK(end_time > start_time) | |
| subject | VARCHAR(200) NOT NULL | |
| purpose | TEXT | |
| attendee_count | INTEGER NOT NULL | |
| status | VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',CHECK(PROCESSING/APPROVED/REJECTED/CANCEL_REQUESTED/CANCELLED) | |
| previous_status | VARCHAR(20) | 記錄申請退回前的原始狀態,供審核拒絕時還原 |
| created_at / updated_at | TIMESTAMP | |

### reservation_reviews(審核紀錄)

| 欄位 | 型別 | 說明 |
|---|---|---|
| id | BIGSERIAL PK | |
| reservation_id | BIGINT NOT NULL,FK → reservations.id | |
| reviewer_id | BIGINT NOT NULL,FK → users.id | |
| action | VARCHAR(20) NOT NULL,CHECK(APPROVED/REJECTED) | |
| comment | TEXT | |
| reviewed_at | TIMESTAMP NOT NULL | |
| created_at | TIMESTAMP NOT NULL | |

**設計原因**:四張表對應四個獨立的業務概念(會議室、使用者、預約、審核紀錄),每張表都有明確的單一職責。Room/Reservation 都保留軟刪除或狀態欄位而非真的刪除資料,是為了保留歷史紀錄的完整性(已有預約紀錄的會議室不該被物理刪除,已完成的預約不該因退回而消失)。

---

## 五、預約衝突判斷邏輯

判斷公式(取自考題規格):

```
existing.start_time < new_end_time
AND existing.end_time > new_start_time
```

只要新舊兩筆預約的時間區間有任何重疊(部分重疊、完全包含、完全相同),上述條件就會成立。只比對 `startTime` 是否相同會漏掉「新預約完全落在既有預約中間」這種情況,因此必須採用區間重疊判斷。

`rejected` 與 `cancelled` 狀態不視為佔用,`processing` 與 `approved` 視為佔用——這個規則透過 Service 層傳入 Repository 查詢的 `blockingStatuses` 參數控制,不寫死在 SQL 裡,方便未來規則調整。

---

## 六、Transaction / Lock 設計說明

**選用方式**:悲觀鎖(Pessimistic Lock),透過 JPA 的 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 搭配 `@Transactional` 實作,對應到 PostgreSQL 的 `SELECT ... FOR NO KEY UPDATE`。

**使用的方法**:`ReservationService.create()` 是主要有 `@Transactional` 的寫入方法,因為它牽涉到「查詢是否衝突」與「寫入新預約」兩個步驟,必須包在同一個 Transaction 內,搭配悲觀鎖,確保這兩步之間不會被其他請求插隊。

**為什麼選擇悲觀鎖**:會議室預約屬於「衝突機率不低、衝突代價高」的場景(兩人同時搶同一時段是可預期的正常情境,不是極端邊界案例),悲觀鎖能直接透過資料庫層級的鎖定機制,確保同一時間只有一個請求能真正完成寫入,邏輯直觀、實作簡單。

**限制**:悲觀鎖會讓其他請求等待,流量大時可能造成請求堆積、回應變慢。且鎖定範圍是「符合條件的既有預約」,如果查詢條件設計不當(例如少了 `room_id` 篩選),鎖定範圍可能過大,反而降低併發效能。

**流量變大時的因應**:可考慮改用樂觀鎖(在 Reservation 加上 `version` 欄位,靠版本號衝突偵測),或改用 PostgreSQL 的 Exclusion Constraint 直接在資料庫層級禁止時間重疊,兩者都能減少鎖等待、提升吞吐量,但都需要額外處理「衝突發生後」的重試邏輯。

---

## 七、Index 設計說明

### reservations 表

- `idx_reservations_room_status_time (room_id, status, start_time, end_time)`——支援衝突檢查查詢,先鎖定會議室、篩狀態、再比對時間
- `idx_reservations_user_id (user_id)`——支援查詢單一使用者的所有預約
- `idx_reservations_status_start_time (status, start_time)`——支援 Timeline API、Monthly Summary 這類「先篩狀態、再看時間範圍」的查詢

### reservation_reviews 表

- `idx_reviews_reservation_id (reservation_id)`——支援查詢某筆預約的審核歷程

**使用到這些 Index 的查詢**:建立預約時的衝突檢查、Timeline API、Monthly Summary、依會議室查詢預約、依使用者查詢預約。

**資料量增加到 100 萬筆時的調整**:
- Timeline / Monthly Summary 這類「掃描整個時間範圍」的查詢會最先變慢,可考慮依 `start_time` 做分區(Partitioning),或針對常查詢的月份建立實體化視圖(Materialized View)
- 用 `EXPLAIN ANALYZE` 檢查實際執行計畫,確認 Index 真的有被使用,而不是被 Query Planner 判定為「全表掃描更快」而略過

**不適合盲目建立 Index 的欄位**:`purpose`(TEXT 長文字,很少作為查詢條件)、`created_at`(目前沒有查詢需求用到它,建了也是浪費空間跟拖慢寫入速度)。Index 越多,寫入時需要同步維護的成本越高,不該對「用不到」的欄位建立。

---

## 八、如何執行測試

```bash
mvnw.cmd test
```

會依序執行:
- Unit Test(`ReservationServiceTest`)——用 Mockito 隔離資料庫,測試商業邏輯本身
- Repository Test(`ReservationRepositoryTest`,`@DataJpaTest`)——連接真實 PostgreSQL,驗證 JPQL/Native Query 正確性
- Controller Test(`ReservationControllerTest`,`@WebMvcTest`)——驗證 HTTP 層的路由、驗證、錯誤處理

執行 Repository Test 前,請確認 PostgreSQL 容器已經啟動(`docker compose up -d`)。

---

## 九、已完成項目與未完成項目

### 已完成

- 完整資料庫設計(4 張表、外鍵、CHECK 限制、Index)
- Room / User 完整 CRUD
- Reservation 建立邏輯(全部驗證規則 + 衝突檢查 + 悲觀鎖)
- 退回申請與審核流程(含角色權限控制)
- 5 支查詢類 API(總覽篩選分頁、依會議室查詢、Timeline、月統計、使用率排行)
- 全域錯誤處理(400/403/404/409/500)
- Unit Test / Repository Test / Controller Test(共 30 個測試,全數通過)

### 未完成(待補)

- Integration Test(Testcontainers)
- Concurrency Test(模擬多執行緒搶同一時段)
- JaCoCo 覆蓋率報告
- Swagger / OpenAPI
- Spring Security / JWT
- Audit Log
- 匯出報表功能

---

## 十、你認為可以改善的地方

1. **目前所有 API 都沒有身份驗證機制**,`userId`/`reviewerId` 都是直接從 request body 傳入,實務上應該搭配 JWT 從 token 解析出真實身份,避免使用者冒充他人操作。
2. **併發測試目前只驗證了悲觀鎖的 SQL 語法(`FOR NO KEY UPDATE`)有生效**,還沒有寫真正模擬「兩個執行緒同時搶同一時段」的測試,這是驗證併發控制正確性最直接的方式,應優先補上。
3. **`GlobalExceptionHandler` 目前把所有未預期錯誤都記錄成籠統的 500**,可以再細分,例如針對資料庫連線失敗、外部服務逾時等不同情境給更精確的錯誤分類與監控告警。
4. **Timeline / Monthly Summary API 目前沒有分頁**,如果會議室數量或月預約量非常大,回應內容可能過於龐大,可考慮加上分頁或限制查詢範圍。

---

## 十一、Design Decisions

1. **為什麼這樣設計 Room、User、Reservation、ReservationReview 的資料表?**
   四個資料表對應四個獨立的業務實體,關聯清楚(Reservation 多對一 Room / User,ReservationReview 多對一 Reservation / Reviewer)。Room 跟 Reservation 都保留狀態欄位而非物理刪除,避免破壞歷史紀錄的完整性。

2. **Reservation status 為什麼這樣設計?**
   五種狀態(PROCESSING / APPROVED / REJECTED / CANCEL_REQUESTED / CANCELLED)對應考題描述的完整生命週期,加上 `previous_status` 欄位讓「退回被拒絕」時能還原成正確的原始狀態,而不是寫死還原成單一固定值。

3. **為什麼 rejected / cancelled 的預約不應該阻擋新預約?**
   這兩種狀態代表這個時段實際上「沒有真正被使用」——rejected 代表申請沒有通過,cancelled 代表已經取消,兩者都不該繼續佔用會議室資源。

4. **你如何判斷兩筆預約時間是否重疊?**
   見上方「預約衝突判斷邏輯」章節,採用區間重疊公式,而非只比對開始時間。

5. **你選擇在哪一層處理商業邏輯?為什麼?**
   商業邏輯集中在 Service 層,搭配少數「屬於物件自身狀態轉換規則」的邏輯(如 `requestCancel`)放進 Entity 的 domain method。Controller 只負責 HTTP 轉接,Repository 只負責純粹的資料查詢,不參雜業務規則判斷(例如「哪些狀態算佔用」由 Service 決定,傳入 Repository,而非寫死在查詢裡)。

6. **哪些 API 有使用 transaction?為什麼?**
   所有會寫入資料的 Service 方法都使用 `@Transactional`(`create`、`cancelRequest`、`review`、Room 的 `create`/`update`/`deactivate`、User 的 `create`),確保多步驟操作的原子性。純查詢方法使用 `@Transactional(readOnly = true)`,讓 Hibernate 可以跳過部分追蹤機制以提升效能。

7. **如果重新設計一次,會修改哪三個地方?**
   - 一開始就先確定「資料庫 CHECK 限制」跟「Java Enum」的大小寫慣例一致,而不是先用小寫寫 CHECK 限制,之後才發現跟 Java Enum 大寫慣例衝突,得靠額外的 migration 補救
   - 會更早導入 JWT 驗證機制,而不是把它當成加分項最後才考慮,因為它牽涉到「誰可以操作誰的資料」這個貫穿全專案的核心概念
   - 會提早寫 Concurrency Test,而不是留到最後——併發控制的正確性,光靠看 SQL log 沒辦法完全確認,應該儘早用真實的多執行緒場景驗證

---

## 十二、Requirement Clarification

### 情境 1:預約時間邊界(10:00-11:00 vs 11:00-12:00)

**這是否算時間衝突?** 不算。

**系統如何判斷?** 套用衝突公式 `existing.start < new.end AND existing.end > new.start`:`10:00 < 12:00` 為真,但 `11:00 > 11:00` 為假,因此整體判定為不衝突——這正確反映「前一場會議結束的瞬間,下一場可以立刻開始」的常理。

**如何寫測試驗證?** `ReservationRepositoryTest` 裡已有專門測試「時間不重疊」的案例,採用類似的邊界值(既有預約結束時間 = 新預約開始時間)可以再補一個明確的邊界測試,驗證這個臨界點行為正確。

### 情境 2:退回申請與新預約(cancel_requested 佔用問題)

**cancel_requested 是否仍應佔用會議室?** 應該。

**理由:** 在審核者做出最終決定之前,系統無法確定這筆預約最終會不會真的取消(審核者也可能拒絕退回申請,恢復原狀)。若在 `cancel_requested` 階段就放開時段讓別人預約,一旦審核者拒絕退回,會出現兩筆預約同時佔用同一時段的衝突情況。

**在審核通過前,其他人能不能預約同一時段?** 不能——本專案的 `BLOCKING_STATUSES` 目前只包含 `PROCESSING` 跟 `APPROVED`,**這裡刻意記錄一個目前系統尚未涵蓋的已知限制**:`cancel_requested` 目前並未被視為佔用狀態,這與上述推論的理想行為有落差,是後續應該修正的項目(把 `CANCEL_REQUESTED` 加入 `BLOCKING_STATUSES` 常數即可)。

### 情境 3:會議室停用

**已存在的未來預約要不要自動取消?** 不會自動取消,保留原有預約,但因為會議室不再開放,理論上這些預約後續應由管理員或系統另外處理(例如通知申請人另覓場地),本專案目前未實作這個自動化流程。

**新預約是否應該被禁止?** 應該——目前的 Room Service `getById`/`create` 沒有對「已停用會議室建立新預約」做出額外限制,這是一個應該補上的驗證(在 `ReservationService.create` 檢查 `room.getIsActive()`)。

**Timeline API 是否仍要顯示這間會議室?** 目前 `findByIsActiveTrueOrderByNameAsc()` 只查詢啟用中的會議室,因此停用的會議室**不會**出現在 Timeline 裡——這符合「已停用的資源不該再顯示給使用者選擇」的直覺。

### 情境 4:容量超過限制

**應該直接拒絕,還是允許建立 processing 狀態等待審核?** 本專案選擇**直接拒絕**(拋出 `InvalidReservationException`,回傳 400)。

**理由:** 容量限制是物理事實(會議室真的容不下那麼多人),不像「是否核准使用」屬於管理裁量範圍。允許超過容量的請求進入 `processing` 狀態,只是把「應該當下就能判斷是錯誤」的請求,延後到審核階段才處理,徒增審核者的負擔。

---

## 十三、Database Reasoning

### 預約衝突 SQL

```sql
SELECT r FROM Reservation r
WHERE r.room.id = :roomId
  AND r.status IN :blockingStatuses
  AND r.startTime < :endTime
  AND r.endTime > :startTime
```

**為什麼這樣可以判斷時間區間重疊?** 見上方「預約衝突判斷邏輯」章節的完整推導(基於笛摩根定律,從「不衝突」的反面推導而來)。

**為什麼不能只判斷 startTime 是否相同?** 會漏掉「新預約完全落在既有預約中間」等重疊情境,例如既有 10:00-12:00、新的 11:00-11:30,兩者 startTime 不同但確實重疊。

**rejected / cancelled 狀態如何排除?** 透過 `blockingStatuses` 這個由 Service 層傳入的參數控制,Repository 本身不寫死規則。

### Index 設計

見上方「Index 設計說明」章節。

### Group By 查詢

**如何依 status 統計數量?**
```sql
SELECT r.status AS status, COUNT(r) AS count
FROM Reservation r
WHERE r.startTime >= :monthStart AND r.startTime < :monthEnd
GROUP BY r.status
```

**如何查詢指定月份?** 用 `[月份第一天 00:00:00, 下個月第一天 00:00:00)` 這個左閉右開區間篩選,避免「月底最後一天」被排除在外的邊界錯誤。

**日期區間使用 startTime 還是 createdAt?為什麼?** 使用 `startTime`。因為「六月的預約統計」在業務語意上指的是「六月份要開會的預約」,不是「六月份被建立的預約」——一筆在 5 月底建立、但預約時間是 6 月初的預約,應該算進 6 月統計,而不是 5 月。

---

## 十四、AI Usage Statement

本專案開發過程中使用 Claude(Anthropic)作為輔助工具,採「家教式」逐步引導的方式進行——每個步驟都由開發者本人在自己的電腦上實際執行、驗證,並在遇到問題時親自貼出錯誤訊息討論。

**AI 協助的部分**:
- 專案架構規劃、程式碼撰寫的初稿與解釋
- 遇到 Spring Boot 4.0.7 相關的多次框架升級變動(Flyway 自動設定機制改變、`@DataJpaTest` 與 `@AutoConfigureTestDatabase` 套件拆分、`@WebMvcTest` 套件搬遷、Jackson 3 全面改用 `tools.jackson` 命名空間取代 `com.fasterxml.jackson`)時,協助搜尋官方文件查證正確解法

**開發者自行驗證與修改的部分**:
- 每一步驟的程式碼都是在本機環境實際編譯、啟動、測試過,而非憑空產生
- 多次錯誤是靠實際貼出終端機輸出的完整錯誤訊息、逐步排查後才解決的,包含 package 命名不一致、VS Code 編輯器快取導致的假性錯誤、測試資料汙染既有 V5 假資料等問題
- Requirement Clarification 章節中的判斷與取捨,均由開發者確認後採用

**AI 產生內容出現錯誤並修正的案例**:
- 最初設計 `V6` migration(修正 status/action 大小寫)時,順序寫反(先轉換資料才移除舊限制),導致 Flyway migration 執行失敗,後續調整為「先移除限制、再轉換資料、最後套用新限制」的正確順序
- 多次因為套件版本或位置查證不夠即時,導致提供的 import 路徑錯誤,最終都靠實際查詢 Spring Boot 4.x 官方 API 文件確認正確答案後修正