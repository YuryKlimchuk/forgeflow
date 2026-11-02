CREATE TABLE plan (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL
);
CREATE UNIQUE INDEX plan_name_org_uq ON plan (organization_id, name);

CREATE TABLE plan_item (
    plan_id UUID NOT NULL REFERENCES plan (id) ON DELETE CASCADE,
    assembly_id UUID NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (plan_id, assembly_id)
);
