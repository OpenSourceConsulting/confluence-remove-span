# confluence-remove-span

### ㅇ 필수 조건

##### - Java 1.8+

### ㅇ 실행 방법

##### - java [OPTIONS] -Djdbc.url=[Value] -Djdbc.username=[Value] -Djdbc.password=[Value] -jar remove-span.jar [ARG_OPTION]

 
### ㅇ OPTIONS (Java  표준 VM Arguments 전달 방식으로 -D 를 포함한다.)

##### -  jdbc.driver : 현재 Oracle만 지원 가능하므로 oracle.jdbc.OracleDriver 만 지원

##### -  wiki.span.count : 쿼리 조건으로 <span lang=”ko”> 태그의 연속된 갯수 (기본값은 3)

##### -  wiki.dryRun : true 또는 false의 Boolean 값으로, true를 입력할 경우 실제 DB Update는 하지 않고 지정된 파라메타에 따라 DB 조회 후 span 태그가 삭제된 결과 텍스트를 콘솔에서 확인한다.

 
### ㅇ ARG_OPTION

##### - BODYCONTENT 테이블의 CONTENTID 값으로 wiki.span.count 값에 상관없이 주어진 contentId 값으로 쿼리하여 span 태그를 제거하고 DB에 업데이트 한다. (wiki.dryRun 옵션을 동시 사용 가능)