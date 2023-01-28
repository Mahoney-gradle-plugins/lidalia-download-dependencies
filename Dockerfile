# syntax=docker/dockerfile:1.4.0
ARG username=worker
ARG work_dir=/home/$username/work
ARG gid=1000
ARG uid=1001

FROM eclipse-temurin:17.0.1_12-jdk-focal as worker
ARG username
ARG work_dir
ARG gid
ARG uid

RUN addgroup --system $username --gid $gid && \
    adduser --system $username --ingroup $username --uid $uid

USER $username
RUN mkdir -p $work_dir
WORKDIR $work_dir


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

COPY --chown=$username . .

FROM builder as tester
# So the actual build can run without network access. Proves no tests rely on external services.
RUN --mount=type=cache,target=$gradle_cache_dir,gid=$gid,uid=$uid \
    ./gradlew --no-watch-fs || mkdir -p build


FROM scratch as build-output
ARG work_dir

COPY --from=tester $work_dir/plugin/build .

# The builder step is guaranteed not to fail, so that the worker output can be tagged and its
# contents (build reports) extracted.
# You run this as:
# `docker build . --target build-reports --output build-reports && docker build .`
# to retrieve the build reports whether or not the previous line exited successfully.
# Workaround for https://github.com/moby/buildkit/issues/1421
FROM tester as checker
RUN --mount=type=cache,target=$gradle_cache_dir,gid=$gid,uid=$uid \
    ./gradlew --no-watch-fs --stacktrace build

FROM scratch as jarfile
ARG work_dir

COPY --from=checker $work_dir/plugin/build/libs .
