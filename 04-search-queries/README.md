# Chapter 04 — Search Queries (term / match / bool)

> โปรเจกต์ Spring Boot WebFlux อิสระ สาธิต Elasticsearch **Query DSL** พื้นฐาน: `term`, `match`, และ
> `bool` (`must`/`should`/`must_not`/`minimum_should_match`) พร้อม relevance `_score` บน product
> catalog เล็ก ๆ ด้วยสอง client (Spring Data reactive + official Java API Client) ที่รัน query เดียวกัน

---

## 1. Learning Objectives
เมื่อจบบทนี้คุณจะ:
- เข้าใจ Query DSL และความต่างระหว่าง **exact-match** (`term`) กับ **full-text** (`match`)
- เข้าใจว่า field mapping (`keyword` vs `text`) มีผลต่อผลลัพธ์อย่างไร
- ประกอบ `bool` query ด้วย `must` / `should` / `must_not` และใช้ `minimum_should_match`
- เข้าใจ relevance scoring (`_score`) และพฤติกรรมที่ `should` เป็น optional เมื่อมี `must`
- เปรียบเทียบการทำงานของ Spring Data Elasticsearch กับ official Java API Client

## 2. Query DSL fundamentals
Elasticsearch query คือ JSON tree ของ "query clauses" แต่ละ clause มีชนิด (`term`, `match`, `bool`, ...)
บทนี้ไม่ได้ expose raw DSL ออกเป็น public API แต่ห่อด้วย DTO เล็ก ๆ (`term`/`match`/`bool`) เพื่อการเรียนรู้
> ทั้งสอง client สร้าง query จาก builder ของ `co.elastic.clients` ตัวเดียวกัน (ดู `query/ProductQueries`) จึงรัน query เดียวกันเป๊ะ ต่างกันแค่วิธี execute

## 3. Mappings และทำไมถึงสำคัญ
| Field | Type | ใช้ทำ |
|---|---|---|
| id | `_id` | document id |
| name | `text` | full-text (`match`) |
| category | `keyword` | exact (`term`) |
| brand | `keyword` | exact (`term`) |
| description | `text` | full-text (`match`) |
| price | `double` | ข้อมูลประกอบ |
| quantity | `integer` | ข้อมูลประกอบ |

- `text` ถูก **analyze** (ตัดคำ, lowercase) ตอน index → เหมาะกับ `match`
- `keyword` ไม่ถูก analyze เก็บค่าทั้งก้อน → เหมาะกับ `term` (exact, case-sensitive)

## 4. Term vs Match
- **`term`** = หา "indexed term" แบบ exact มักใช้กับ `keyword` เช่น `category = laptop`
  - ⚠️ `term` บน field ชนิด `text` จะ match กับ token ที่ถูก analyze แล้ว (เช่น lowercased) ซึ่งมักไม่ใช่สิ่งที่ตั้งใจ — จึงควร exact-match บน `keyword`
  - exact จริง: `category=Laptop` (ผิด case) หรือ `brand=apple` จะ **ไม่เจอ**
- **`match`** = full-text ข้อความค้นถูก analyze เหมือน field ตอน index แล้ว match ที่ระดับ token พร้อมคิด `_score`

| | term | match | bool |
|---|---|---|---|
| ชนิดการ match | exact indexed term | full-text (analyzed) | ประกอบหลาย clause |
| field ที่เหมาะ | `keyword` | `text` | ผสมได้ |
| วิเคราะห์ข้อความค้น | ไม่ | ใช่ | ตาม sub-clause |
| มีผลต่อ `_score` | น้อย/คงที่ | ใช่ | รวมจาก must/should |
| ตัวอย่าง | `category=laptop` | `name ~ "macbook"` | must+should+must_not |

## 5. Bool must / should / must_not
- **`must`** — ต้อง match ทุก clause (AND) และมีผลต่อ score
- **`should`** — clause ทางเลือก; **ถ้า bool มี `must` อยู่แล้ว** `should` จะทำหน้าที่แค่ boost score (optional) เว้นแต่กำหนด `minimum_should_match`
- **`must_not`** — เอกสารที่ match จะถูก **ตัดออก**

## 6. Minimum should match
`minimum_should_match` กำหนดว่าต้อง match `should` อย่างน้อยกี่ clause
- ตัวอย่าง: must `category=laptop`, should `name~pro` หรือ `name~air`, `minimumShouldMatch=1`
  → เหลือเฉพาะ laptop ที่ชื่อ match "pro" หรือ "air" คือ p01 (Pro), p02 (Air)

## 7. Relevance scoring (`_score`)
`match`/`should` ทำให้แต่ละ hit มี `_score` (ยิ่งตรงมากยิ่งสูง) ผลลัพธ์เรียงจาก score มากไปน้อย
> บทนี้ไม่ assert ค่า `_score` แบบ exact (ขึ้นกับ Lucene scoring) แต่ตรวจ membership/ordering แทน

