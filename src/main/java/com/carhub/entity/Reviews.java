package com.carhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Data
@ToString
@SuperBuilder
@NoArgsConstructor
@Table(name = "reviews")
public class Reviews implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "car_id")
    private Long carId;

    @Column(name = "rating")
    private Long rating;

    @Column(name = "comment")
    private String comment;

    @Column(name = "is_anonymous")
    private Boolean anonymous;

    @Column(name = "is_approved")
    private Boolean approved;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}
