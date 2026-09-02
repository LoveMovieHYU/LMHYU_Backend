# 1. Base Image: 런타임에는 JRE만 있어도 되지만, 호환성을 위해 JDK 17 유지
FROM eclipse-temurin:17-jdk-jammy

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. Timezone 설정 (KST) - 배포 서버 시간과 로그 시간을 맞추기 위함
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 4. 빌드된 JAR 파일 복사 (ARG를 사용해 유연성 확보)
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 5. 실행
ENTRYPOINT ["java", "-jar", "-Duser.timezone=Asia/Seoul", "app.jar"]