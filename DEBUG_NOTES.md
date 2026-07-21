# DEBUG_NOTES.md
 
本文件回答考題指定的三個 Debug 情境,並附上本專案開發過程中實際遇到的真實除錯案例。
 
---
 
## Debug 情境 1:重複預約(兩人成功預約同一間會議室、同一個時段)
 
### 1. 會先檢查哪些程式?
 
依序檢查:
1. **`ReservationRepository.findConflictingReservations`** 的 JPQL 語法本身——時間重疊判斷公式(`start < end AND end > start`)有沒有寫對,`blockingStatuses` 有沒有正確傳入
2. **`ReservationService.create`** 裡呼叫衝突檢查跟真正 `save()` 之間的順序——是否真的先檢查再寫入,還是被改成先寫入再檢查
3. **`@Transactional` 跟 `@Lock` 註解**是否還在——這兩個是防止併發寫入的最後一道防線,如果不小心被誰移除了,衝突檢查跟實際寫入之間會出現「空隙」,讓兩個幾乎同時的請求都通過檢查
### 2. 會查哪些資料表?
 
直接查 `reservations` 表,找出 `room_id`、`start_time`、`end_time` 完全相同(或有重疊)、`status` 同時是 `PROCESSING`/`APPROVED` 的兩筆(或多筆)紀錄:
 
```sql
SELECT room_id, start_time, end_time, status, created_at
FROM reservations
WHERE room_id = ?
ORDER BY start_time, created_at;
```
 
觀察這兩筆紀錄的 `created_at` 時間差多少——如果差距在幾毫秒到幾百毫秒之間,高度指向「併發競爭條件(Race Condition)」,而不是邏輯判斷寫錯。
 
### 3. 會如何重現這個問題?
 
單純用 `curl` 依序發送兩次請求測不出來(因為是依序執行,不是同時)。需要用**多執行緒同時觸發**才能重現,例如用 `ExecutorService` 開兩個執行緒,搭配 `CountDownLatch` 讓兩個執行緒盡量在同一時刻各自呼叫 `create()`,模擬「真正同時」發生的情境。
 
### 4. 會如何修正?
 
確認 `ReservationRepository.findConflictingReservations` 上的 `@Lock(LockModeType.PESSIMISTIC_WRITE)`,以及呼叫它的 `ReservationService.create` 方法上的 `@Transactional` 都存在且生效——這兩者缺一不可,只有 `@Lock` 沒有 `@Transactional` 包住,鎖定範圍只有查詢那一瞬間,起不到保護作用。
 
可以透過查看應用程式日誌(開啟 `spring.jpa.properties.hibernate.format_sql=true`),確認實際送到資料庫的 SQL 有沒有出現 `FOR NO KEY UPDATE`(PostgreSQL 表達悲觀鎖的語法),來驗證鎖真的有生效。
 
### 5. 會如何寫測試避免再次發生?
 
寫一個 Concurrency Test:用 `ExecutorService` 開兩個執行緒、搭配 `CountDownLatch` 讓兩者盡量同時觸發同一筆預約請求,斷言「兩個結果裡只有一個成功、另一個丟出 `ReservationConflictException`」,並且查詢資料庫確認最終 `reservations` 表裡不存在兩筆衝突的紀錄。這種測試比單元測試更能真實驗證併發控制的正確性。
 
---
 
## Debug 情境 2:Timeline API 很慢(`GET /api/reservations/timeline?date=` 超過 5 秒)
 
### 1. 如何確認瓶頸在 API 邏輯還是 SQL?
 
開啟 `spring.jpa.show-sql=true`,重新呼叫這支 API 一次,計算「從發出請求到回應」的總時間,再對照 log 裡每一條 SQL 前後的時間戳記,算出 SQL 執行本身佔了多少比例。如果 SQL 執行時間本身就佔掉 4 秒以上,問題在資料庫層;如果 SQL 都很快(幾十毫秒),但總時間還是很長,問題可能在 Java 端的資料組裝邏輯(例如巢狀迴圈效能不佳)。
 
### 2. 如何檢查是否有 N+1 Query?
 
**最直接的方法:數 SQL 執行次數。** 呼叫一次 Timeline API,計算 log 裡出現幾次 `Hibernate:` 開頭的段落。**正常設計應該是固定次數**(本專案設計為 2 次:一次查會議室、一次用 `JOIN FETCH` 帶出預約跟使用者)。如果實際數字**隨會議室或預約筆數變動**(例如有 10 間會議室就出現 11 次查詢),就是 N+1 問題的明確訊號。
 
