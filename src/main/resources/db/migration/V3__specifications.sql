CREATE TABLE specification_row (
    organization_id UUID NOT NULL,
    assembly_id UUID NOT NULL,
    item_type VARCHAR(32) NOT NULL,
    item_id UUID NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (organization_id, assembly_id, item_type, item_id)
);

CREATE TABLE substitution (
    organization_id UUID NOT NULL,
    assembly_id UUID NOT NULL,
    item_type VARCHAR(32) NOT NULL,
    item_id UUID NOT NULL,
    substitute_type VARCHAR(32) NOT NULL,
    substitute_id UUID NOT NULL,
    PRIMARY KEY (organization_id, assembly_id, item_type, item_id, substitute_type, substitute_id)
);
