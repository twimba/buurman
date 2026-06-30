import { useState } from 'react';
import { FileText, FileDown, Languages, Shield, Globe } from 'lucide-react';
import { useGenerateExtensionDocuments } from '@/hooks/useContractExtensionHooks';
import type { GenerateExtensionDocumentsRequest } from '@/generated/models';
import { useTranslation } from 'react-i18next';

const SUPPORTED_LANGUAGES = [
  { code: 'en', label: 'English' },
  { code: 'nl', label: 'Nederlands' },
  { code: 'de', label: 'Deutsch' },
  { code: 'fr', label: 'Français' },
  { code: 'pt', label: 'Português' },
  { code: 'es', label: 'Español' },
  { code: 'sv', label: 'Svenska' },
  { code: 'it', label: 'Italiano' },
] as const;

/** Maps country codes to their official/mandatory language(s) for legal documents. */
export const COUNTRY_OFFICIAL_LANGUAGES: Record<string, string[]> = {
  NL: ['nl'],
  BE: ['nl', 'fr', 'de'],
  DE: ['de'],
  AT: ['de'],
  CH: ['de', 'fr', 'it'],
  FR: ['fr'],
  PT: ['pt'],
  BR: ['pt'],
  ES: ['es'],
  SE: ['sv'],
  IT: ['it'],
  GB: ['en'],
  IE: ['en'],
  US: ['en'],
  CA: ['en', 'fr'],
  LU: ['fr', 'de'],
};

interface GenerateDocumentsModalProps {
  contractIdentifier: string;
  extensionIdentifier: string;
  extensionNumber: number;
  defaultLanguages: string[];
  countryCode?: string;
  regenerate?: boolean;
  onClose: () => void;
}

