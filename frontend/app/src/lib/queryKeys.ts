/**
 * Strips trailing `undefined` values from an array so that
 * `key('properties')` called with no extra args produces `['properties']`
 * (a valid prefix for invalidation) instead of `['properties', undefined]`.
 */
function k<T extends readonly unknown[]>(...parts: T): T {
  let end = parts.length;
  while (end > 0 && parts[end - 1] === undefined) {
    end--;
  }
  return parts.slice(0, end) as unknown as T;
}

export const queryKeys = {
  // --- Auth ---
  auth: {
    currentUser: () => k('currentUser'),
    registrationConfig: () => k('registrationConfig'),
  },

  // --- User Preferences ---
  userPreferences: {
    profile: () => k('userProfile'),
    phonePolicy: () => k('phonePolicy'),
    preferences: () => k('userPreferences'),
    notificationTypePreferences: () => k('notificationTypePreferences'),
    teams: () => k('user-teams'),
  },

  // --- Teams ---
  teams: {
    current: () => k('currentTeam'),
    members: (teamId?: string) => k('teamMembers', teamId),
    pendingInvitations: (teamId?: string) =>
      k('teamPendingInvitations', teamId),
    invitation: (token?: string) => k('invitation', token),
    pending: () => k('pendingInvitations'),
    settings: (teamId?: string) => k('teamSettings', teamId),
    reminderSettings: (teamId?: string) => k('teamReminderSettings', teamId),
    reminderPreview: (teamId?: string, tone?: string, offsetDays?: number) =>
      k('teamReminderPreview', teamId, tone, offsetDays),
  },

  // --- Properties ---
  properties: {
    all: (params?: unknown) => k('properties', params),
    detail: (id?: string) => k('property', id),
    documents: (propertyId?: string) => k('propertyDocuments', propertyId),
    auditLog: (propertyId?: string) => k('propertyAuditLog', propertyId),
    photos: (propertyId?: string) => k('propertyPhotos', propertyId),
    outdoorAreas: (propertyId?: string) => k('outdoor-areas', propertyId),
    amenitiesCatalog: (category?: string) => k('amenities', category),
    dashboard: (propertyId?: string, months?: number) =>
      k('propertyDashboard', propertyId, months),
  },

  // --- Property Financials ---
  propertyFinancials: {
    summary: (propertyId?: string) => k('propertyFinancials', propertyId),
    acquisition: (propertyId?: string) => k('propertyAcquisition', propertyId),
    valuations: (propertyId?: string) => k('propertyValuations', propertyId),
    latestValuation: (propertyId?: string) =>
      k('propertyValuation', 'latest', propertyId),
    financings: (propertyId?: string) => k('propertyFinancings', propertyId),
    financingPayments: (propertyId?: string, financingId?: string) =>
      k('financingPayments', propertyId, financingId),
    financingPaymentDocuments: (
      propertyId?: string,
      financingId?: string,
      paymentId?: string
    ) => k('financingPaymentDocuments', propertyId, financingId, paymentId),
    insurances: (propertyId?: string) => k('propertyInsurances', propertyId),
    taxes: (propertyId?: string) => k('propertyTaxes', propertyId),
    fees: (propertyId?: string) => k('propertyFees', propertyId),
  },

  // --- Contacts ---
  contacts: {
    all: (params?: unknown) => k('contacts', params),
    detail: (id?: string) => k('contact', id),
    history: (contactId?: string) => k('contactHistory', contactId),
    auditLog: (contactId?: string) => k('contactAuditLog', contactId),
    documents: (contactId?: string) => k('contactDocuments', contactId),
    photos: (contactId?: string) => k('contactPhotos', contactId),
    addresses: (contactId?: string) => k('contactAddresses', contactId),
    notes: (contactId?: string) => k('contactNotes', contactId),
    credits: (contactId?: string) => k('contactCredits', contactId),
    relationships: (contactId?: string) => k('contactRelationships', contactId),
    activity: (contactId?: string, params?: unknown) =>
      k('contactActivity', contactId, params),
  },

  // --- Contracts ---
  contracts: {
    all: (params?: unknown) => k('contracts', params),
    detail: (id?: string) => k('contract', id),
    documents: (contractId?: string) => k('contractDocuments', contractId),
    auditLog: (contractId?: string) => k('contractAuditLog', contractId),
    metadataSchema: (countryCode?: string) =>
      k('contract-metadata-schema', countryCode),
    paymentsByContract: (contractId?: string) =>
      k('paymentsByContract', contractId),
    deposit: (contractId?: string) => k('contractDeposit', contractId),
    paymentPlans: (contractId?: string) =>
      k('contractPaymentPlans', contractId),
    timeline: (contractId?: string) => k('contractTimeline', contractId),
  },

  // --- Signature Requests ---
  signatureRequests: {
    all: (contractId?: string, documentId?: string) =>
      k('signatureRequests', contractId, documentId),
    detail: (
      contractId?: string,
      documentId?: string,
      signatureRequestId?: string
    ) => k('signatureRequest', contractId, documentId, signatureRequestId),
  },

  // --- Contract Extensions ---
  contractExtensions: {
    all: (contractId?: string, page?: number, size?: number) =>
      k('contractExtensions', contractId, page, size),
    detail: (contractId?: string, extensionId?: string) =>
      k('contractExtension', contractId, extensionId),
    upcomingRenewals: () => k('upcomingRenewals'),
    pendingExtensions: () => k('pendingExtensions'),
    jurisdictionDefaults: (
      countryCode?: string,
      regionCode?: string,
      landlordType?: string,
      furnished?: boolean
    ) =>
      k(
        'jurisdictionDefaults',
        countryCode,
        regionCode,
        landlordType,
        furnished
      ),
  },

  // --- Payments ---
  payments: {
    all: (params?: unknown) => k('payments', params),
    stats: () => k('paymentStats'),
    detail: (id?: string) => k('payment', id),
    overdue: () => k('payments', 'overdue'),
    byContract: (contractId?: string) => k('payments', 'contract', contractId),
    documents: (paymentId?: string) => k('paymentDocuments', paymentId),
    auditLog: (paymentId?: string) => k('paymentAuditLog', paymentId),
    receivals: (paymentId?: string) => k('paymentReceivals', paymentId),
    reminders: (paymentId?: string) => k('paymentReminders', paymentId),
    arrears: () => k('paymentArrears'),
  },

  // --- Payment Instructions ---
  paymentInstructions: {
    all: () => k('paymentInstructions'),
    detail: (id?: string) => k('paymentInstruction', id),
    byContract: (contractId?: string) =>
      k('contractPaymentInstructions', contractId),
    currentByContract: (contractId?: string) =>
      k('currentContractPaymentInstruction', contractId),
  },

  // --- Expenses ---
  expenses: {
    all: (params?: unknown) => k('expenses', params),
    stats: () => k('expenseStats'),
    detail: (id?: string) => k('expense', id),
    byProperty: (propertyId?: string) => k('expenses', 'property', propertyId),
    summary: (period?: string) => k('expenses', 'summary', period),
    documents: (expenseId?: string) => k('expenseDocuments', expenseId),
    auditLog: (expenseId?: string) => k('expenseAuditLog', expenseId),
  },

  // --- Rent Periods ---
  rentPeriods: {
    all: (contractId?: string) => k('rentPeriods', contractId),
  },

  // --- Rent Increases ---
  // (no query keys -- only mutations that invalidate other domains)

  // --- Rent Regulation ---
  rentRegulation: {
    countries: () => k('rentRegulationCountries'),
    countryDetail: (code?: string) => k('rentRegulationCountry', code),
    currentRules: (code?: string) => k('rentRegulationCurrentRules', code),
    rulesByYear: (code?: string, year?: number) =>
      k('rentRegulationRules', code, year),
    regionRules: (code?: string, regionCode?: string) =>
      k('rentRegulationRegionRules', code, regionCode),
  },

  // --- Documents ---
  documents: {
    all: (params?: unknown) => k('documents', params),
    detail: (id?: string) => k('document', id),
  },

  // --- Photos ---
  photos: {
    all: (params?: unknown) => k('photos', params),
    detail: (id?: string) => k('photo', id),
  },

  // --- Dashboard ---
  dashboard: {
    stats: () => k('dashboard', 'stats'),
    auditLogs: (filters?: unknown) => k('auditLogs', filters),
    portfolio: (months?: number, startDate?: string, endDate?: string) =>
      k('portfolioDashboard', months, startDate, endDate),
    propertyDashboard: () => k('propertyDashboard'),
  },

  // --- Reports ---
  reports: {
    dataDateRange: () => k('data-date-range'),
    financialOverview: (
      startDate?: string,
      endDate?: string,
      propertyIds?: string[],
      currency?: string
    ) => k('financial-overview', startDate, endDate, propertyIds, currency),
    incomeTrend: (
      startDate?: string,
      endDate?: string,
      propertyIds?: string[]
    ) => k('income-trend', startDate, endDate, propertyIds),
    expenseBreakdown: (
      startDate?: string,
      endDate?: string,
      propertyIds?: string[]
    ) => k('expense-breakdown', startDate, endDate, propertyIds),
    propertyComparison: (
      startDate?: string,
      endDate?: string,
      propertyIds?: string[]
    ) => k('property-comparison', startDate, endDate, propertyIds),
    occupancyTrend: (startDate?: string, endDate?: string) =>
      k('occupancy-trend', startDate, endDate),
    taxSummary: (year?: number) => k('tax-summary', year),
  },

  // --- Notifications ---
  notifications: {
    all: (params?: unknown) => k('notifications', params),
    detail: (identifier?: string) => k('notification', identifier),
    stats: () => k('notification-stats'),
  },

  // --- Units ---
  // `detail` (and its residentialDetails/amenities sub-keys) are nested under the same 'units'
  // prefix as `all` so that invalidating the bare ['units'] prefix reaches every unit query —
  // while `all(propertyIdentifier)` and `detail(unitIdentifier)` stay independently invalidatable.
  units: {
    all: (propertyIdentifier?: string) => k('units', propertyIdentifier),
    detail: (unitIdentifier?: string) => k('units', 'detail', unitIdentifier),
    residentialDetails: (unitIdentifier?: string) =>
      k('units', 'detail', unitIdentifier, 'residentialDetails'),
    amenities: (unitIdentifier?: string) =>
      k('units', 'detail', unitIdentifier, 'amenities'),
    allocation: (expenseIdentifier?: string) =>
      k('unitAllocation', expenseIdentifier),
  },

  // --- Occupancy Periods ---
  occupancyPeriods: {
    all: (propertyIdentifier?: string) =>
      k('occupancyPeriods', propertyIdentifier),
    detail: (propertyIdentifier?: string, periodIdentifier?: string) =>
      k('occupancyPeriod', propertyIdentifier, periodIdentifier),
    timeline: (propertyIdentifier?: string) =>
      k('propertyTimeline', propertyIdentifier),
  },

  // --- Onboarding ---
  onboarding: {
    status: () => k('onboarding-status'),
    countryCurrencies: () => k('country-currencies'),
  },

  // --- Broadcasts ---
  broadcasts: {
    active: () => k('broadcasts', 'active'),
    public: (context: 'login' | 'register') =>
      k('broadcasts', 'public', context),
    all: () => k('broadcasts'),
  },

  // --- Health ---
  health: {
    status: () => k('health'),
    info: () => k('info'),
  },

  // --- Imports ---
  imports: {
    all: (page?: number, size?: number) => k('imports', page, size),
    detail: (identifier?: string | null) => k('imports', identifier),
  },

  // --- Currencies ---
  currencies: {
    all: () => k('currencies'),
  },

  // --- Calendar Feeds ---
  calendarFeeds: {
    all: () => k('calendarFeeds'),
    preview: (feedUrl: string) => k('calendarFeeds', 'preview', feedUrl),
  },

  // --- Takeouts ---
  takeouts: {
    root: ['takeouts'] as const,
    list: () => k('takeouts', 'list'),
    detail: (identifier: string) => k('takeouts', 'detail', identifier),
  },

  // --- WWS ---
  wws: {
    preFill: (unitIdentifier?: string) => k('wwsPreFill', unitIdentifier),
    calculations: (propertyIdentifier?: string) =>
      k('wwsCalculations', propertyIdentifier),
    latest: (propertyIdentifier?: string) => k('wwsLatest', propertyIdentifier),
  },
} as const;
