# 🔎 Spring WebFlux + Elasticsearch Learning

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-brightgreen)
![WebFlux](https://img.shields.io/badge/Spring-WebFlux-green)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-9.4.8-005571)

คลังเรียนรู้แบบ hands-on ที่รวมโปรเจกต์ Spring Boot (WebFlux) + Elasticsearch ทีละบท
แต่ละบทสอนหนึ่งหัวข้อหลัก พร้อมโค้ดที่รันได้จริง, การเชื่อมต่อ Elasticsearch จริง, tests,
ตัวอย่าง cURL และ README ภาษาไทย (คงศัพท์เทคนิค identifier และโค้ดเป็นภาษาอังกฤษ)

> **แต่ละบทเป็นโปรเจกต์ Maven/Spring Boot อิสระ** — มี `pom.xml`, `docker-compose.yml`, `README.md`
> และ `VERIFICATION.md` ของตัวเอง รันได้โดยไม่ต้องพึ่ง source code ของบทอื่น

## 🗺️ Learning Roadmap

สถานะ: ✅ Verified = ผ่านการ verify จริงพร้อมหลักฐานใน `VERIFICATION.md` · 🛠️ Implemented = มีโค้ดแล้วแต่ยัง verify ไม่ครบ · ⏳ Planned = ยังไม่สร้าง

| # | Chapter | Topic | Status |
|---|---|---|---|
| 01 | [`01-basic-connection`](01-basic-connection/) | เชื่อมต่อ local, สอง client, cluster info | ✅ Verified |
| 02 | [`02-api-key-authentication`](02-api-key-authentication/) | API key security + client setup | ✅ Verified |
| 03 | [`03-document-crud`](03-document-crud/) | Index / get / update / delete | ✅ Verified |
| 04 | [`04-search-queries`](04-search-queries/) | Term, match, bool queries | ✅ Verified |
| 05 | `05-elasticsearch-filtering` | Term/terms, range, exists, bool filter, query vs filter context | ⏳ Planned |
| 06 | `06-custom-analyzers` | Standard, Thai, ngram analyzers | ⏳ Planned |
| 07 | `07-sorting-pagination` | Sorting, pagination, search_after | ⏳ Planned |
| 08 | `08-fuzzy-search` | Fuzzy, prefix, partial search | ⏳ Planned |
| 09 | `09-index-management` | Index / mappings / settings / templates | ⏳ Planned |
| 10 | `10-bulk-operations` | Batch และ bulk indexing | ⏳ Planned |
| 11 | `11-reactive-error-handling` | Timeout, retry, error mapping | ⏳ Planned |
| 12 | `12-index-recreation` | Recreate, resync, safe lifecycle | ⏳ Planned |
| 13 | `13-integration-testing` | Testcontainers และ integration strategies | ⏳ Planned |
| 14 | `14-field-collapse` | Field collapse, grouped hits, inner_hits, collapse vs aggregations | ⏳ Planned |

> ลิงก์ชี้ไปยังโฟลเดอร์เฉพาะบทที่สร้างแล้วเท่านั้น บทที่ยัง ⏳ Planned จะถูก implement ทีละบทเมื่อได้รับการอนุมัติ

## 🚀 Getting Started

### Prerequisites
- Java 21
- Maven
- Docker + Docker Compose (สำหรับรัน Elasticsearch แบบ local)

### ขั้นตอน
1. **เลือกบท** — เข้าไปที่โฟลเดอร์ของบท (เช่น `04-search-queries/`) แล้วอ่าน `README.md` ของบทนั้นเป็นหลัก
2. **สตาร์ท Elasticsearch** — แต่ละบทมี `docker-compose.yml` และใช้ Compose project name แยกกัน:
   ```bash
   cd 04-search-queries
   docker compose -p ch04 up -d
   ```
   > บทที่เปิด security (02 เป็นต้นไป) ต้องตั้ง `ELASTIC_PASSWORD` และสร้าง API key — ดูขั้นตอนใน README ของบทนั้น
3. **รันแอป**
   ```bash
   mvn -DskipTests package
   java -jar target/*.jar
   ```
4. **รันเทสต์** (unit + integration ผ่าน Testcontainers; ต้องเปิด Docker)
   ```bash
   mvn clean verify
   ```

รายละเอียดครบของแต่ละบท (env vars, API key, cURL examples, cleanup) อยู่ใน README ของบทนั้น ๆ ตามลิงก์ในตาราง Roadmap

## 🧰 Tech Stack
Java 21 · Spring Boot 4.1.2 · Spring WebFlux · Spring Data Elasticsearch 6.1.2 · Elasticsearch Java Client 9.4.8 · Elasticsearch 9.4.8 · Testcontainers 2.0.5
