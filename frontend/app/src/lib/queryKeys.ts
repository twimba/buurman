export const queryKeys = {
  // --- Auth ---
  auth: {
    currentUser: () => ['currentUser'] as const,
    registrationConfig: () => ['registrationConfig'] as const,
  },

  // --- User Preferences ---
  userPreferences: {
    profile: () => ['userProfile'] as const,
    phonePolicy: () => ['phonePolicy'] as const,
    preferences: () => ['userPreferences'] as const,
    notificationTypePreferences: () =>
      ['notificationTypePreferences'] as const,
    teams: () => ['user-teams'] as const,
  },

  // --- Teams ---
  teams: {
    current: () => ['currentTeam'] as const,
    members: (teamId?: string) => ['teamMembers', teamId] as const,
    pendingInvitations: (teamId?: string) =>
      ['teamPendingInvitations', teamId] as const,
    invitation: (token?: string) => ['invitation', token] as const,
    pending: () => ['pendingInvitations'] as const,
    settings: (teamId?: string) => ['teamSettings', teamId] as const,
  },

  // --- Properties ---
  properties: {
    all: (params?: unknown) => ['properties', params] as const,
    detail: (id?: string) => ['property', id] as const,
    documents: (propertyId?: string) =>
      ['propertyDocuments', propertyId] as const,
    auditLog: (propertyId?: string) =>
      ['propertyAuditLog', propertyId] as const,
    photos: (propertyId?: string) => ['propertyPhotos', propertyId] as const,
    outdoorAreas: (propertyId?: string) =>
      ['outdoor-areas', propertyId] as const,
    amenities: (propertyId?: string) =>
      ['property-amenities', propertyId] as const,
    amenitiesCatalog: (category?: string) => ['amenities', category] as const,
    dashboard: (propertyId?: string, months?: number) =>
      ['propertyDashboard', propertyId, months] as const,
  },

  // --- Property Financials ---
  propertyFinancials: {
    summary: (propertyId?: string) =>
      ['propertyFinancials', propertyId] as const,
    acquisition: (propertyId?: string) =>
      ['propertyAcquisition', propertyId] as const,
    valuations: (propertyId?: string) =>
      ['propertyValuations', propertyId] as const,
    latestValuation: (propertyId?: string) =>
      ['propertyValuation', 'latest', propertyId] as const,
    financings: (propertyId?: string) =>
      ['propertyFinancings', propertyId] as const,
    financingPayments: (propertyId?: string, financingId?: string) =>
      ['financingPayments', propertyId, financingId] as const,
    financingPaymentDocuments: (
      propertyId?: string,
      financingId?: string,
      paymentId?: string
    ) =>
      [
        'financingPaymentDocuments',
        propertyId,
        financingId,
        paymentId,
      ] as const,
    insurances: (propertyId?: string) =>
      ['propertyInsurances', propertyId] as const,
    taxes: (propertyId?: string) => ['propertyTaxes', propertyId] as const,
    fees: (propertyId?: string) => ['propertyFees', propertyId] as const,
  },

  // --- Contacts ---
  contacts: {
    all: (params?: unknown) => ['contacts', params] as const,
    detail: (id?: string) => ['contact', id] as const,
    history: (contactId?: string) => ['contactHistory', contactId] as const,
    auditLog: (contactId?: string) =>
      ['contactAuditLog', contactId] as const,
    documents: (contactId?: string) =>
      ['contactDocuments', contactId] as const,
    photos: (contactId?: string) => ['contactPhotos', contactId] as const,
    addresses: (contactId?: string) =>
      ['contactAddresses', contactId] as const,
    notes: (contactId?: string) => ['contactNotes', contactId] as const,
    relationships: (contactId?: string) =>
      ['contactRelationships', contactId] as const,
    activity: (contactId?: string, params?: unknown) =>
      ['contactActivity', contactId, params] as const,
  },

  // --- Contracts ---
  contracts: {
    all: (params?: unknown) => ['contracts', params] as const,
    detail: (id?: string) => ['contract', id] as const,
    documents: (contractId?: string) =>
      ['contractDocuments', contractId] as const,
    auditLog: (contractId?: string) =>
      ['contractAuditLog', contractId] as const,
    metadataSchema: (countryCode?: string) =>
      ['contract-metadata-schema', countryCode] as const,
    paymentsByContract: (contractId?: string) =>
      ['paymentsByContract', contractId] as const,
  },

  // --- Contract Extensions ---
  contractExtensions: {
    all: (contractId?: string, page?: number, size?: number) =>
      ['contractExtensions', contractId, page, size] as const,
    detail: (contractId?: string, extensionId?: string) =>
      ['contractExtension', contractId, extensionId] as const,
    upcomingRenewals: () => ['upcomingRenewals'] as const,
    pendingExtensions: () => ['pendingExtensions'] as const,
    jurisdictionDefaults: (
      countryCode?: string,
      regionCode?: string,
      landlordType?: string,
      furnished?: boolean
    ) =>
      [
        'jurisdictionDefaults',
        countryCode,
        regionCode,
        landlordType,
        furnished,
      ] as const,
  },

  // --- Payments ---
  payments: {
    all: (params?: unknown) => ['payments', params] as const,
    stats: () => ['paymentStats'] as const,
    detail: (id?: string) => ['payment', id] as const,
    overdue: () => ['payments', 'overdue'] as const,
    byContract: (contractId?: string) =>
      ['payments', 'contract', contractId] as const,
    documents: (paymentId?: string) =>
      ['paymentDocuments', paymentId] as const,
    auditLog: (paymentId?: string) =>
      ['paymentAuditLog', paymentId] as const,
    receivals: (paymentId?: string) =>
      ['paymentReceivals', paymentId] as const,
  },

  // --- Payment Instructions ---
  paymentInstructions: {
    all: () => ['paymentInstructions'] as const,
    detail: (id?: string) => ['paymentInstruction', id] as const,
    byContract: (contractId?: string) =>
      ['contractPaymentInstructions', contractId] as const,
    currentByContract: (contractId?: string) =>
      ['currentContractPaymentInstruction', contractId] as const,
  },

  // --- Expenses ---
  expenses: {
    all: (params?: unknown) => ['expenses', params] as const,
    stats: () => ['expenseStats'] as const,
    detail: (id?: string) => ['expense', id] as const,
    byProperty: (propertyId?: string) =>
      ['expenses', 'property', propertyId] as const,
    summary: (period?: string) => ['expenses', 'summary', period] as const,
    documents: (expenseId?: string) =>
      ['expenseDocuments', expenseId] as const,
    auditLog: (expenseId?: string) =>
      ['expenseAuditLog', expenseId] as const,
  },

  // --- Rent Periods ---
  rentPeriods: {
    all: (contractId?: string) => ['rentPeriods', contractId] as const,
  },

  // --- Rent Increases ---
  // (no query keys -- only mutations that invalidate other domains)

  // --- Rent Regulation ---
  rentRegulation: {
    countries: () => ['rentRegulationCountries'] as const,
    countryDetail: (code?: string) =>
      ['rentRegulationCountry', code] as const,
    currentRules: (code?: string) =>
      ['rentRegulationCurrentRules', code] as const,
    rulesByYear: (code?: string, year?: number) =>
      ['rentRegulationRules', code, year] as const,
    regionRules: (code?: string, regionCode?: string) =>
      ['rentRegulationRegionRules', code, regionCode] as const,
  },

  // --- Documents ---
  documents: {
    all: (params?: unknown) => ['documents', params] as const,
    detail: (id?: string) => ['document', id] as const,
  },

  // --- Photos ---
  photos: {
    all: (params?: unknown) => ['photos', params] as const,
    detail: (id?: string) => ['photo', id] as const,
  },

  // --- Dashboard ---
  dashboard: {
    stats: () => ['dashboard', 'stats'] as const,
    auditLogs: (filters?: unknown) => ['auditLogs', filters] as const,
    portfolio: (months?: number) => ['portfolioDashboard', months] as const,
    propertyDashboard: () => ['propertyDashboard'] as const,
  },

  // --- Reports ---
  reports: {
    dataDateRange: () => ['data-date-range'] as const,
    financialOverview: (
      startDate?: string,
      endDate?: string,
      propertyIds?: string[],
      currency?: string
    ) =>
      [
        'financial-overview',
        startDate,
        endDate,
        propertyIds,
        currency,
      ] as const,
    incomeTrend: (
      startDate?: string,
      endDate?: string,
      propertyIds?: string[]
    ) => ['income-trend', startDate, endDate, propertyIds] as const,
    expenseBreakdown: (
      startDate?: string,
      endDate?: string,
      propertyIds?: string[]
    ) => ['expense-breakdown', startDate, endDate, propertyIds] as const,
    propertyComparison: (
      startDate?: string,
      endDate?: string,
      propertyIds?: string[]
    ) => ['property-comparison', startDate, endDate, propertyIds] as const,
    occupancyTrend: (startDate?: string, endDate?: string) =>
      ['occupancy-trend', startDate, endDate] as const,
    taxSummary: (year?: number) => ['tax-summary', year] as const,
  },

  // --- Notifications ---
  notifications: {
    all: (params?: unknown) => ['notifications', params] as const,
    detail: (identifier?: string) => ['notification', identifier] as const,
    stats: () => ['notification-stats'] as const,
  },

  // --- Occupancy Periods ---
  occupancyPeriods: {
    all: (propertyIdentifier?: string) =>
      ['occupancyPeriods', propertyIdentifier] as const,
    detail: (propertyIdentifier?: string, periodIdentifier?: string) =>
      ['occupancyPeriod', propertyIdentifier, periodIdentifier] as const,
    timeline: (propertyIdentifier?: string) =>
      ['propertyTimeline', propertyIdentifier] as const,
  },

  // --- Onboarding ---
  onboarding: {
    status: () => ['onboarding-status'] as const,
    countryCurrencies: () => ['country-currencies'] as const,
  },

  // --- Broadcasts ---
  broadcasts: {
    active: () => ['broadcasts', 'active'] as const,
    public: (context: 'login' | 'register') =>
      ['broadcasts', 'public', context] as const,
    all: () => ['broadcasts'] as const,
  },

  // --- Health ---
  health: {
    status: () => ['health'] as const,
    info: () => ['info'] as const,
  },

  // --- Imports ---
  imports: {
    all: (page?: number, size?: number) => ['imports', page, size] as const,
    detail: (identifier?: string | null) =>
      ['imports', identifier] as const,
  },

  // --- Currencies ---
  currencies: {
    all: () => ['currencies'] as const,
  },

  // --- Calendar Feeds ---
  calendarFeeds: {
    all: () => ['calendarFeeds'] as const,
  },

  // --- Takeouts ---
  takeouts: {
    root: ['takeouts'] as const,
    list: () => ['takeouts', 'list'] as const,
    detail: (identifier: string) =>
      ['takeouts', 'detail', identifier] as const,
  },

  // --- WWS ---
  wws: {
    preFill: (propertyIdentifier?: string) =>
      ['wwsPreFill', propertyIdentifier] as const,
    calculations: (propertyIdentifier?: string) =>
      ['wwsCalculations', propertyIdentifier] as const,
    latest: (propertyIdentifier?: string) =>
      ['wwsLatest', propertyIdentifier] as const,
  },
} as const;
