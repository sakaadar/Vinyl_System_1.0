# Vinyl System 1.0
![Java](https://img.shields.io/badge/Java-17%2B-blue)
![Build](https://img.shields.io/badge/build-maven-success)
![Protocol](https://img.shields.io/badge/protocol-line--delimited%20JSON-informational)

Name-resolution Directory + TCP CatalogServer + CLI-klient.  
Protokol: **line-delimited JSON** (én JSON pr. linje). Fokus: enkel, robust, demo-venlig.

---

## Indholdsfortegnelse
- [Arkitektur](#arkitektur)
- [Statuskoder](#statuskoder)
- [Krav](#krav)
- [Build](#build)
- [Kørsel](#kørsel)
  - [Directory](#1-directory)
  - [Server (Catalog)](#2-server-catalog)
  - [Client (CLI)](#3-client-cli)
- [Eksempel-output](#eksempel-output)
- [Protokol](#protokol)
- [Projektstruktur](#projektstruktur)
- [Fejlfinding](#fejlfinding)
- [Videre arbejde](#videre-arbejde)
- [Licens](#licens)

---

## Arkitektur

```mermaid
sequenceDiagram
  autonumber
  participant C as Client (CLI)
  participant D as Directory
  participant S as CatalogServer

  C->>D: UDP LOOKUP {"NAME": "..."}
  D-->>C: {"STATUS":"000000","NAME","IPv4","TTL"}

  S->>D: TCP REGISTER {"CMD":"REGISTER","NAME","IPv4","TTL"}
  D-->>S: {"STATUS":"000000","TTL"}
  loop hver TTL/2
    S->>D: TCP RENEW {"CMD":"RENEW","NAME","IPv4","TTL"}
    D-->>S: {"STATUS":"000000","TTL"}
  end

  C->>S: TCP {"CMD":"LIST"/"SEARCH"/"GET"/"QUIT"}
  S-->>C: TCP svar (line-delimited JSON)
