# 안심구매 MCP

해외직구·중고거래 전에 제품의 공식 리콜 이력을 확인하는 Java/Spring Boot 기반 MCP 서버입니다.
위치정보, 회원정보, 데이터베이스를 사용하지 않으며 미국 소비자제품안전위원회(CPSC)의 공개 Recall API를 실시간 조회합니다.

## 제공 도구

- `search_product_recalls`: 제품명·브랜드·모델 번호로 공식 리콜 후보 검색
- `get_recall_details`: 리콜 대상 범위, 위험, 사고, 환불·교환·수리 방법 조회
- `create_product_safety_checklist`: 제품 라벨 확인 및 즉시 사용 중지 체크리스트 생성

## 기술 구성

- Java 17
- Spring Boot 3.5
- Spring AI MCP Server WebMVC
- Streamable HTTP
- Gradle

## 로컬 실행

```bash
./gradlew bootRun
```

Windows:

```powershell
.\gradlew.bat bootRun
```

기본 포트는 `8080`, MCP 엔드포인트는 `/mcp`, 상태 확인은 `/actuator/health`입니다.

## 환경 변수

| 이름 | 기본값 | 설명 |
|---|---:|---|
| `PORT` | `8080` | 서버 포트 |
| `CPSC_BASE_URL` | `https://www.saferproducts.gov` | CPSC API 기준 URL |
| `CPSC_READ_TIMEOUT_MILLIS` | `8000` | 외부 API 읽기 제한시간 |
| `CPSC_MAX_RESULTS` | `10` | 한 번에 반환할 최대 결과 수 |

API 키와 사용자 데이터는 필요하지 않습니다.

## Docker

```bash
docker build -t agentic-player-safety-mcp .
docker run --rm -p 8080:8080 agentic-player-safety-mcp
```

## 추천 시연 질문

```text
SEGMART SOSTT051BR 미니 트램펄린을 중고로 사려는데 리콜 대상인지 확인해줘.
```

```text
TurboSke FX010 어린이 자전거 헬멧의 위험과 환불 방법을 공식 출처로 확인해줘.
```

## 주의사항

- 현재 공급자는 미국 CPSC이므로 미국 소비자제품 리콜만 조회합니다.
- 결과가 없더라도 안전이 보장되는 것은 아닙니다.
- 실제 동일 제품 여부는 모델·제조번호·생산일자·사진을 공식 공고와 대조해야 합니다.
- 환불, 폐기, 수리는 공식 리콜 페이지의 최신 안내를 우선합니다.

## 데이터 출처

- CPSC Recall API: https://www.cpsc.gov/Recalls/CPSC-Recalls-Application-Program-Interface-API-Information
- API endpoint: https://www.saferproducts.gov/RestWebServices/Recall
