import { queryKeys } from '../queryKeys';

describe('queryKeys', () => {
  describe('k() strips trailing undefined values', () => {
    it('properties.all() with no params returns single-element array', () => {
      expect(queryKeys.properties.all()).toEqual(['properties']);
    });

    it('properties.detail() with no id returns single-element array', () => {
      expect(queryKeys.properties.detail()).toEqual(['property']);
    });

    it('teams.members() with no teamId returns single-element array', () => {
      expect(queryKeys.teams.members()).toEqual(['teamMembers']);
    });

    it('propertyFinancials.financingPayments() with no args strips all trailing undefined', () => {
      expect(queryKeys.propertyFinancials.financingPayments()).toEqual([
        'financingPayments',
      ]);
    });
  });

  describe('keys include provided params', () => {
    it('properties.detail includes identifier', () => {
      expect(queryKeys.properties.detail('prop_abc123')).toEqual([
        'property',
        'prop_abc123',
      ]);
    });

    it('properties.all includes params object', () => {
      const params = { page: 1, size: 20 };
      expect(queryKeys.properties.all(params)).toEqual(['properties', params]);
    });

    it('teams.members includes teamId', () => {
      expect(queryKeys.teams.members('team_xyz')).toEqual([
        'teamMembers',
        'team_xyz',
      ]);
    });

    it('properties.dashboard includes propertyId and months', () => {
      expect(queryKeys.properties.dashboard('prop_1', 6)).toEqual([
        'propertyDashboard',
        'prop_1',
        6,
      ]);
    });

    it('reports.financialOverview includes all params', () => {
      expect(
        queryKeys.reports.financialOverview('2026-01', '2026-03', ['p1'], 'EUR')
      ).toEqual(['financial-overview', '2026-01', '2026-03', ['p1'], 'EUR']);
    });
  });

  describe('key shape consistency', () => {
    it('auth keys are simple strings', () => {
      expect(queryKeys.auth.currentUser()).toEqual(['currentUser']);
      expect(queryKeys.auth.registrationConfig()).toEqual([
        'registrationConfig',
      ]);
    });

    it('dashboard.stats returns nested key', () => {
      expect(queryKeys.dashboard.stats()).toEqual(['dashboard', 'stats']);
    });

    it('payments.overdue returns nested key', () => {
      expect(queryKeys.payments.overdue()).toEqual(['payments', 'overdue']);
    });

    it('takeouts.detail includes identifier', () => {
      expect(queryKeys.takeouts.detail('tk_123')).toEqual([
        'takeouts',
        'detail',
        'tk_123',
      ]);
    });
  });
});
