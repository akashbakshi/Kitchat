FROM gradle:5.6.2-jdk8 AS builder
COPY --chown=gradle:gradle . /stage

WORKDIR /stage
RUN rm -rf /stage/build/libs
RUN gradle build --no-daemon
RUN ls /stage/build/libs

FROM openjdk:8-jre-alpine

ENV APPLICATION_USER ktor
RUN adduser -D -g '' $APPLICATION_USER

RUN mkdir /app
RUN chown -R $APPLICATION_USER /app

USER $APPLICATION_USER

COPY --from=builder /stage/build/libs/guessQ.jar /app/guessQ.jar
WORKDIR /app

CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:InitialRAMFraction=2", "-XX:MinRAMFraction=2", "-XX:MaxRAMFraction=2", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "guessQ.jar"]

