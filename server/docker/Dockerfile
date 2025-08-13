FROM ubuntu:latest

WORKDIR /app

RUN apt-get update && apt-get install -y openjdk-17-jre locales wget && rm -rf /var/lib/apt/lists/*

#COPY app.jar .
COPY run.sh .
COPY exchanges.txt ./server/
COPY current_app_version.txt .

RUN chmod +x run.sh

EXPOSE 8080

CMD ["./run.sh"]