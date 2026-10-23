# Chapter 03 — Document CRUD (จัดการเอกสารใน Elasticsearch)

> โปรเจกต์ Spring Boot WebFlux อิสระ สาธิต **CRUD เอกสาร** (`Product`) บน index เดียว
> (`learning-products`) ด้วยสอง client (Spring Data reactive + official Java API Client)
> บน Elasticsearch ที่เปิด security และ auth ด้วย API key (ต่อยอดจาก Chapter 02)

---

## 1. Learning Objectives
เมื่อจบบทนี้คุณจะ:
- ทำ Create / Get / Update / Delete เอกสารด้วยทั้งสอง client บน index และ schema เดียวกัน
- เข้าใจความต่างของ `_id` กับ `_source` และ HTTP semantics ของ CRUD (201/200/204/404/409/400)
- เข้าใจว่า Elasticsearch เป็น search engine + document store ไม่ใช่ relational database
- เข้าใจ near-real-time search กับ real-time GET และบทบาทของ `refresh`
- จัดการ error ครบถ้วน (404/409/400/401/403/503/504/502/500) โดยไม่ leak ข้อมูล

## 2. Prerequisites
- Java 21, Maven, Docker + Docker Compose
- `jq`, `curl`

## 3. Elasticsearch Document & Index concepts
- **Index** = ชุดของเอกสาร (เทียบคร่าว ๆ กับ "ตาราง" แต่จริง ๆ คือ inverted index สำหรับ search)
- **Document** = JSON หนึ่งก้อน มี `_id` และ `_source`
- Elasticsearch เป็น **document store + search engine**: schema ยืดหยุ่น, join แบบ RDBMS แทบไม่มี, เน้น denormalize
- บทนี้ใช้ index เดียว `learning-products` และ schema:
  ```
  id (String, _id) · name (text) · description (text) · price (double) · quantity (integer)
  ```

## 4. `_id` vs `_source`
- **`_id`** = กุญแจของเอกสาร ใช้ทำ **real-time GET** (`GET /index/_doc/{id}`) ได้ทันทีหลังเขียน
- **`_source`** = ตัว JSON จริงที่เก็บและคืนกลับ (fields `name`, `price`, ...)
- ในบทนี้เราใช้ **explicit id** (มาจาก URL path) เพื่อให้ตัวอย่าง/เทสต์ reproducible ไม่พึ่ง id ที่ ES สุ่มให้

## 5. CRUD operation overview
| Operation | HTTP | semantics |
|---|---|---|
| Create | `POST .../products/{id}` | สร้างใหม่ด้วย `op_type=create` → id ซ้ำ = 409 (ไม่ upsert) |
| Get | `GET .../products/{id}` | real-time GET by id → ไม่พบ = 404 |
| Update | `PUT .../products/{id}` | **full replacement** ของเอกสารที่มีอยู่ → ไม่มี = 404 (ไม่ upsert) |
| Delete | `DELETE .../products/{id}` | ลบ by id → ไม่พบ = 404, สำเร็จ = 204 |

> **PUT = แทนที่ทั้งเอกสาร** ไม่ใช่ partial update และ **ไม่ silently upsert** (ต้องมีอยู่ก่อน)

## 6. Start Elasticsearch using Docker Compose
```bash
cd 03-document-crud
export ELASTIC_PASSWORD='local-dev-password'
docker compose -p ch03 up -d

# readiness (ควรได้ 200 เมื่อ auth; 401 ถ้าไม่ใส่ credential = security เปิดจริง)
curl -s -o /dev/null -w "%{http_code}\n" -u "elastic:${ELASTIC_PASSWORD}" http://127.0.0.1:9200/_cluster/health
```

