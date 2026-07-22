-- 우리가 직접 만들고 제어하는 운영 테이블 (4.2). ORDER_TB / SHIPMENT_TB는 여기 없다
-- (RECRUIT 소유의 스펙 제공 테이블이고, 별도 Oracle DataSource로 접근한다).
--
-- 이 스키마는 로컬 H2(localDataSource)에 대해서만 실행된다 (EaiConfig.localDataSource 참조).
-- APPLICANT 계정이 Oracle에서 CREATE TABLE 권한이 없어(ORA-01031 확인됨), 우리가 완전히
-- 통제 가능한 별도 DB에 이 테이블들을 둔다. H2는 기본 모드로도 VARCHAR2/CLOB/TIMESTAMP를
-- 지원하므로 Oracle과 동일한 DDL을 그대로 재사용할 수 있다.
--
-- 재기동 시 "이미 존재함" 에러는 EaiConfig에서 continueOnError(true)로 무시하고 넘어간다.

CREATE TABLE OUTBOX (
    OUTBOX_ID       VARCHAR2(64)  PRIMARY KEY,
    APPLICANT_KEY   VARCHAR2(64)  NOT NULL,
    CORRELATION_ID  VARCHAR2(64)  NOT NULL,
    FILE_NAME       VARCHAR2(255) NOT NULL,
    PAYLOAD         CLOB          NOT NULL,
    SEND_STATUS     VARCHAR2(1)   NOT NULL,
    RETRY_COUNT     NUMBER(10)    DEFAULT 0 NOT NULL,
    CREATED_AT      TIMESTAMP     NOT NULL
);

CREATE TABLE INTEGRATION_HISTORY (
    HISTORY_ID      VARCHAR2(64)  PRIMARY KEY,
    CORRELATION_ID  VARCHAR2(64)  NOT NULL,
    INTERFACE_ID    VARCHAR2(32)  NOT NULL,
    STAGE           VARCHAR2(32)  NOT NULL,
    STATUS          VARCHAR2(8)   NOT NULL,
    SOURCE_PAYLOAD  CLOB,
    TARGET_PAYLOAD  CLOB,
    ERROR_DETAIL    CLOB,
    RETRY_SEQ       NUMBER(10),
    LOG_LEVEL       VARCHAR2(8)   NOT NULL,
    CREATED_AT      TIMESTAMP     NOT NULL
);

CREATE INDEX idx_outbox_pending ON OUTBOX (SEND_STATUS, RETRY_COUNT);
CREATE INDEX idx_history_correlation ON INTEGRATION_HISTORY (CORRELATION_ID);
