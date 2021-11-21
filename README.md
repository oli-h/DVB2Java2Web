# DVB ➔ Java ➔ Web

## Motivation
Private small project with these challenges
- access Linux DVB-Device (for now: DVB-C) from Java via JNA
- receive and decode all those data-tables (PAT, PMT, NIT, SDT, EIT, etc...) - also with Java
- present decoded stuff in a small Web-UI (using good old AngularJS)

## Howto
- Clone
- Adapt some for-now hard-coded parameters to your needs (e.g. Adapter-Number, Network-ID, Initial tuning parameter)
- Maven-build - or just have IntelliJ or so
- Start Spring-Boot application `ch.oli.web.DvbApplication`
- Open Browser and go to http://localhost:8080/index.html - press the "Tune" button

## technical info
- Java-part mainly controls DVB-Devices "DVB-Frontend" and "DVB-Demux" via ioctl-commands
- Starts a receiver-thread for every relevant PID (PID 0 = PAT, PID 16 = NIT, etc...)
- Receive and decode all those DVB-tables
- Send the decoded values via JSON and WebSocket to the Web-UI
- Web-UI collects the single infos to a "whole view" and presents it