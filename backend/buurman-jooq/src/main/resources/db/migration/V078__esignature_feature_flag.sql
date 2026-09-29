INSERT INTO
    feature_flags (
        key,
        value_type,
        default_enabled,
        default_value,
        description
    )
VALUES
    (
        'esignature_enabled',
        'boolean',
        FALSE,
        NULL,
        'E-signature (Documenso) integration for generated letters/addenda'
    );
