# Chapter 02 — API Key Authentication (เชื่อมต่อ Elasticsearch อย่างปลอดภัยด้วย API Key)

> โปรเจกต์ Spring Boot WebFlux อิสระ สาธิตการเชื่อมต่อ Elasticsearch ที่ **เปิด security** ด้วย
> **API Key** โดยใช้สอง client (Spring Data reactive + official Java API Client) ที่ auth ด้วย key เดียวกัน

---

## 1. Learning Objectives
เมื่อจบบทนี้คุณจะ:
- เปิด Elasticsearch security และเข้าใจความต่างระหว่าง **Basic Authentication** กับ **API Key Authentication**
- เข้าใจว่า Elasticsearch API Key ทำงานอย่างไร (สร้าง / ใช้ / invalidate / least-privilege)
- ตั้งค่า Spring Boot ให้ทั้งสอง client auth ด้วย API key ผ่าน property เดียว โดยไม่ hardcode / ไม่ log key
- จัดการและแยกแยะ **authentication failure (401)** ออกจาก **authorization failure (403)** พร้อม error ที่ไม่ leak ข้อมูล

## 2. Prerequisites
- Java 21
- Maven
- Docker + Docker Compose
- `jq` และ `curl` (สำหรับตัวอย่างใน README)

## 3. How API Key Authentication works

### Basic Auth vs API Key
- **Basic Authentication**: ส่ง `username:password` (เช่น `elastic`) ทุก request — เป็น credential ของ "ตัวตน" ที่มักมีสิทธิ์กว้าง เหมาะกับงาน admin ไม่เหมาะฝังในแอป
- **API Key Authentication**: สร้าง key เฉพาะกิจ ผูก **สิทธิ์แบบจำกัด (least-privilege)** ได้, เพิกถอน (invalidate) ได้ทันทีโดยไม่กระทบรหัสผ่านผู้ใช้, หมดอายุได้ — เหมาะกับ service-to-service

### API Key ทำงานอย่างไร
1. เรียก `POST /_security/api_key` (โดย principal ที่มีสิทธิ์ เช่น `elastic`) พร้อมระบุ `role_descriptors` เพื่อจำกัดสิทธิ์
2. Elasticsearch คืน `id`, `api_key` และ `encoded` (ค่า `base64(id:api_key)`)
3. ทุก request แนบ header `Authorization: ApiKey <encoded>`
4. เพิกถอนได้ด้วย `DELETE /_security/api_key` → key ใช้ไม่ได้อีกทันที (ได้ 401)

> API Key ต้องเปิด security ก่อนเสมอ ในบทนี้เราเปิด `xpack.security.enabled=true`

## 4. Start Elasticsearch with Docker Compose
```bash
cd 02-api-key-authentication

# ตั้ง password ของ elastic superuser (local เท่านั้น)
export ELASTIC_PASSWORD='local-dev-password'

docker compose -p ch02 up -d

# รอ readiness (ควรได้ 200 เมื่อ auth ถูกต้อง)
curl -s -o /dev/null -w "%{http_code}\n" -u "elastic:${ELASTIC_PASSWORD}" http://127.0.0.1:9200/_cluster/health
# ทดสอบว่า security เปิดจริง: ไม่ใส่ credential ต้องได้ 401
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:9200/_cluster/health
```

## 5. Bootstrap credentials และสร้าง API Key
```bash
export ELASTIC_PASSWORD='local-dev-password'

# สร้าง API key แบบ least-privilege (บทนี้ต้องการแค่ cluster:monitor สำหรับ health + info)
curl -s -u "elastic:${ELASTIC_PASSWORD}" -H 'Content-Type: application/json' \
  -XPOST http://127.0.0.1:9200/_security/api_key -d '{
    "name": "chapter02-app-key",
    "role_descriptors": { "ch02_reader": { "cluster": ["monitor"], "indices": [] } }
  }' | jq .
```
ผลลัพธ์จะมี field `encoded` — นี่คือค่าที่แอปต้องใช้
```json
{ "id": "...", "name": "chapter02-app-key", "api_key": "...", "encoded": "WVJKYWFL...Ynl5UQ==" }
```

