#!/bin/bash

set -e

PLATFORM="linux/amd64"
TAG="latest"

echo "🚀 [1/4] 전체 프로젝트 Gradle 빌드 시작 (bootJar)..."

./gradlew clean bootJar

echo "📦 [2/4] 모듈별 도커 이미지 빌드 시작..."

MODULES=(
    "admin:admin-service"
    "core/core-api:api-service"
    "multicast-server:multicast-service"
)

for ENTRY in "${MODULES[@]}"; do
    DIR="${ENTRY%%:*}"
    IMAGE_NAME="hyeonseong1010/${ENTRY#*:}"

    echo "--------------------------------------------------"
    echo "🛠️ 빌드 중: $IMAGE_NAME (경로: $DIR)"
    echo "--------------------------------------------------"

    docker build --platform "$PLATFORM" \
        -t "$IMAGE_NAME:$TAG" \
        -f "./$DIR/Dockerfile" \
        "./$DIR"

    echo "📤 업로드 중: $IMAGE_NAME:$TAG"
    docker push "$IMAGE_NAME:$TAG"
done

echo "--------------------------------------------------"
echo "✅ [3/4] 모든 이미지 빌드 완료!"
docker images | grep -E "admin-service|api-service|multicast-service"

echo "💡 [4/4] 다음 단계: docker-compose up -d 를 실행하여 컨테이너를 띄우세요."