## 8. ความต่างของสอง client
| | Spring Data (`ReactiveElasticsearchOperations`) | Java API Client (`ElasticsearchClient`) |
|---|---|---|
| โมเดล | reactive (non-blocking) | synchronous (blocking) |
| การ execute | `search(...)` คืน `Flux<SearchHit>` | `search(...)` คืน `SearchResponse` ต้อง offload |
| non-blocking | ใช่ โดยธรรมชาติ | ต้อง `Mono.fromCallable(...).subscribeOn(boundedElastic())` |
| query | `NativeQuery` ห่อ `co.elastic` Query | ใช้ `co.elastic` Query ตรง ๆ |
ทั้งคู่ค้น index เดียวกัน (`search-products`) และให้ผล membership เดียวกัน

## 9. Project Structure
```text
04-search-queries/
├── .env.example
├── .gitignore
├── docker-compose.yml
├── pom.xml
├── README.md
├── VERIFICATION.md
└── src/
    ├── main/
    │   ├── java/com/example/learning/searchqueries/
    │   │   ├── config/
    │   │   │   └── DataInitializer.java
    │   │   ├── controller/
    │   │   │   ├── JavaClientSearchController.java
    │   │   │   └── SpringDataSearchController.java
    │   │   ├── data/
    │   │   │   └── SampleProducts.java
    │   │   ├── domain/
    │   │   │   └── Product.java
    │   │   ├── dto/
    │   │   │   ├── BoolSearchRequest.java
    │   │   │   ├── ProductHit.java
    │   │   │   ├── QueryClause.java
    │   │   │   └── SearchResponse.java
    │   │   ├── exception/
    │   │   │   └── GlobalExceptionHandler.java
    │   │   ├── query/
    │   │   │   └── ProductQueries.java
    │   │   ├── service/
    │   │   │   ├── JavaClientSearchService.java
    │   │   │   └── SpringDataSearchService.java
    │   │   └── SearchQueriesApplication.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/com/example/learning/searchqueries/
            ├── controller/
            │   ├── JavaClientSearchControllerTest.java
            │   └── SpringDataSearchControllerTest.java
            ├── integration/
            │   └── SearchQueriesIT.java
            ├── query/
            │   └── ProductQueriesTest.java
            └── service/
                └── JavaClientSearchServiceTest.java
```

## 10. Docker และ API key setup
```bash
cd 04-search-queries
export ELASTIC_PASSWORD='local-dev-password'
docker compose -p ch04 up -d

# readiness
curl -s -o /dev/null -w "%{http_code}\n" -u "elastic:${ELASTIC_PASSWORD}" http://127.0.0.1:9200/_cluster/health

# สร้าง API key แบบ least-privilege (ต้องมีสิทธิ์บน index search-products)
curl -s -u "elastic:${ELASTIC_PASSWORD}" -H 'Content-Type: application/json' \
  -XPOST http://127.0.0.1:9200/_security/api_key -d '{
    "name": "ch04-app",
    "role_descriptors": {
      "search": {
        "cluster": ["monitor"],
        "indices": [
          { "names": ["search-products"],
            "privileges": ["create_index","read","write","view_index_metadata"] }
        ]
      }
    }
  }' | jq .
```
นำค่า `encoded` ไปใส่ env `ELASTICSEARCH_API_KEY`

## 11. Sample data initialization
- แอปสร้าง index `search-products` + mapping และ **seed ตัวอย่าง 10 รายการ** ตอน startup **เฉพาะเมื่อ index ยังไม่มี** (non-destructive)
- ถ้า index มีอยู่แล้ว จะไม่ลบ/ไม่ seed ซ้ำ/ไม่ทับข้อมูล
- **reset dataset**: ลบ index แล้ว restart แอป
  ```bash
  curl -s -u "elastic:${ELASTIC_PASSWORD}" -XDELETE http://127.0.0.1:9200/search-products
  # inspect
  curl -s -u "elastic:${ELASTIC_PASSWORD}" "http://127.0.0.1:9200/search-products/_search?size=20" | jq '.hits.total'
  ```

## 12. Run Spring Boot
```bash
export ELASTICSEARCH_API_KEY='<encoded key>'
mvn -DskipTests package
java -jar target/search-queries.jar
```

