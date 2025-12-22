package com.flowerbed.api.v1.repository;

import com.flowerbed.api.v1.domain.CodeGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 코드 그룹 Repository
 */
public interface CodeGroupRepository extends JpaRepository<CodeGroup, String> {

    /**
     * 표시 순서로 정렬하여 모든 코드 그룹 조회
     */
    List<CodeGroup> findAllByOrderByDisplayOrderAsc();

    /**
     * 수정 가능한 코드 그룹만 조회
     */
    List<CodeGroup> findByIsEditableTrueOrderByDisplayOrderAsc();
}
