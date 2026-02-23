-- Insert the system user for automated operations (scheduler, background jobs).
-- This user is referenced by created_by/updated_by in auto-generated records.
INSERT INTO
    users (
        id,
        identifier,
        keycloak_id,
        email,
        first_name,
        last_name,
        email_verified_at,
        created_at,
        updated_at
    )
VALUES
    (
        '00000000-0000-0000-0000-000000000001',
        'USR000000000000000000SYSTEM',
        'system',
        'system@buurman.io',
        'System',
        'Buurman',
        now(),
        now(),
        now()
    )
ON CONFLICT (id) DO NOTHING;
