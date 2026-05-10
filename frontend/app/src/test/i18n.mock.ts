import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import commonEn from '../../public/locales/en/common.json';
import navigationEn from '../../public/locales/en/navigation.json';
import propertiesEn from '../../public/locales/en/properties.json';
import tenantsEn from '../../public/locales/en/tenants.json';
import contractsEn from '../../public/locales/en/contracts.json';
import paymentsEn from '../../public/locales/en/payments.json';
import expensesEn from '../../public/locales/en/expenses.json';
import documentsEn from '../../public/locales/en/documents.json';
import settingsEn from '../../public/locales/en/settings.json';
import adminEn from '../../public/locales/en/admin.json';

i18n.use(initReactI18next).init({
  lng: 'en',
  fallbackLng: 'en',
  ns: ['common', 'navigation', 'properties', 'tenants', 'contracts', 'payments', 'expenses', 'documents', 'settings', 'admin'],
  defaultNS: 'common',
  resources: {
    en: {
      common: commonEn,
      navigation: navigationEn,
      properties: propertiesEn,
      tenants: tenantsEn,
      contracts: contractsEn,
      payments: paymentsEn,
      expenses: expensesEn,
      documents: documentsEn,
      settings: settingsEn,
      admin: adminEn,
    },
  },
  interpolation: { escapeValue: false },
  react: { useSuspense: false },
});

export default i18n;
