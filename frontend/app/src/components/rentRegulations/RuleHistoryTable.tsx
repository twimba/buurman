import { ExternalLink } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { RichTextDisplay } from '@buurman/ui';
import type { RentRegulationRuleResponse } from '@/types/rentRegulation';

interface RuleHistoryTableProps {
  rules: RentRegulationRuleResponse[];
}

const humanizeEnum = (value: string): string => {
  const labels: Record<string, string> = {
    FIXED_PERCENTAGE: 'Fixed Percentage',
    CPI_LINKED: 'CPI Linked',
    INDEX_LINKED: 'Index Linked',
    MARKET_RENT: 'Market Rent',
    NEGOTIATED: 'Negotiated',
    FROZEN: 'Frozen',
    OTHER: 'Other',
  };
  return (
    labels[value] ??
    value.replace(/_/g, '').replace(/\b\w/g, (c) => c.toUpperCase())
  );
};

export const RuleHistoryTable = ({ rules }: RuleHistoryTableProps) => {
  const { t } = useTranslation('contracts');
  const currentYear = new Date().getFullYear();
  const sortedRules = [...rules].sort((a, b) => b.year - a.year);

  if (sortedRules.length === 0) {
    return (
      <div className="bg-surface-card rounded-lg border border-border-default p-8 text-center">
        <p className="text-text-secondary">{t('rentRegulations.noRules')}</p>
      </div>
    );
  }

  return (
    <div className="bg-surface-card rounded-lg border border-border-default overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border-default">
              <th className="text-left px-4 py-3 font-semibold text-text-secondary">
                Year
              </th>
              <th className="text-left px-4 py-3 font-semibold text-text-secondary">
                Category
              </th>
              <th className="text-left px-4 py-3 font-semibold text-text-secondary">
                Max Increase
              </th>
              <th className="text-left px-4 py-3 font-semibold text-text-secondary">
                Type
              </th>
              <th className="text-left px-4 py-3 font-semibold text-text-secondary">
                Effective Date
              </th>
              <th className="text-left px-4 py-3 font-semibold text-text-secondary">
                Notice Period
              </th>
              <th className="text-left px-4 py-3 font-semibold text-text-secondary">
                Source
              </th>
            </tr>
          </thead>
          <tbody>
            {sortedRules.map((rule) => {
              const isCurrent = rule.year === currentYear;
              return (
                <tr
                  key={rule.identifier}
                  className={`border-b border-border-default last:border-b-0 ${
                    isCurrent
                      ? 'bg-primary-50 dark:bg-primary-500/10 font-semibold'
                      : 'hover:bg-surface-page dark:hover:bg-surface-card'
                  }`}
                >
                  <td className="px-4 py-3">
                    <span
                      className={
                        isCurrent
                          ? 'text-primary-500 dark:text-primary-300'
                          : 'text-text-primary'
                      }
                    >
                      {rule.year}
                      {isCurrent && (
                        <span className="ml-2 text-xs px-1.5 py-0.5 rounded bg-primary-500 text-white font-normal">
                          Current
                        </span>
                      )}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-text-secondary">
                    {rule.propertyCategory}
                    {rule.sector && (
                      <span className="block text-xs text-text-muted">
                        {rule.sector}
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-text-primary">
                    {rule.maxIncreasePercentage != null
                      ? `${rule.maxIncreasePercentage}%`
                      : '-'}
                    {rule.indexName && (
                      <span className="block text-xs text-text-muted">
                        {rule.indexName}
                        {rule.indexValue != null && `: ${rule.indexValue}`}
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-text-secondary">
                    {humanizeEnum(rule.maxIncreaseType)}
                  </td>
                  <td className="px-4 py-3 text-text-secondary">
                    {rule.effectiveDate ?? '-'}
                  </td>
                  <td className="px-4 py-3 text-text-secondary">
                    {rule.noticePeriodDays != null
                      ? t('rentRegulations.days', {
                          count: rule.noticePeriodDays,
                        })
                      : '-'}
                  </td>
                  <td className="px-4 py-3">
                    {rule.sourceUrl ? (
                      <a
                        href={rule.sourceUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1 text-primary-500 dark:text-primary-300 hover:underline"
                      >
                        <ExternalLink className="h-3.5 w-3.5" />
                        Source
                      </a>
                    ) : (
                      <span className="text-text-muted">-</span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {sortedRules.some((r) => r.notes || r.additionalConditions) && (
        <div className="border-t border-border-default p-4">
          <h4 className="text-xs font-semibold uppercase tracking-wider text-text-muted mb-2">
            Notes
          </h4>
          <div className="space-y-1">
            {sortedRules
              .filter((r) => r.notes || r.additionalConditions)
              .map((r) => (
                <div key={r.identifier} className="text-xs text-text-secondary">
                  <span className="font-medium">{r.year}:</span>{' '}
                  {r.additionalConditions ? (
                    <RichTextDisplay
                      content={r.additionalConditions}
                      className="inline text-xs [&>*]:inline"
                    />
                  ) : (
                    r.notes
                  )}
                </div>
              ))}
          </div>
        </div>
      )}
    </div>
  );
};