## 7. Create API Key with appropriate permissions
CRUD ต้องมีสิทธิ์ระดับ index บน `learning-products` (ไม่ใช่แค่ `cluster:monitor` เหมือน Chapter 02)
```bash
export ELASTIC_PASSWORD='local-dev-password'
curl -s -u "elastic:${ELASTIC_PASSWORD}" -H 'Content-Type: application/json' \
  -XPOST http://127.0.0.1:9200/_security/api_key -d '{
    "name": "ch03-app",
    "role_descriptors": {
      "crud": {
        "cluster": ["monitor"],
        "indices": [
          { "names": ["learning-products"],
            "privileges": ["create_index","read","write","delete","view_index_metadata"] }
        ]
      }
    }
  }' | jq .
```
เก็บค่า field `encoded` ไปใส่ env `ELASTICSEARCH_API_KEY`

## 8. Index initialization and mappings
แอปสร้าง index `learning-products` พร้อม mapping ให้อัตโนมัติตอน startup **ถ้ายังไม่มี** (idempotent)
ผ่าน `IndexInitializer` (ใช้ `ReactiveIndexOperations.createWithMapping()` จาก annotation ของ `Product`)
- ไม่มี index template / alias / rollover (เก็บไว้บทหลัง)
- mapping: `name`/`description` = text, `price` = double, `quantity` = integer

## 9. Configure environment variables
```bash
cp .env.example .env    # อย่า commit .env
```
- `ELASTICSEARCH_API_KEY` — ค่า `encoded` จากขั้นตอนที่ 7
- `ELASTICSEARCH_URIS` — ดีฟอลต์ `http://localhost:9200`
- `SERVER_PORT` — ดีฟอลต์ `8082`

## 10. Run Spring Boot
```bash
export ELASTICSEARCH_API_KEY='<encoded key>'
mvn -DskipTests package
java -jar target/document-crud.jar
```

## 11. REST API examples (cURL)
```bash
base=http://127.0.0.1:8082
body='{"name":"Keyboard","description":"mechanical","price":129.90,"quantity":10}'

# CREATE (201) — spring-data
curl -s -w ' %{http_code}\n' -XPOST $base/api/spring-data/products/p100 -H 'Content-Type: application/json' -d "$body"

# GET (200) — java-client (เห็นข้อมูลที่ spring-data เขียน เพราะ index เดียวกัน)
curl -s $base/api/java-client/products/p100 | jq .

# UPDATE (200, full replace) — java-client
curl -s -XPUT $base/api/java-client/products/p100 -H 'Content-Type: application/json' \
  -d '{"name":"Keyboard Pro","description":"hot-swappable","price":159.00,"quantity":7}' | jq .

# GET updated (200) — spring-data
curl -s $base/api/spring-data/products/p100 | jq .

# DELETE (204) — spring-data
curl -s -o /dev/null -w '%{http_code}\n' -XDELETE $base/api/spring-data/products/p100
```
ทั้งสอง client ใช้ path เดียวกันต่างแค่ prefix `spring-data` / `java-client` และ behavior เหมือนกันทุกอย่าง

## 12. Error scenarios
| สถานการณ์ | status |
|---|---|
| Create สำเร็จ | 201 |
| Get / Update สำเร็จ | 200 |
| Delete สำเร็จ | 204 |
| ไม่พบเอกสาร | 404 (`Product not found`) |
| Create id ซ้ำ | 409 (`Product already exists`) |
| request ไม่ถูกต้อง (validation) | 400 (`Invalid request`) |
| API key ผิด/หาย/ถูกเพิกถอน | 401 |
| สิทธิ์ไม่พอ | 403 |
| ES ติดต่อไม่ได้ / timeout / error อื่น | 503 / 504 / 502 |

ตัวอย่าง:
```bash
# duplicate (409)
curl -s -w ' %{http_code}\n' -XPOST $base/api/java-client/products/p100 -H 'Content-Type: application/json' -d "$body"
# invalid (400)
curl -s -w ' %{http_code}\n' -XPOST $base/api/spring-data/products/bad -H 'Content-Type: application/json' -d '{"name":"","price":-1,"quantity":-5}'
# not found (404)
curl -s -w ' %{http_code}\n' $base/api/java-client/products/ghost
```
error body เป็น `ProblemDetail` แบบ generic ไม่มี key / host / stack trace / ES message ดิบ