export const GenerateDocumentsModal = ({
  contractIdentifier,
  extensionIdentifier,
  extensionNumber,
  defaultLanguages,
  countryCode,
  regenerate = false,
  onClose,
}: GenerateDocumentsModalProps) => {
  const { t } = useTranslation('contracts');

  const DOCUMENT_TYPES = [
    {
      value: 'EXTENSION_ADDENDUM' as const,
      label: t('generateDocuments.types.EXTENSION_ADDENDUM.label'),
      icon: FileText,
      description: t('generateDocuments.types.EXTENSION_ADDENDUM.description'),
    },
    {
      value: 'RENT_INCREASE_LETTER' as const,
      label: t('generateDocuments.types.RENT_INCREASE_LETTER.label'),
      icon: FileDown,
      description: t(
        'generateDocuments.types.RENT_INCREASE_LETTER.description'
      ),
    },
  ];

  const officialLanguages = countryCode
    ? (COUNTRY_OFFICIAL_LANGUAGES[countryCode.toUpperCase()] ?? [])
    : [];
  const [selectedTypes, setSelectedTypes] = useState<Set<string>>(
    new Set(['EXTENSION_ADDENDUM', 'RENT_INCREASE_LETTER'])
  );
  const [selectedLanguages, setSelectedLanguages] = useState<Set<string>>(
    () => {
      const initial = defaultLanguages.length > 0 ? defaultLanguages : ['en'];
      return new Set([...initial, ...officialLanguages]);
    }
  );

  const generateDocs = useGenerateExtensionDocuments(contractIdentifier);

  const toggleType = (type: string) => {
    setSelectedTypes((prev) => {
      const next = new Set(prev);
      if (next.has(type)) {
        next.delete(type);
      } else {
        next.add(type);
      }
      return next;
    });
  };

  const toggleLanguage = (lang: string) => {
    if (officialLanguages.includes(lang)) {
      return;
    }
    setSelectedLanguages((prev) => {
      const next = new Set(prev);
      if (next.has(lang)) {
        if (next.size > 1) {
          next.delete(lang);
        }
      } else {
        next.add(lang);
      }
      return next;
    });
  };

  const totalDocuments = selectedTypes.size * selectedLanguages.size;

  const handleGenerate = () => {
    const request: GenerateExtensionDocumentsRequest = {
      documentTypes: Array.from(
        selectedTypes
      ) as GenerateExtensionDocumentsRequest['documentTypes'],
      languages: Array.from(selectedLanguages),
      ...(regenerate && { replaceExisting: true }),
    };
    generateDocs.mutate(
      { extensionId: extensionIdentifier, request },
      { onSuccess: () => onClose() }
    );
  };

  return (
    <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-surface-card rounded-lg shadow-xl dark:shadow-black/20 max-w-lg w-full mx-4">
        {/* Header */}
        <div className="p-4 border-b border-border-default">
          <h3 className="text-lg font-semibold text-text-primary">
            {regenerate
              ? t('generateDocuments.regenerateTitle')
              : t('generateDocuments.title')}
          </h3>
          <p className="text-sm text-text-secondary mt-1">
            Extension #{extensionNumber} —{' '}
            {regenerate
              ? t('generateDocuments.regenerateDescription')
              : t('generateDocuments.selectDescription')}
          </p>
        </div>

        <div className="p-4 space-y-5">
          {/* Document Types */}
          <div>
            <label className="block text-sm font-medium text-text-primary mb-2">
              {t('generateDocuments.documentTypes')}
            </label>
            <div className="space-y-2">
              {DOCUMENT_TYPES.map((type) => {
                const Icon = type.icon;
                const isSelected = selectedTypes.has(type.value);
                return (
                  <button
                    key={type.value}
                    type="button"
                    onClick={() => toggleType(type.value)}
                    className={`w-full flex items-center gap-3 p-3 rounded-lg border transition-colors text-left ${
                      isSelected
                        ? 'border-primary-500 bg-primary-500/5'
                        : 'border-border-default hover:border-border-strong'
                    }`}
                  >
                    <div
                      className={`flex items-center justify-center w-8 h-8 rounded-md ${
                        isSelected
                          ? 'bg-primary-500/10 text-primary-500'
                          : 'bg-surface-inset text-text-muted'
                      }`}
                    >
                      <Icon className="h-4 w-4" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p
                        className={`text-sm font-medium ${
                          isSelected
                            ? 'text-text-primary'
                            : 'text-text-secondary'
                        }`}
                      >
                        {type.label}
                      </p>
                      <p className="text-xs text-text-muted">
                        {type.description}
                      </p>
                    </div>
                    <div
                      className={`w-5 h-5 rounded border-2 flex items-center justify-center transition-colors ${
                        isSelected
                          ? 'border-primary-500 bg-primary-500'
                          : 'border-border-strong'
                      }`}
                    >
                      {isSelected && (
                        <svg
                          className="w-3 h-3 text-white"
                          viewBox="0 0 12 12"
                          fill="none"
                        >
                          <path
                            d="M2 6L5 9L10 3"
                            stroke="currentColor"
                            strokeWidth="2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          />
                        </svg>
                      )}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Languages */}
          <div>
            <label className="flex items-center gap-1.5 text-sm font-medium text-text-primary mb-2">
              <Languages className="h-4 w-4" />
              {t('generateDocuments.languages')}
            </label>
            {officialLanguages.length > 0 && (
              <div className="flex items-center gap-4 mb-2 text-xs text-text-muted">
                <span className="flex items-center gap-1">
                  <Shield className="h-3 w-3 text-amber-500" />
                  {t('generateDocuments.officialLanguage')}
                </span>
                <span className="flex items-center gap-1">
                  <Globe className="h-3 w-3 text-sky-500" />
                  {t('generateDocuments.translation')}
                </span>
              </div>
            )}
            <div className="grid grid-cols-2 gap-2">
              {SUPPORTED_LANGUAGES.map((lang) => {
                const isSelected = selectedLanguages.has(lang.code);
                const isOfficial = officialLanguages.includes(lang.code);
                const isTranslation =
                  officialLanguages.length > 0 && !isOfficial;
                return (
                  <button
                    key={lang.code}
                    type="button"
                    onClick={() => toggleLanguage(lang.code)}
                    className={`flex items-center gap-2 px-3 py-2 rounded-md border text-sm transition-colors ${
                      isOfficial
                        ? 'border-amber-400/60 bg-amber-500/5 text-text-primary font-medium cursor-default'
                        : isSelected
                          ? 'border-primary-500 bg-primary-500/5 text-text-primary font-medium'
                          : 'border-border-default text-text-secondary hover:border-border-strong'
                    }`}
                  >
                    <div
                      className={`w-4 h-4 rounded border-2 flex items-center justify-center flex-shrink-0 ${
                        isSelected
                          ? 'border-primary-500 bg-primary-500'
                          : 'border-border-strong'
                      }`}
                    >
                      {isSelected && (
                        <svg
                          className="w-2.5 h-2.5 text-white"
                          viewBox="0 0 12 12"
                          fill="none"
                        >
                          <path
                            d="M2 6L5 9L10 3"
                            stroke="currentColor"
                            strokeWidth="2"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          />
                        </svg>
                      )}
                    </div>
                    <span className="uppercase text-xs font-mono w-5">
                      {lang.code}
                    </span>
                    <span className="flex-1 text-left">{lang.label}</span>
                    {isOfficial && (
                      <Shield className="h-3.5 w-3.5 text-amber-500 flex-shrink-0" />
                    )}
                    {isTranslation && isSelected && (
                      <Globe className="h-3.5 w-3.5 text-sky-500 flex-shrink-0" />
                    )}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Summary */}
          {totalDocuments > 0 && (
            <div className="bg-surface-inset rounded-md px-3 py-2 text-sm text-text-secondary">
              {t('generateDocuments.willGenerate', { count: totalDocuments })}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 p-4 border-t border-border-default">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-text-secondary bg-surface-card border border-border-strong rounded-md hover:bg-surface-inset"
            disabled={generateDocs.isPending}
          >
            {t('common:buttons.cancel')}
          </button>
          <button
            type="button"
            onClick={handleGenerate}
            disabled={selectedTypes.size === 0 || generateDocs.isPending}
            className="px-4 py-2 text-sm font-medium text-white bg-primary-500 rounded-md hover:bg-primary-600 disabled:opacity-50 transition-colors"
          >
            {generateDocs.isPending
              ? regenerate
                ? t('generateDocuments.regenerating')
                : t('generateDocuments.generating')
              : regenerate
                ? t('generateDocuments.regenerateButton', {
                    count: totalDocuments,
                  })
                : t('generateDocuments.generateButton', {
                    count: totalDocuments,
                  })}
          </button>
        </div>
      </div>
    </div>
  );
};
