#!/bin/bash

cd /home/ubuntu

if [ -f ./app.pid ]; then
  echo "기존 프로세스를 종료합니다."
  kill -15 $(cat ./app.pid)
  rm ./app.pid
fi
nohup java -jar /home/ubuntu/build/libs/*.jar & echo $! > ./app.pid
