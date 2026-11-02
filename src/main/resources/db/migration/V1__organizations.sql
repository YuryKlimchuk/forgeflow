CREATE TABLE organization (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    part_number_pattern VARCHAR(255),
    assembly_number_pattern VARCHAR(255),
    buy_number_pattern VARCHAR(255),
    standart_number_pattern VARCHAR(255),
    vzk_number_pattern VARCHAR(255)
);

CREATE TABLE organization_member (
    organization_id UUID NOT NULL REFERENCES organization (id) ON DELETE CASCADE,
    user_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (organization_id, user_id)
);

CREATE TABLE owner (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL UNIQUE,
    organization_id UUID REFERENCES organization (id)
);
