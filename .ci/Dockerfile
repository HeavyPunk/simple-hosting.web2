FROM ubuntu:22.04

RUN apt update -y
RUN apt install openjdk-17-jre -y

COPY build/ /web-api/

EXPOSE 9000

CMD /web-api/bin/simple-hosting-web2 -J--add-opens -Jjava.base/java.lang=ALL-UNNAMED
