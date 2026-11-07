CREATE TABLE material (
    id UUID PRIMARY KEY,
    number VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    standard_type VARCHAR(255) NOT NULL,
    standard VARCHAR(255) NOT NULL,
    unit VARCHAR(255) NOT NULL,
    organization_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX material_number_org_uq
    ON material (organization_id, number) WHERE deleted_at IS NULL;

CREATE TABLE sortament (
    id UUID PRIMARY KEY,
    number VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    standard_type VARCHAR(255) NOT NULL,
    standard VARCHAR(255) NOT NULL,
    unit VARCHAR(255) NOT NULL,
    organization_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX sortament_number_org_uq
    ON sortament (organization_id, number) WHERE deleted_at IS NULL;
