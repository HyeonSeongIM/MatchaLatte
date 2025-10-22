#!/bin/bash

cd /home/ubuntu

if [ -f ./app.pid ]; then
  PID=$(cat ./app.pid)
  echo "기존 프로세스(PID: $PID)를 종료합니다. (kill -15)"
  kill -15 $PID

  # 👇 [개선!] 프로세스가 종료될 때까지 최대 10초간 기다림
  for i in {1..10}; do
    if ! ps -p $PID > /dev/null; then
      echo "프로세스(PID: $PID)가 성공적으로 종료되었습니다."
      break
    fi
    echo "종료 대기 중... ($i/10)"
    sleep 1
  done

  # 👇 10초가 지나도 살아있으면 강제 종료 (kill -9)
  if ps -p $PID > /dev/null; then
    echo "프로세스가 10초간 응답이 없어 강제 종료합니다. (kill -9)"
    kill -9 $PID
  fi

  rm ./app.pid
fi

if [ -f /home/ubuntu/.env ]; then
  echo ".env 파일에서 환경변수를 로드합니다."
  export $(cat /home/ubuntu/.env | xargs)
fi

echo "새 애플리케이션을 시작합니다. (Profile: dev)"
nohup java -jar /home/ubuntu/build/libs/*.jar --spring.profiles.active=dev > /home/ubuntu/app.log 2>&1 & echo $! > ./app.pid