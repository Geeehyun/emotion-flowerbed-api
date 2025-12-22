package com.flowerbed.api.v1.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * 코드 엔티티
 *
 * 공통 코드 데이터를 관리합니다.
 * 예: STUDENT, TEACHER, ADMIN (ROLE 그룹)
 *     DEEP_BREATHING, WALK, DRAW (EMOTION_CONTROL 그룹)
 */
@Entity
@Table(name = "codes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_group_code", columnNames = {"group_code", "code"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE codes SET deleted_at = NOW() WHERE code_id = ?")
@Where(clause = "deleted_at IS NULL")
public class Code extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code_id")
    private Long codeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_code", nullable = false, foreignKey = @ForeignKey(name = "fk_codes_group"))
    private CodeGroup codeGroup;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "code_name", nullable = false, length = 100)
    private String codeName;

    @Column(length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "extra_value1", length = 200)
    private String extraValue1;

    @Column(name = "extra_value2", length = 200)
    private String extraValue2;

    @Column(name = "extra_value3", length = 200)
    private String extraValue3;

    public Code(CodeGroup codeGroup, String code, String codeName,
                String description, Integer displayOrder) {
        this.codeGroup = codeGroup;
        this.code = code;
        this.codeName = codeName;
        this.description = description;
        this.isActive = true;
        this.displayOrder = displayOrder;
    }

    public Code(CodeGroup codeGroup, String code, String codeName,
                String description, Integer displayOrder,
                String extraValue1, String extraValue2, String extraValue3) {
        this(codeGroup, code, codeName, description, displayOrder);
        this.extraValue1 = extraValue1;
        this.extraValue2 = extraValue2;
        this.extraValue3 = extraValue3;
    }

    public void updateInfo(String codeName, String description, Integer displayOrder) {
        this.codeName = codeName;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public void updateExtraValues(String extraValue1, String extraValue2, String extraValue3) {
        this.extraValue1 = extraValue1;
        this.extraValue2 = extraValue2;
        this.extraValue3 = extraValue3;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}
