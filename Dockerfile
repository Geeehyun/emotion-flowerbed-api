############################################
# 1단계: 빌드 전용 스테이지
# - Spring Boot 애플리케이션을 jar로 빌드하기 위한 단계
# - JDK가 필요하므로 JDK 이미지 사용
############################################
FROM eclipse-temurin:21-jdk AS build

# 컨테이너 내부 작업 디렉토리 설정
WORKDIR /app

# 현재 프로젝트 전체를 컨테이너로 복사
# (gradlew, build.gradle, src 등 전부 포함)
COPY . .

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

# Spring Boot 실행 가능한 jar 생성
# -x test : 테스트는 CI/CD 속도와 안정성을 위해 스킵
RUN ./gradlew clean bootJar -x test


############################################
# 2단계: 실행 전용 스테이지
# - 실제 운영에서 컨테이너가 실행될 단계
# - JDK는 필요 없고 JRE만 있으면 되므로 더 가벼운 이미지 사용
############################################
FROM eclipse-temurin:21-jre

# 실행용 작업 디렉토리
WORKDIR /app

# 빌드 스테이지에서 생성된 jar 파일만 복사
# → 소스코드, gradle, 캐시 등은 포함되지 않음
COPY --from=build /app/build/libs/*.jar app.jar

# 이 컨테이너가 8080 포트를 사용한다는 "정보"
# (실제 포트 오픈은 docker run / docker-compose에서 설정)
EXPOSE 8080

# 컨테이너가 시작될 때 실행할 명령
# = java -jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
