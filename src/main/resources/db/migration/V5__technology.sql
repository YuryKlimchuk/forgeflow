CREATE TABLE technology_process (
    id UUID PRIMARY KEY,
    number VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    part_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    default_process BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX technology_process_number_org_uq
    ON technology_process (organization_id, number) WHERE deleted_at IS NULL;

CREATE TABLE technology_process_operation (
    process_id UUID NOT NULL REFERENCES technology_process (id) ON DELETE CASCADE,
    operation_number INT NOT NULL,
    operation_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (process_id, operation_number)
);
