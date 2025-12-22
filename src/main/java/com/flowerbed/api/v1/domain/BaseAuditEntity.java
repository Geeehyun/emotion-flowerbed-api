package com.flowerbed.api.v1.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Audit 정보를 포함하는 Base Entity
 *
 * 모든 엔티티는 이 클래스를 상속받아 생성/수정/삭제 정보를 자동으로 추적합니다.
 * - created_at: 생성 시각 (자동 설정)
 * - created_by: 생성자 사용자 ID (자동 설정)
 * - updated_at: 수정 시각 (자동 설정)
 * - updated_by: 수정자 사용자 ID (자동 설정)
 * - deleted_at: 삭제 시각 (Soft Delete 시 수동 설정)
 * - deleted_by: 삭제자 사용자 ID (Soft Delete 시 수동 설정)
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 255)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 255)
    private String deletedBy;

    /**
     * Soft Delete 시 삭제 정보 설정
     * @param deletedBy 삭제자 사용자 ID
     */
    public void markAsDeleted(String deletedBy) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }
}
