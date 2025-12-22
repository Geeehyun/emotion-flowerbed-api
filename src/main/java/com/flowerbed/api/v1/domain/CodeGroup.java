package com.flowerbed.api.v1.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 코드 그룹 엔티티
 *
 * 공통 코드의 그룹을 관리합니다.
 * 예: ROLE (권한/역할), EMOTION_CONTROL (감정 제어 활동)
 */
@Entity
@Table(name = "code_groups")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CodeGroup extends BaseAuditEntity {

    @Id
    @Column(name = "group_code", length = 50)
    private String groupCode;

    @Column(name = "group_name", nullable = false, length = 100)
    private String groupName;

    @Column(length = 500)
    private String description;

    @Column(name = "is_editable", nullable = false)
    private Boolean isEditable = true;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @OneToMany(mappedBy = "codeGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Code> codes = new ArrayList<>();

    public CodeGroup(String groupCode, String groupName, String description,
                     Boolean isEditable, Integer displayOrder) {
        this.groupCode = groupCode;
        this.groupName = groupName;
        this.description = description;
        this.isEditable = isEditable;
        this.displayOrder = displayOrder;
    }

    public void updateInfo(String groupName, String description, Integer displayOrder) {
        this.groupName = groupName;
        this.description = description;
        this.displayOrder = displayOrder;
    }
}