## 13. Near-real-time visibility & refresh
- Elasticsearch **search** เป็น near-real-time: เอกสารที่เพิ่ง index จะค้นเจอหลัง refresh (ดีฟอลต์ ~1s)
- **real-time GET by `_id`** เห็นผลทันทีโดยไม่ต้องรอ refresh
- บทนี้ตั้ง `refresh` บน write (`RefreshPolicy.IMMEDIATE` สำหรับ spring-data, `refresh=true` สำหรับ java-client) เพื่อให้ตัวอย่าง/เทสต์เห็นผลทันที — **ใน production ไม่ควรใช้ immediate refresh ทุก write** เพราะกระทบ performance

## 14. Troubleshooting
- **401 ทั้งที่ key ถูก**: ใช้ค่า `encoded` ไม่ใช่ `api_key` ดิบ และตรวจว่า env ถูกส่งเข้าแอป
- **403**: key สิทธิ์ไม่พอ — ตรวจ `role_descriptors` ว่ามี index privileges ครบ (`create_index`,`read`,`write`,`delete`)
- **ค้นไม่เจอแต่ GET เจอ**: เป็นเรื่อง refresh/near-real-time ปกติ (ดูหัวข้อ 13)
- **สร้าง key ได้ 401**: ES ยัง init security ไม่เสร็จ รอ health 200 ก่อน
- **พอร์ตชน**: แก้ `SERVER_PORT` หรือ mapping ใน `docker-compose.yml`

## 15. Cleanup
```bash
# หยุดแอป Ctrl+C
export ELASTIC_PASSWORD='local-dev-password'
docker compose -p ch03 down     # ลบ container + network (ไม่มี persistent volume)
```

## 16. Security considerations
- ปิด TLS บน HTTP layer **เฉพาะ local** — production ต้องเปิด HTTPS เสมอ ไม่งั้น API key รั่วทาง network
- bind `127.0.0.1` เท่านั้น
- ใช้ **least-privilege**: key จำกัดสิทธิ์เฉพาะ index `learning-products`
- ความลับมาจาก env เท่านั้น, `.env` อยู่ใน `.gitignore`, ไม่ commit secret, ไม่ log key/Authorization header
- เพิกถอน key ที่รั่ว/เลิกใช้ด้วย `DELETE /_security/api_key`

---

## เวอร์ชัน
Java 21 · Spring Boot 4.1.2 · Spring Data Elasticsearch 6.1.2 · Elasticsearch Java client 9.4.8 · Elasticsearch server 9.4.8 · Testcontainers 2.0.5

## โครงสร้างโปรเจกต์
```
03-document-crud/
├── docker-compose.yml        # ES 9.4.8, security on, http TLS off (local), bind 127.0.0.1
├── .env.example
├── pom.xml
└── src/
    ├── main/java/com/example/learning/documentcrud/
    │   ├── DocumentCrudApplication.java
    │   ├── domain/Product.java                     # @Document(learning-products) + field mappings
    │   ├── dto/{ProductRequest,ProductResponse}.java
    │   ├── config/IndexInitializer.java            # สร้าง index+mapping ตอน startup (idempotent)
    │   ├── controller/{SpringData,JavaClient}ProductController.java
    │   ├── service/{SpringData,JavaClient}ProductService.java
    │   └── exception/{GlobalExceptionHandler,ProductNotFoundException,DuplicateProductException}.java
    ├── main/resources/application.yml
    └── test/java/com/example/learning/documentcrud/
        ├── controller/… , service/…  (unit)
        └── integration/ProductCrudIT.java          # Testcontainers: security + API key + CRUD จริง
```

## Testing
```bash
mvn clean verify
```
- unit: controller slice (ทั้งสอง client, 201/200/204/404/409/400), service (semantics ไม่ upsert, mapping, blocking isolation)
- integration (Testcontainers, security เปิดจริง, API key สิทธิ์ครบ): flow ข้าม client (create spring-data → get java-client → update → get → delete → gone) + duplicate 409 + missing 404