### 3. 會建立哪些 Index?
 
依照這支 API 實際的查詢條件設計:
- 針對 `rooms.is_active` 建 Index(如果會議室數量龐大,篩選啟用中的會議室會受惠)
- 針對 `reservations (status, start_time)` 建複合 Index(本專案已建立 `idx_reservations_status_start_time`),讓「篩選 APPROVED、限定某一天」的查詢能直接命中索引,不用全表掃描
### 4. 會如何調整查詢?
 
1. 確認關聯查詢有沒有用 `JOIN FETCH` 一次把 `Room`/`User` 帶出來,而不是依賴 Lazy Loading 讓 Hibernate 自動逐筆補查
2. 確認分組邏輯(依 `room_id` 分組)是在 Java 記憶體裡用 `Collectors.groupingBy` 完成,而不是對每個房間各自重新查一次資料庫
3. 如果日期篩選條件效率不佳,確認有沒有用「左閉右開區間」(`>= 當天 00:00 AND < 隔天 00:00`)而不是用容易誤判時區或精度問題的日期函式比對
### 5. 資料量持續成長時如何重新設計?
 
- 考慮把 Timeline 拆成分頁或限制查詢範圍(例如只回傳有預約的會議室,而不是永遠回傳全部會議室)
- 針對高頻率查詢的「常用日期範圍」(例如未來 7 天)建立快取層,減少重複計算
- 若會議室數量本身破千,可以評估將 `rooms` 依部門或樓層做垂直切分,避免單一查詢掃描過大的資料範圍
---
 
## Debug 情境 3:`LazyInitializationException`
 
### 1. 這個錯誤通常為什麼發生?
 
`Reservation` Entity 裡的 `room`、`user` 關聯設定為 `FetchType.LAZY`,代表查詢 Reservation 本身時不會自動撈出關聯的 Room/User,只有真正呼叫 `.getRoom()`/`.getUser()` 的那一刻,Hibernate 才會發出額外查詢去補資料。**這個「補資料」的動作,必須在資料庫連線(Session)還開著的時候才能完成**。如果程式碼是在 Transaction 已經結束、Session 已經關閉之後才去呼叫這些 getter,Hibernate 找不到活著的連線可以用,就會拋出這個例外。
 
最常見的觸發時機:Service 方法回傳一個 Entity(不是 DTO),回到 Controller 層才嘗試存取它的關聯欄位——但 `@Transactional` 通常只包住 Service 方法本身,Controller 已經在 Transaction 範圍之外了。
 
### 2. 會如何修正?
 
本專案的做法是**在 Service 層、Transaction 還沒結束前,就把 Entity 轉換成 DTO**,確保所有需要用到的關聯資料,都在資料庫連線還活著的時候提前撈出來、轉換完畢。DTO 本身不帶有任何 Lazy 屬性,離開 Transaction 範圍後可以安全地被 Controller、甚至序列化成 JSON,不會再觸發任何延遲查詢。
 
### 3. 會選擇 fetch join、DTO projection,還是調整 transaction scope?
 
三種都有用到,依情境選擇:
- **一般情況**:在 Service 層轉換成 DTO(本專案 Mapper 的做法),簡單直接,適合大多數場景
- **需要一次撈大量資料、避免 N+1**:用 `JOIN FETCH`(本專案 Timeline、Monthly Summary 用的做法),一條 SQL 把關聯資料一起帶出來
- **關聯資料很龐大、只需要少數欄位**:可以考慮用 DTO Projection(直接讓查詢回傳部分欄位組成的介面,像本專案的 `StatusCountProjection`),連完整 Entity 都不用載入,效能更好
### 4. 為什麼不建議直接把所有關聯都改成 EAGER?
 
`EAGER` 代表「不管用不用得到,每次查詢主體都順便撈出關聯資料」。表面上看起來能避免 `LazyInitializationException`,但代價是:
1. **效能浪費**——查一筆 Reservation 列表,卻不小心把每一筆的 Room、User 全部撈出來,即使呼叫端根本不需要
2. **容易間接引發 N+1**——如果 Entity 之間有多層關聯,EAGER 可能會一路連鎖觸發下去,撈出遠比預期更多的資料
3. **失去掌控權**——EAGER 是寫死在 Entity 上的全域設定,任何用到這個 Entity 的地方都被迫套用,沒辦法依情境彈性決定「這次要不要撈關聯資料」;LAZY + 顯式的 DTO/JOIN FETCH,才能讓每個查詢自己決定需要什麼
