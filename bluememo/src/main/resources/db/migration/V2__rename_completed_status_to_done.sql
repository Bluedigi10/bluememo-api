ALTER TABLE todos
    DROP CONSTRAINT IF EXISTS ck_todos_status;

UPDATE todos
SET status = 'DONE'
WHERE status = 'COMPLETED';

ALTER TABLE todos
    ADD CONSTRAINT ck_todos_status
    CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DONE'));