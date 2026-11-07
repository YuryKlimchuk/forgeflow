CREATE TABLE requirement_calculation (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    plan_id UUID NOT NULL REFERENCES plan (id),
    executed_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX requirement_calculation_org_plan_idx ON requirement_calculation (organization_id, plan_id);

CREATE TABLE requirement_calculation_item (
    id UUID PRIMARY KEY,
    calculation_id UUID NOT NULL REFERENCES requirement_calculation (id) ON DELETE CASCADE,
    item_type VARCHAR(32) NOT NULL,
    item_id UUID NOT NULL,
    number VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    need_type VARCHAR(32) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    UNIQUE (calculation_id, item_type, item_id)
);

CREATE TABLE requirement_calculation_item_source (
    calculation_item_id UUID NOT NULL REFERENCES requirement_calculation_item (id) ON DELETE CASCADE,
    source_assembly_id UUID NOT NULL,
    PRIMARY KEY (calculation_item_id, source_assembly_id)
);
