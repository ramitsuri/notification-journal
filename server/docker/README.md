# Docker setup

- Get the version file
  from https://github.com/ramitsuri/notification-journal/releases/latest/download/app_version.txt,
  download it and paste it in this directory
- Rename it to current_app_version.txt
- Modify its contents to `10.0`
- Get the script file
  from https://raw.githubusercontent.com/ramitsuri/notification-journal/refs/heads/main/server/scripts/run.sh, download
  it and paste it in this directory
- Create a file exchanges.txt and put exchange names separated by comma
- Run `docker compose up --build -d`                                                                         