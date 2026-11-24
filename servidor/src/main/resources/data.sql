MERGE INTO amostra (amostra_id, lote_id) KEY(amostra_id) VALUES
    ('A1', 'L2026-001'), ('A2', 'L2026-001'), ('A3', 'L2026-001'),
    ('A4', 'L2026-002'), ('A5', 'L2026-002'), ('A6', 'L2026-002');
INSERT INTO selecao_estacao (id, amostra_id)
    SELECT 1, 'A1' WHERE NOT EXISTS (SELECT 1 FROM selecao_estacao WHERE id = 1);
