package com.company.meetingroom.specification;

import com.company.meetingroom.entity.Reservation;
import com.company.meetingroom.entity.ReservationStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class ReservationSpecifications {

    public static Specification<Reservation> dateFrom(LocalDate dateFrom) {
        return (root, query, cb) -> dateFrom == null ? null
                : cb.greaterThanOrEqualTo(root.get("startTime"), dateFrom.atStartOfDay());
    }

    public static Specification<Reservation> dateTo(LocalDate dateTo) {
        return (root, query, cb) -> dateTo == null ? null
                : cb.lessThan(root.get("startTime"), dateTo.plusDays(1).atStartOfDay());
    }

    public static Specification<Reservation> roomId(Long roomId) {
        return (root, query, cb) -> roomId == null ? null
                : cb.equal(root.get("room").get("id"), roomId);
    }

    public static Specification<Reservation> roomName(String roomName) {
        return (root, query, cb) -> (roomName == null || roomName.isBlank()) ? null
                : cb.like(root.get("room").get("name"), "%" + roomName + "%");
    }

    public static Specification<Reservation> username(String username) {
        return (root, query, cb) -> (username == null || username.isBlank()) ? null
                : cb.like(root.get("user").get("username"), "%" + username + "%");
    }

    public static Specification<Reservation> status(ReservationStatus status) {
        return (root, query, cb) -> status == null ? null
                : cb.equal(root.get("status"), status);
    }
}
