CREATE TABLE base.tenant_deletion_requests (
                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                               tenant_id UUID NOT NULL,
                                               requested_by UUID NOT NULL,
                                               approved_by_1 UUID,
                                               approved_by_2 UUID,
                                               reason TEXT,
                                               note TEXT,
                                               status TEXT NOT NULL DEFAULT 'PENDING',
                                               created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                               approved_at TIMESTAMPTZ
);


ALTER TABLE base.buildings
    ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE base.buildings
    ADD CONSTRAINT ck_buildings_status
        CHECK (status IN (
                          'ACTIVE',
                          'LOCKED',
                          'PENDING_DELETION',    -
                          'DELETING',
                          'ARCHIVED'
            ));
