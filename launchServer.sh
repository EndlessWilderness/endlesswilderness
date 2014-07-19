#!/bin/bash

ew="endlesswilderness"

dir="$(ls -1d ../$ew-*)"
lock="lock.pid"
if [ ! -e "$dir/$lock" ];then
  rm -r $dir
  git pull
  ./gradlew distZip
  cd ..
  unzip $ew/build/distributions/$ew*.zip

  dir="$(ls -1d $ew-*)"
  cd $dir
  sed -i "s/com.jdydev.ew.Launcher/com.jdydev.ew.server.EWServer/g" bin/$ew

  nohup bin/$ew > server.log 2>&1 &
  echo $! > $lock
  echo "Launched EWServer"
else
  echo "Not launching EWServer, lock file exists $dir/$lock"
fi