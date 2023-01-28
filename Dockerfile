# syntax=docker/dockerfile:1.4.0
ARG username=worker
ARG work_dir=/home/$username/work
ARG gid=1000
ARG uid=1001

FROM eclipse-temurin:18.0.2.1_1-jdk-focal as worker
ARG username
ARG work_dir
ARG gid
ARG uid

RUN addgroup --system $username --gid $gid && \
    adduser --system $username --ingroup $username --uid $uid

USER $username
RUN mkdir -p $work_dir
WORKDIR $work_dir


# Copy across all the *.gradle.kts files in a separate stage
# This will not get any layer caching if anything in the context has changed, but when we
# subsequently copy them into a different stage that stage *will* get layer caching. So if none of
# the *.gradle.kts files have changed, a subsequent command will also get layer caching.
FROM alpine as gradle-files
RUN --mount=type=bind,target=/docker-context \
    mkdir -p /gradle-files/gradle && \
    cd /docker-context/ && \
    find . -name "*.gradle" -exec cp --parents "{}" /gradle-files/ \; && \
    find . -name "*.gradle.kts" -exec cp --parents "{}" /gradle-files/ \; && \
    find . -name "libs.versions.toml" -exec cp --parents "{}" /gradle-files/ \; && \
    find . -name "*module-info.java" -exec cp --parents "{}" /gradle-files/ \;


FROM worker as builder
ARG username
ARG gid
ARG uid

# The single use daemon will be unavoidable in future so don't waste time trying to prevent it
ENV GRADLE_OPTS='-Dorg.gradle.daemon=false'
ARG gradle_cache_dir=/home/$username/.gradle/caches

# Download gradle in a separate step to benefit from layer caching
COPY --chown=$username gradle/wrapper gradle/wrapper
COPY --chown=$username gradlew gradlew
COPY --chown=$username gradle.properties gradle.properties
RUN ./gradlew --version

# Do all the downloading in one step...
COPY --chown=$username --from=gradle-files /gradle-files ./
RUN --mount=type=cache,target=$gradle_cache_dir,gid=$gid,uid=$uid \
    ./gradlew --no-watch-fs --stacktrace downloadDependencies

COPY --chown=$username . .

FROM builder as tester
# So the actual build can run without network access. Proves no tests rely on external services.
RUN --mount=type=cache,target=$gradle_cache_dir,gid=$gid,uid=$uid \
    --network=none \
    ./gradlew --no-watch-fs --offline build || mkdir -p build


FROM scratch as build-output
ARG work_dir

COPY --from=tester $work_dir/build .

# The builder step is guaranteed not to fail, so that the worker output can be tagged and its
# contents (build reports) extracted.
# You run this as:
# `docker build . --target build-output --output build && docker build .`
# to retrieve the build reports whether or not the previous line exited successfully.
# Workaround for https://github.com/moby/buildkit/issues/1421
FROM tester as checker
RUN --mount=type=cache,target=$gradle_cache_dir,gid=$gid,uid=$uid \
    --network=none \
    ./gradlew --no-watch-fs --stacktrace --offline build

FROM scratch as jarfile
ARG work_dir

COPY --from=checker $work_dir/build/libs .
