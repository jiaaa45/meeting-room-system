-- 步驟 1:先拿掉舊的(小寫版)CHECK 限制,這樣才能把資料轉成大寫
ALTER TABLE reservations DROP CONSTRAINT ck_reservations_status;
ALTER TABLE reservation_reviews DROP CONSTRAINT ck_reviews_action;

-- 步驟 2:現在限制已經拿掉了,可以安全地把既有資料轉成大寫
UPDATE reservations SET status = UPPER(status);

-- 步驟 3:資料都轉好了,現在套用新的(大寫版)CHECK 限制
ALTER TABLE reservations ADD CONSTRAINT ck_reservations_status
    CHECK (status IN ('PROCESSING', 'APPROVED', 'REJECTED', 'CANCEL_REQUESTED', 'CANCELLED'));

ALTER TABLE reservation_reviews ADD CONSTRAINT ck_reviews_action
    CHECK (action IN ('APPROVED', 'REJECTED'));

-- 步驟 4:修正欄位預設值,避免以後新增資料時又塞進小寫值
ALTER TABLE reservations ALTER COLUMN status SET DEFAULT 'PROCESSING';