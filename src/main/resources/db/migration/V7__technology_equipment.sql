CREATE TABLE equipment (
    id UUID PRIMARY KEY,
    number VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    organization_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX equipment_number_org_uq
    ON equipment (organization_id, number) WHERE deleted_at IS NULL;

ALTER TABLE technology_process_operation
    ADD COLUMN equipment_id UUID;
