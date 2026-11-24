CREATE TABLE IF NOT EXISTS amostra (
    amostra_id VARCHAR(64) PRIMARY KEY,
    lote_id VARCHAR(64) NOT NULL
);
CREATE TABLE IF NOT EXISTS selecao_estacao (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    amostra_id VARCHAR(64) NOT NULL REFERENCES amostra(amostra_id)
);
CREATE SEQUENCE IF NOT EXISTS captura_id START WITH 1;
CREATE TABLE IF NOT EXISTS captura (
    id BIGINT PRIMARY KEY,
    estacao_id VARCHAR(16) NOT NULL,
    lote_id VARCHAR(64) NOT NULL,
    amostra_id VARCHAR(64) NOT NULL REFERENCES amostra(amostra_id),
    lux DOUBLE PRECISION NOT NULL CHECK (lux >= 0),
    resolucao VARCHAR(16) NOT NULL,
    simulacao BOOLEAN NOT NULL,
    recebida_em TIMESTAMP(9) WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS captura_por_amostra ON captura(amostra_id, recebida_em, id);
CREATE SEQUENCE IF NOT EXISTS comando_id START WITH 1;
CREATE TABLE IF NOT EXISTS comando (
    id BIGINT PRIMARY KEY,
    tipo VARCHAR(32) NOT NULL CHECK (tipo IN ('capturar', 'status', 'ajustar_brilho')),
    valor INTEGER,
    entregue BOOLEAN NOT NULL,
    CHECK ((tipo = 'ajustar_brilho' AND valor IS NOT NULL AND valor BETWEEN 0 AND 10)
        OR (tipo IN ('capturar', 'status') AND valor IS NULL))
);