## 13. cURL examples และผลลัพธ์ที่คาดหวัง
```bash
base=http://127.0.0.1:8083

# TERM — exact keyword (category=laptop) -> p01,p02,p03,p04,p10
curl -s "$base/api/spring-data/products/search/term?field=category&value=laptop" | jq '{total, ids:[.hits[].id]}'

# TERM exact-miss (ผิด case) -> 200 + hits ว่าง (ไม่ใช่ 404)
curl -s "$base/api/spring-data/products/search/term?field=category&value=Laptop" | jq '{total}'

# MATCH — full-text (name ~ macbook) -> p01,p02
curl -s "$base/api/java-client/products/search/match?field=name&text=macbook" | jq '{ids:[.hits[].id]}'

# BOOL must (category=laptop AND name~gaming) -> p10
curl -s -XPOST "$base/api/spring-data/products/search/bool" -H 'Content-Type: application/json' \
  -d '{"must":[{"type":"term","field":"category","value":"laptop"},{"type":"match","field":"name","value":"gaming"}]}' | jq '{ids:[.hits[].id]}'

# BOOL must_not (laptop, ตัด brand=Apple) -> p03,p04,p10
curl -s -XPOST "$base/api/java-client/products/search/bool" -H 'Content-Type: application/json' \
  -d '{"must":[{"type":"term","field":"category","value":"laptop"}],"mustNot":[{"type":"term","field":"brand","value":"Apple"}]}' | jq '{ids:[.hits[].id]}'

# BOOL minimum_should_match=1 (should name~pro OR name~air) -> p01,p02
curl -s -XPOST "$base/api/spring-data/products/search/bool" -H 'Content-Type: application/json' \
  -d '{"must":[{"type":"term","field":"category","value":"laptop"}],"should":[{"type":"match","field":"name","value":"pro"},{"type":"match","field":"name","value":"air"}],"minimumShouldMatch":1}' | jq '{ids:[.hits[].id]}'
```
response: `{ "client": "...", "total": N, "hits": [ { id, name, category, brand, description, price, quantity, score } ] }`

### Input validation (ได้ HTTP 400)
- GET term/match: parameter ที่จำเป็น (`field`, `value`, `text`) ต้องไม่ว่างและไม่เป็น whitespace ล้วน — ถ้าขาดหรือว่าง จะได้ **400** ก่อนเรียก Elasticsearch
- POST bool: `minimumShouldMatch` เป็น optional แต่ถ้าใส่ต้องเป็นจำนวนเต็ม **ไม่ติดลบ** (`null`/0/บวก = ผ่าน, ติดลบ = **400**)
  - หมายเหตุ: Elasticsearch รองรับ `minimum_should_match` syntax ที่ซับซ้อนกว่านี้ (เปอร์เซ็นต์, ค่าติดลบ) แต่บทนี้จงใจ expose เป็น non-negative integer แบบง่ายเท่านั้น
```bash
# field ว่าง -> 400
curl -s -w ' STATUS=%{http_code}\n' "$base/api/spring-data/products/search/term?field=&value=laptop"
# ขาด value -> 400
curl -s -w ' STATUS=%{http_code}\n' "$base/api/java-client/products/search/term?field=brand"
# minimumShouldMatch ติดลบ -> 400
curl -s -w ' STATUS=%{http_code}\n' -XPOST "$base/api/spring-data/products/search/bool" -H 'Content-Type: application/json' \
  -d '{"must":[{"type":"term","field":"category","value":"laptop"}],"minimumShouldMatch":-1}'
# -> {"title":"Invalid request","status":400,...}
```

## 14. Testing
```bash
mvn clean verify
```
- unit: controller slice (term/match GET, bool POST, validation 400, empty 200 ทั้งสอง client), `ProductQueries` (การสร้าง term/match/bool + minimum_should_match), Java client service (mapping, empty, blocking isolation)
- integration (Testcontainers, security เปิดจริง, API key, seed จริง): term exact hit/miss, match, bool must/should/must_not, minimum_should_match, should-as-OR, empty, cross-client consistency
  - ต้องมี Docker daemon ทำงานอยู่

## 15. Common mistakes
- ใช้ `term` บน field `text` แล้วงงว่าทำไมไม่เจอ → ใช้ `keyword` สำหรับ exact (เช่น `category`, `brand`)
- คาดว่า `term` จะ case-insensitive → `term` บน keyword เป็น exact/case-sensitive
- คาดว่า `should` จะบังคับ match เสมอ → ถ้ามี `must` อยู่ `should` เป็นแค่ตัว boost เว้นแต่ตั้ง `minimum_should_match`
- assert ค่า `_score` แบบ exact → เปราะ ควรตรวจ membership/ordering
- ค้นไม่เจอข้อมูลที่เพิ่ง index → near-real-time (บทนี้ seed ด้วย refresh ทันทีเพื่อ demo)

## 16. Production considerations
- เปิด HTTPS บน HTTP layer เสมอใน production (บทนี้ปิด TLS เฉพาะ local)
- อย่าใช้ immediate refresh ทุก write ใน production (กระทบ performance) — บทนี้ใช้เพื่อ demo/seed
- ใช้ least-privilege API key, ความลับผ่าน env, ไม่ commit secret, ไม่ log key/Authorization header
- ระวัง deep pagination / result size ใหญ่ (เรื่อง pagination อยู่ Chapter 07)

---

## เวอร์ชัน
Java 21 · Spring Boot 4.1.2 · Spring Data Elasticsearch 6.1.2 · Elasticsearch Java client 9.4.8 · Elasticsearch server 9.4.8 · Testcontainers 2.0.5
