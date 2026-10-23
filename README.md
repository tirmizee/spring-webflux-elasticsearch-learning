# spring-webflux-elasticsearch-learning

คลังเรียนรู้แบบ hands-on ที่รวมโปรเจกต์ Spring Boot (WebFlux) + Elasticsearch แบบ **independent** ทีละบท แต่ละบทสอนหนึ่งหัวข้อหลัก พร้อมโค้ดที่รันได้จริง, การเชื่อมต่อ Elasticsearch จริง, tests, ตัวอย่าง cURL และ README ภาษาไทย (คงศัพท์เทคนิค identifier และโค้ดเป็นภาษาอังกฤษ)

> แต่ละบทเป็นโปรเจกต์ Maven/Spring Boot อิสระ มี `pom.xml`, `docker-compose.yml`, `README.md` และ `VERIFICATION.md` ของตัวเอง ไม่พึ่งพา source code ของบทอื่น

## Prerequisites
- Java 21
- Maven
- Docker + Docker Compose (สำหรับรัน Elasticsearch แบบ local)

## Roadmap (13 chapters)

สถานะ: `planned` = ยังไม่สร้าง, `implemented` = มีโค้ดแล้ว, `verified` = ผ่านการ verify จริงตาม `VERIFICATION.md` แล้ว
ลิงก์จะชี้ไปยังโฟลเดอร์เมื่อบทนั้นถูกสร้างขึ้นจริงเท่านั้น

| # | Directory | Topic | Status |
|---|---|---|---|
| 01 | [`01-basic-connection`](01-basic-connection/) | Local unauthenticated connectivity, two clients, cluster information | verified |
| 02 | [`02-api-key-authentication`](02-api-key-authentication/) | API key security and client setup | verified |
| 03 | `03-document-crud` | Index/get/update/delete | planned |
| 04 | `04-search-queries` | Term, match, bool queries | planned |
| 05 | `05-elasticsearch-filtering` | Term/terms, range, exists, bool filter/must_not, AND/OR, query vs filter context, combining filters with full-text search | planned |
| 06 | `06-custom-analyzers` | Standard, Thai, ngram analysis | planned |
| 07 | `07-sorting-pagination` | Sorting, pagination, search_after | planned |
| 08 | `08-fuzzy-search` | Fuzzy, prefix and partial search | planned |
| 09 | `09-index-management` | Index/mappings/settings/templates | planned |
| 10 | `10-bulk-operations` | Batch and bulk indexing | planned |
| 11 | `11-reactive-error-handling` | Timeout, retry, error mapping | planned |
| 12 | `12-index-recreation` | Recreate, resync, safe lifecycle | planned |
| 13 | `13-integration-testing` | Testcontainers and integration strategies | planned |

## Getting Started
Chapter 01 พร้อมใช้งานและผ่านการ verify แล้ว เริ่มต้นที่ [`01-basic-connection/README.md`](01-basic-connection/README.md)
(prerequisites, `docker compose -p ch01 up -d`, การรันแอป, cURL examples และการ stop/cleanup)

Chapter 02 (API key authentication) ก็ implement และ verify แล้ว ดู [`02-api-key-authentication/README.md`](02-api-key-authentication/README.md)

บทที่เหลือ (03–13) ยังเป็น `planned` จะถูก implement ทีละบทเมื่อได้รับการอนุมัติเท่านั้น