> ℹ️ `cluster:["monitor"]` คือสิทธิ์ขั้นต่ำสำหรับ cluster health/info ของบทนี้ ไม่ให้สิทธิ์ index/write ใด ๆ

## 6. Configure environment variables
```bash
cp .env.example .env   # แล้วแก้ค่าใน .env (อย่า commit .env)
```
ตัวแปรที่แอปใช้:
- `ELASTICSEARCH_API_KEY` — ค่า `encoded` จากขั้นตอนที่ 5 (ส่งเข้าแอปผ่าน env)
- `ELASTICSEARCH_URIS` — ดีฟอลต์ `http://localhost:9200`
- `SERVER_PORT` — ดีฟอลต์ `8081`

> ความลับทั้งหมดมาจาก environment ไม่มีการ hardcode ในโค้ด/ไฟล์ที่ commit และแอปไม่ log key หรือ Authorization header

## 7. Start the Spring Boot application
```bash
export ELASTICSEARCH_API_KEY='<ค่า encoded จากขั้นตอนที่ 5>'

mvn -DskipTests package
java -jar target/api-key-authentication.jar
#   หรือ: ELASTICSEARCH_API_KEY=... mvn spring-boot:run
```

## 8. API Examples (valid API key)
ทุก endpoint คืน 200 พร้อม JSON ที่ map เป็น DTO เสถียร
```bash
base=http://127.0.0.1:8081

curl --fail-with-body -s $base/api/spring-data/elasticsearch/health | jq .
# → {"client":"spring-data","clusterName":"docker-cluster","status":"green","numberOfNodes":1,"reachable":true}

curl --fail-with-body -s $base/api/spring-data/elasticsearch/cluster-info | jq .
# → {"client":"spring-data","clusterName":"docker-cluster","clusterUuid":"<uuid>","versionNumber":"9.4.8","tagline":"You Know, for Search"}

curl --fail-with-body -s $base/api/java-client/elasticsearch/health | jq .
curl --fail-with-body -s $base/api/java-client/elasticsearch/cluster-info | jq .
# → client = "java-client", ค่าอื่น ๆ เหมือนกัน (ติดต่อ cluster เดียวกัน)
```

## 9. Authentication / Authorization failures
แอปจำแนก error ตามสาเหตุและไม่ leak key/host/response ดิบ

| กรณี | พฤติกรรม | HTTP status | title |
|---|---|---|---|
| Valid API key | ทำงานปกติ | 200 | — |
| Missing / Invalid / Revoked key | ES ตอบ 401 | **401** | `Elasticsearch authentication failed` |
| Insufficient permission | ES ตอบ 403 | **403** | `Elasticsearch authorization failed` |
| ES ติดต่อไม่ได้ | — | 503 | `Elasticsearch unavailable` |
| ES timeout | — | 504 | `Elasticsearch timeout` |
| ES error อื่น | — | 502 | `Elasticsearch returned an error` |
| error อื่นในแอป | — | 500 | `Internal server error` |

ทดสอบ invalid key (รันแอปด้วย key มั่ว ๆ):
```bash
ELASTICSEARCH_API_KEY='bm90LWEtcmVhbC1pZDpub3QtYS1yZWFsLXNlY3JldA==' java -jar target/api-key-authentication.jar &
curl -s -w ' STATUS=%{http_code}\n' http://127.0.0.1:8081/api/java-client/elasticsearch/health
# → {"title":"Elasticsearch authentication failed","status":401,...}  STATUS=401
```

> **authentication (401)** = ยืนยันตัวตนไม่ผ่าน (key หาย/ผิด/ถูกเพิกถอน)
> **authorization (403)** = ตัวตนถูกต้องแต่สิทธิ์ไม่พอ (เช่น key ไม่มี `cluster:monitor`)

## 10. Revoke an API Key
```bash
export ELASTIC_PASSWORD='local-dev-password'
# ใช้ id ที่ได้ตอนสร้าง key
curl -s -u "elastic:${ELASTIC_PASSWORD}" -H 'Content-Type: application/json' \
  -XDELETE http://127.0.0.1:9200/_security/api_key -d '{"ids":["<KEY_ID>"]}' | jq .
# → {"invalidated_api_keys":["<KEY_ID>"], ...}
```
หลังเพิกถอน request ที่ใช้ key เดิมจะได้ 401 ทันที

