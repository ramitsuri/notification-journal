#!/bin/bash

# Check if new version needs to be downloaded
cd "$HOME"/usbdrv/notification-journal || exit

current_version=$(cat current_app_version.txt)
current_version_code="${current_version//.}"
current_version_code="$((current_version_code))"
if [[ $current_version_code == 0 ]]
then
  echo "Failed to get current app version"
  java -jar app.jar
  exit
fi

wget https://github.com/ramitsuri/notification-journal/releases/latest/download/app_version.txt
new_version=$(cat app_version.txt) || exit
new_version_code="${new_version//.}"
new_version_code="$((new_version_code))"
if [[ $new_version_code == 0 ]]
then
  echo "Failed to get new app version"
  java -jar app.jar
  exit
fi

if [[ $new_version_code -gt $current_version_code ]]
then
  rm app.jar
  wget https://github.com/ramitsuri/notification-journal/releases/latest/download/app.jar
  rm current_version.txt
  mv app_version.txt current_version.txt
else
  rm app_version.txt
fi

java -jar app.jar

