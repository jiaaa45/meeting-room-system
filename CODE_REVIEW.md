# CODE_REVIEW.md
 
以下針對考題提供的範例程式碼進行 Code Review。原始程式碼如下:
 
```java
@PostMapping("/reserve")
public Reservation reserve(@RequestBody Map<String, String> body) {
    Long roomId = Long.valueOf(body.get("roomId"));
    Long userId = Long.valueOf(body.get("userId"));
    LocalDateTime start = LocalDateTime.parse(body.get("startTime"));
    LocalDateTime end = LocalDateTime.parse(body.get("endTime"));
    List<Reservation> reservations = reservationRepository.findAll();
    for (Reservation r : reservations) {
        if (r.getRoom().getId().equals(roomId)
            && r.getStartTime().equals(start)) {
            throw new RuntimeException("room booked");
        }
    }
    Room room = roomRepository.findById(roomId).get();
    User user = userRepository.findById(userId).get();
    Reservation reservation = new Reservation();
    reservation.setRoom(room);
    reservation.setUser(user);
    reservation.setStartTime(start);
    reservation.setEndTime(end);
    reservation.setStatus("approved");
    return reservationRepository.save(reservation);
}
```
 
以下列出發現的問題,依類別分組,每一項都附上本專案實際採用的對照做法。
 
---
 
## 1. API Request 設計 / DTO 缺失
 
**問題**:用 `@RequestBody Map<String, String> body` 接收請求,不是一個有明確結構的 DTO 類別。
 
**為什麼有問題**:
- 完全沒有型別檢查,`body.get("roomId")` 打錯字(例如打成 `"room_id"`)只會在執行時得到 `null`,不會在編譯期被發現
- 沒有任何 IDE 自動補全或型別提示,可讀性差
- 考題明確禁止「不可使用 `Map<String, String>` 作為主要 request body」
**本專案做法**:定義 `ReservationRequestDto`,搭配 `@Valid` 觸發 Bean Validation,每個欄位有明確型別跟驗證規則。
 
---
 
## 2. 完全沒有 Validation
 
**問題**:所有欄位都沒有做任何合法性檢查——`roomId`/`userId` 可以是 `null`、`startTime` 可以晚於 `endTime`、可以預約過去時間、`attendeeCount` 甚至根本沒有這個欄位(容量超過限制完全沒被檢查)。
 
**本專案做法**:`ReservationRequestDto` 上有 `@NotNull`、`@NotBlank`、`@Positive` 等註解;`ReservationService.create` 裡有完整的時間邏輯、容量檢查等商業規則驗證。
 
---
 
## 3. `Long.valueOf(body.get("roomId"))` 沒有處理轉換失敗
 
**問題**:如果前端傳來的 `roomId` 不是合法數字字串(例如空字串、非數字文字),`Long.valueOf(...)` 會直接拋出 `NumberFormatException`,而這個例外沒有被任何機制攔截,最終會變成一個很醜的 500 錯誤,並可能洩漏 Stack Trace。
 
**本專案做法**:DTO 直接宣告 `Long roomId` 型別,Spring 在反序列化 JSON 時就會處理型別轉換,轉換失敗會被 `GlobalExceptionHandler` 統一攔截處理成 400。
 
---
 
## 4. Exception Handling:直接丟 `RuntimeException`,而且訊息是英文寫死字串
 
**問題**:`throw new RuntimeException("room booked")`——這是最泛用的例外類別,呼叫端沒辦法區分「衝突」跟其他任何錯誤;而且**這個例外完全沒有被任何地方攔截處理**,最終會變成 500,不是考題要求的 409。
 
**本專案做法**:定義專屬的 `ReservationConflictException`,搭配 `GlobalExceptionHandler` 統一攔截並轉換成 409 Conflict,附帶考題規定的錯誤格式(`timestamp`、`status`、`error`、`message`、`path`)。
 
---
 
## 5. `Optional.get()` 沒有處理「找不到」的情況
 
**問題**:`roomRepository.findById(roomId).get()` 跟 `userRepository.findById(userId).get()`——如果傳入一個不存在的 `roomId`,`Optional.get()` 會直接拋出 `NoSuchElementException`,同樣沒有被攔截,變成不明確的 500 錯誤,而不是考題要求的 404。
 
**本專案做法**:`roomRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(...))`,搭配 `GlobalExceptionHandler` 轉成 404。
 
---
 
