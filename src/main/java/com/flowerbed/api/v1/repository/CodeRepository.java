package com.flowerbed.api.v1.repository;

import com.flowerbed.api.v1.domain.Code;
import com.flowerbed.api.v1.domain.CodeGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 코드 Repository
 */
public interface CodeRepository extends JpaRepository<Code, Long> {

    /**
     * 그룹 코드로 활성화된 코드 목록 조회
     */
    List<Code> findByCodeGroup_GroupCodeAndIsActiveTrueOrderByDisplayOrderAsc(String groupCode);

    /**
     * 그룹 코드로 모든 코드 목록 조회 (비활성 포함)
     */
    List<Code> findByCodeGroup_GroupCodeOrderByDisplayOrderAsc(String groupCode);

    /**
     * 그룹 코드와 코드값으로 조회
     */
    Optional<Code> findByCodeGroup_GroupCodeAndCode(String groupCode, String code);

    /**
     * CodeGroup 엔티티로 조회
     */
    List<Code> findByCodeGroupAndIsActiveTrueOrderByDisplayOrderAsc(CodeGroup codeGroup);

    /**
     * 활성화된 모든 코드 조회
     */
    List<Code> findByIsActiveTrueOrderByCodeGroup_DisplayOrderAscDisplayOrderAsc();
}
