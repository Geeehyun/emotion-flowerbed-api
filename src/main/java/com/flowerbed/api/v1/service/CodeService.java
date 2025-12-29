package com.flowerbed.api.v1.service;

import com.flowerbed.api.v1.domain.Code;
import com.flowerbed.api.v1.domain.CodeGroup;
import com.flowerbed.api.v1.dto.CodeGroupResponse;
import com.flowerbed.api.v1.dto.CodeResponse;
import com.flowerbed.api.v1.repository.CodeGroupRepository;
import com.flowerbed.api.v1.repository.CodeRepository;
import com.flowerbed.exception.ErrorCode;
import com.flowerbed.exception.business.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 코드 서비스
 *
 * 공통 코드 조회 서비스입니다.
 * 코드는 자주 변경되지 않으므로 캐싱을 적용합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeService {

    private final CodeGroupRepository codeGroupRepository;
    private final CodeRepository codeRepository;

    /**
     * 모든 코드 그룹 조회 (코드 목록 포함)
     */
    @Cacheable(value = "codeGroups", key = "'all'")
    public List<CodeGroupResponse> getAllCodeGroups() {
        log.debug("Fetching all code groups with codes");
        return codeGroupRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(CodeGroupResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 코드 그룹 조회 (코드 목록 포함)
     */
    @Cacheable(value = "codeGroup", key = "#groupCode")
    public CodeGroupResponse getCodeGroup(String groupCode) {
        log.debug("Fetching code group: {}", groupCode);
        CodeGroup codeGroup = codeGroupRepository.findById(groupCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.CODE_NOT_FOUND,
                        "코드 그룹을 찾을 수 없습니다: " + groupCode));
        return CodeGroupResponse.from(codeGroup);
    }

    /**
     * 특정 그룹의 활성화된 코드 목록 조회
     */
    @Cacheable(value = "codes", key = "#groupCode")
    public List<CodeResponse> getCodesByGroup(String groupCode) {
        log.debug("Fetching codes for group: {}", groupCode);
        return codeRepository.findByCodeGroup_GroupCodeAndIsActiveTrueOrderByDisplayOrderAsc(groupCode)
                .stream()
                .map(CodeResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 그룹의 특정 코드 조회
     */
    @Cacheable(value = "code", key = "#groupCode + '_' + #code")
    public CodeResponse getCode(String groupCode, String code) {
        log.debug("Fetching code: groupCode={}, code={}", groupCode, code);
        Code foundCode = codeRepository.findByCodeGroup_GroupCodeAndCode(groupCode, code)
                .orElseThrow(() -> new BusinessException(ErrorCode.CODE_NOT_FOUND,
                        String.format("코드를 찾을 수 없습니다: groupCode=%s, code=%s", groupCode, code)));
        return CodeResponse.from(foundCode);
    }

    /**
     * USER_TYPE 코드 목록 조회
     */
    public List<CodeResponse> getUserTypes() {
        return getCodesByGroup("USER_TYPE");
    }

    /**
     * EMOTION_CONTROL 코드 목록 조회
     */
    public List<CodeResponse> getEmotionControls() {
        return getCodesByGroup("EMOTION_CONTROL");
    }

    /**
     * 코드명으로 코드값 조회
     * 예: "학생" → "STUDENT"
     */
    public String getCodeByName(String groupCode, String codeName) {
        log.debug("Finding code by name: groupCode={}, codeName={}", groupCode, codeName);
        List<Code> codes = codeRepository.findByCodeGroup_GroupCodeAndIsActiveTrueOrderByDisplayOrderAsc(groupCode);
        return codes.stream()
                .filter(code -> code.getCodeName().equals(codeName))
                .map(Code::getCode)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CODE_NOT_FOUND,
                        String.format("코드를 찾을 수 없습니다: groupCode=%s, codeName=%s", groupCode, codeName)));
    }
}
