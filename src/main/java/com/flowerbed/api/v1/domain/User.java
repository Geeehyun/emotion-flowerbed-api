package com.flowerbed.api.v1.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE user_sn = ?")
@Where(clause = "deleted_at IS NULL")
public class User extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_sn")
    private Long userSn;

    @Column(unique = true, nullable = false)
    private String userId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "user_type_cd", length = 50)
    private String userTypeCd;

    @Column(name = "school_code")
    private String schoolCode;

    @Column(name = "school_nm")
    private String schoolNm;

    @Column(name = "class_code")
    private String classCode;

    @Column(name = "emotion_control_cd")
    private String emotionControlCd;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Diary> diaries = new ArrayList<>();

    public User(String userId, String password, String name) {
        this.userId = userId;
        this.password = password;
        this.name = name;
    }

    public User(String userId, String password, String name, String userTypeCd) {
        this.userId = userId;
        this.password = password;
        this.name = name;
        this.userTypeCd = userTypeCd;
    }
}
