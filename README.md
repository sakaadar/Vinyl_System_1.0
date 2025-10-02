# Vinyl System 1.0
![Java](https://img.shields.io/badge/Java-17%2B-blue)
![Build](https://img.shields.io/badge/build-maven-success)
![Protocol](https://img.shields.io/badge/protocol-line--delimited%20JSON-informational)

Name-resolution Directory + TCP CatalogServer + CLI-klient.  
Protokol: **line-delimited JSON** (én JSON pr. linje). Fokus: enkel, robust, demo-venlig.

---

## Indholdsfortegnelse
## Arkitektur

```mermaid
sequenceDiagram
  autonumber
  participant S as CatalogServer
  participant D as Directory
  participant C as Client (CLI)

  %% 1) Directory startes først
Note over D: Directory running (TCP:5044, UDP:4555, TTL:3600s)
  S->>D: TCP REGISTER {"CMD":"REGISTER","NAME","IPv4","TTL"}
  D-->>S: {"STATUS":"000000","TTL"}
  loop hver TTL/2
    S->>D: TCP RENEW {"CMD":"RENEW","NAME","IPv4","TTL"}
    D-->>S: {"STATUS":"000000","TTL"}
  end

  %% 2) Klient kan nu resolve og forbinde
  C->>D: UDP LOOKUP {"NAME":"..."}
  D-->>C: {"STATUS":"000000","NAME","IPv4","TTL"}

  C->>S: TCP {"CMD":"LIST"/"SEARCH"/"GET"/"QUIT"}
  S-->>C: TCP svar (line-delimited JSON)
