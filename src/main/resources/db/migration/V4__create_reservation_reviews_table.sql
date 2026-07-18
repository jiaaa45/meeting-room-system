CREATE TABLE reservation_reviews (
    id              BIGSERIAL PRIMARY KEY,
    reservation_id  BIGINT NOT NULL,
    reviewer_id     BIGINT NOT NULL,
    action          VARCHAR(20) NOT NULL,
    comment         TEXT,
    reviewed_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reviews_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations(id),
    CONSTRAINT fk_reviews_reviewer
        FOREIGN KEY (reviewer_id) REFERENCES users(id),

    CONSTRAINT ck_reviews_action
        CHECK (action IN ('approved', 'rejected'))
);

CREATE INDEX idx_reviews_reservation_id
    ON reservation_reviews (reservation_id);

COMMENT ON TABLE reservation_reviews IS '預約退回審核紀錄表';