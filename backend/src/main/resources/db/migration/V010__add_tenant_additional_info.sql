-- Add rich text field for additional tenant information
ALTER TABLE tenants ADD COLUMN additional_info TEXT;

-- Migrate existing tax_number and id_number into HTML format
UPDATE tenants
SET additional_info = CONCAT(
    '<p>',
    CASE WHEN tax_number IS NOT NULL THEN CONCAT('Tax Number: ', tax_number, '<br>') ELSE '' END,
    CASE WHEN id_number IS NOT NULL THEN CONCAT('ID Number: ', id_number) ELSE '' END,
    '</p>'
)
WHERE tax_number IS NOT NULL OR id_number IS NOT NULL;

-- Comment for documentation
COMMENT ON COLUMN tenants.additional_info IS 'Rich text field for additional tenant information (replaces tax_number and id_number)';