## 11. Troubleshooting
- **สร้าง key ได้ 401**: ES ยังไม่พร้อม (security ใช้เวลา init) — รอให้ `-u elastic:...` /_cluster/health ตอบ 200 ก่อน
- **แอปได้ 401 ทั้งที่ key ถูก**: ตรวจว่าใช้ค่า `encoded` (ไม่ใช่ `api_key` ดิบ) และ env ถูกส่งเข้าแอปจริง
- **แอปได้ 403**: key มีสิทธิ์ไม่พอ — เพิ่ม `cluster:["monitor"]` ใน `role_descriptors`
- **พอร์ตชน**: แก้ `SERVER_PORT` หรือ mapping ใน `docker-compose.yml`
- **`ELASTIC_PASSWORD` ไม่ถูกตั้ง**: compose จะ fail พร้อมข้อความเตือน ให้ `export ELASTIC_PASSWORD=...`

## 12. Cleanup
```bash
# หยุดแอปด้วย Ctrl+C
export ELASTIC_PASSWORD='local-dev-password'
docker compose -p ch02 down     # ลบ container + network (ไม่มี persistent volume)
```

## 13. Security considerations
- บทนี้ปิด TLS บน HTTP layer (`xpack.security.http.ssl.enabled=false`) **เพื่อความง่ายใน local เท่านั้น** — ใน production **ต้องเปิด HTTPS** เสมอ ไม่งั้น API key จะถูกส่งแบบ clear text
- bind `127.0.0.1` เท่านั้น ห้าม expose node ออกนอกเครื่อง
- ใช้ **least-privilege** กับ API key เสมอ (บทนี้ให้แค่ `cluster:monitor`)
- ความลับมาจาก environment เท่านั้น, `.env` อยู่ใน `.gitignore`, ไม่ commit secret, ไม่ log key/Authorization header
- เพิกถอน key ที่ไม่ใช้/รั่วไหลทันทีด้วย `_security/api_key` DELETE และพิจารณาตั้ง `expiration`

---

## เวอร์ชันที่ใช้
Java 21 · Spring Boot 4.1.2 · Spring Data Elasticsearch 6.1.2 · Elasticsearch Java client 9.4.8 · Elasticsearch server 9.4.8 · Testcontainers 2.0.5

## โครงสร้างโปรเจกต์
```text
02-api-key-authentication/
├── .env.example
├── .gitignore
├── docker-compose.yml
├── pom.xml
├── README.md
├── VERIFICATION.md
└── src/
    ├── main/
    │   ├── java/com/example/learning/apikeyauth/
    │   │   ├── controller/
    │   │   │   ├── JavaClientConnectionController.java
    │   │   │   └── SpringDataConnectionController.java
    │   │   ├── dto/
    │   │   │   ├── ClusterHealthResponse.java
    │   │   │   └── ClusterInfoResponse.java
    │   │   ├── exception/
    │   │   │   └── GlobalExceptionHandler.java
    │   │   ├── service/
    │   │   │   ├── JavaClientConnectionService.java
    │   │   │   └── SpringDataConnectionService.java
    │   │   └── ApiKeyAuthenticationApplication.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/example/learning/apikeyauth/
            ├── controller/
            │   ├── JavaClientConnectionControllerTest.java
            │   └── SpringDataConnectionControllerTest.java
            ├── exception/
            │   ├── ErrorThrowingTestController.java
            │   └── GlobalExceptionHandlerTest.java
            ├── integration/
            │   └── ApiKeyAuthenticationIT.java
            └── service/
                ├── JavaClientConnectionServiceTest.java
                └── SpringDataConnectionServiceTest.java
```

## Testing
```bash
mvn clean verify
```
- unit: controller slice, service (StepVerifier + mapping + blocking isolation + lazy), exception handler (401/403/502/503/504/500 + no-leak)
- integration (Testcontainers, security เปิดจริง): สร้าง API key จริง แล้วยืนยันทั้งสอง client auth ผ่าน + เคส invalid key ได้ 401
