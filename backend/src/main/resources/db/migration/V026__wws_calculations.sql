-- WWS (Woningwaarderingsstelsel) calculation history for Dutch rental properties
CREATE TABLE wws_calculations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  identifier VARCHAR(26) NOT NULL,
  team_id UUID NOT NULL REFERENCES teams(id),
  property_id UUID NOT NULL REFERENCES properties(id),
  contract_id UUID REFERENCES contracts(id),
  system_version VARCHAR(30) NOT NULL,
  total_points DECIMAL(8,2) NOT NULL,
  sector_classification VARCHAR(30) NOT NULL,
  max_rent_indication DECIMAL(10,2),
  category_breakdown JSONB NOT NULL,
  input_data JSONB NOT NULL,
  calculation_date DATE NOT NULL DEFAULT CURRENT_DATE,
  notes TEXT,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW(),
  created_by UUID,
  updated_by UUID,
  deleted_at TIMESTAMP,
  UNIQUE(team_id, identifier)
);

CREATE INDEX idx_wws_calc_property ON wws_calculations(property_id);
CREATE INDEX idx_wws_calc_team ON wws_calculations(team_id);
