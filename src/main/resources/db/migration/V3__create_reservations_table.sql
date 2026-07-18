CREATE TABLE reservations (
    id              BIGSERIAL PRIMARY KEY,
    room_id         BIGINT NOT NULL,
    user_id         BIGINT NOT NULL,
    start_time      TIMESTAMP NOT NULL,
    end_time        TIMESTAMP NOT NULL,
    subject         VARCHAR(200) NOT NULL,
    purpose         TEXT,
    attendee_count  INTEGER NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'processing',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reservations_room
        FOREIGN KEY (room_id) REFERENCES rooms(id),
    CONSTRAINT fk_reservations_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT ck_reservations_status
        CHECK (status IN ('processing', 'approved', 'rejected', 'cancel_requested', 'cancelled')),

    CONSTRAINT ck_reservations_time_order
        CHECK (end_time > start_time)
);

COMMENT ON TABLE reservations IS '會議室預約資料表';

-- 給「檢查會議室時段衝突」用:先鎖定會議室、篩掉不佔用時段的狀態,再比對時間
CREATE INDEX idx_reservations_room_status_time
    ON reservations (room_id, status, start_time, end_time);

-- 給「查某使用者的所有預約」用
CREATE INDEX idx_reservations_user_id
    ON reservations (user_id);

-- 給「查某天/某月 approved 預約」用,例如 timeline、monthly summary
CREATE INDEX idx_reservations_status_start_time
    ON reservations (status, start_time);