## 6. 查詢效能:用 `findAll()` 撈全部資料,在記憶體裡用迴圈找衝突
 
**問題**:`reservationRepository.findAll()` 會把資料庫裡**所有**預約紀錄全部撈進記憶體,再用 `for` 迴圈一筆一筆比對——資料量一旦成長到幾萬、幾十萬筆,這支 API 會直接變得極慢,而且非常浪費記憶體。
 
**本專案做法**:透過帶條件的 JPQL 查詢(`findConflictingReservations`),讓資料庫直接篩選出「這間會議室、這個狀態、這個時間範圍」的預約,搭配 Index 加速,不會撈出不相關的資料。
 
---
 
## 7. 時間重疊判斷邏輯完全錯誤
 
**問題**:`r.getStartTime().equals(start)`——只比對開始時間**是否完全相同**,這是考題明確點名禁止的錯誤做法。如果既有預約是 10:00-12:00,新預約是 11:00-11:30,兩者開始時間不同,這段程式碼完全偵測不到衝突,會誤判成「可以預約」。
 
**本專案做法**:採用區間重疊公式 `existing.start < new.end AND existing.end > new.start`,能正確判斷任何形式的時間重疊(部分重疊、完全包含等)。
 
---
 
## 8. Status 用 magic string,不是 Enum
 
**問題**:`reservation.setStatus("approved")`——直接寫死一個字串。這裡還有一個更嚴重的邏輯錯誤:**新建立的預約直接設成 `"approved"`(已核准),完全跳過審核流程**,這跟考題描述的「新預約應該是 processing 狀態,等待審核」完全不符。而且字串沒有任何拼字檢查,打錯字(例如 `"aproved"`)不會被編譯器抓到。
 
**本專案做法**:使用 `ReservationStatus` Enum,新建立的預約狀態固定是 `ReservationStatus.PROCESSING`,搭配資料庫的 CHECK 限制雙重把關。
 
---
 
## 9. 完全沒有 Transaction 保護
 
**問題**:這段程式碼裡「檢查衝突」跟「真正寫入」是兩個分開的動作,中間完全沒有 `@Transactional` 保護。如果兩個請求幾乎同時執行,兩者都會各自查到「目前沒有衝突」,然後都成功寫入,造成同一個時段被兩筆預約同時佔用。
 
**本專案做法**:`ReservationService.create` 標註 `@Transactional`,搭配衝突查詢上的悲觀鎖(`@Lock(LockModeType.PESSIMISTIC_WRITE)`),確保檢查跟寫入是不可分割的單一操作。
 
---
 
## 10. Concurrency:沒有任何併發控制機制
 
**問題**:即使前一點提到的 Transaction 補上了,如果沒有搭配鎖定機制,單純的 Transaction 依然無法防止「兩個 Transaction 同時各自查到沒有衝突」的情況——這是這段程式碼最根本、最危險的問題,直接對應考題的核心考點。
 
**本專案做法**:見上一點,悲觀鎖確保同一時間只有一個請求能真正完成「查詢並寫入」的完整流程,其他請求必須排隊等待。
 
---
 
## 11. Response 格式:直接回傳 Entity
 
**問題**:方法回傳型別是 `Reservation`(Entity),不是 DTO。這會導致:
- Entity 內部所有欄位(包含不該讓前端看到的技術細節)全部暴露
- 如果 Entity 之間有雙向關聯(例如 Room 也持有 Reservation 清單),序列化時可能發生**循環參照**,導致 `StackOverflowError`
- 考題明確禁止「不可直接回傳 Entity 作為 API response」
**本專案做法**:所有 Controller 方法都回傳專屬的 Response DTO(如 `ReservationResponseDto`),經過 Mapper 轉換,只暴露必要欄位。
 
---
 
## 12. 沒有檢查 attendeeCount 是否超過會議室容量
 
**問題**:整段程式碼完全沒有讀取或驗證 `attendeeCount` 這個欄位,考題明確要求的「人數不可超過會議室容量」規則完全沒有實作。
 
**本專案做法**:`ReservationService.create` 裡有明確的 `validateAttendeeCount` 檢查步驟。
 
---
 
## 13. 沒有檢查「不可預約過去時間」與「30 分鐘單位」等規則
 
**問題**:考題規定的多項驗證規則(不可預約過去時間、時間需以 30 分鐘為單位)在這段程式碼裡完全缺席。
 
**本專案做法**:`ReservationService.validateTimeRules` 方法統一處理這些規則。
 