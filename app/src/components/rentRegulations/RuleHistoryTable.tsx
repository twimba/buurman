import { ExternalLink } from 'lucide-react';
import type { RentRegulationRuleResponse } from '@/types/rentRegulation';

interface RuleHistoryTableProps {
  rules: RentRegulationRuleResponse[];
}

export const RuleHistoryTable = ({ rules }: RuleHistoryTableProps) => {
  const currentYear = new Date().getFullYear();
  const sortedRules = [...rules].sort((a, b) => b.year - a.year);

  if (sortedRules.length === 0) {
    return (
      <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] p-8 text-center">
        <p className="text-[#6b7194] dark:text-[#8b90a8]">
          No regulation rules available
        </p>
      </div>
    );
  }

  return (
    <div className="bg-white dark:bg-[#14161f] rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
              <th className="text-left px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                Year
              </th>
              <th className="text-left px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                Category
              </th>
              <th className="text-left px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                Max Increase
              </th>
              <th className="text-left px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                Type
              </th>
              <th className="text-left px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                Effective Date
              </th>
              <th className="text-left px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
                Notice Period
              </th>
              <th className="text-left px-4 py-3 font-semibold text-[#3d4463] dark:text-[#c4c8db]">
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
                  className={`border-b border-[#e2e6f0] dark:border-[#2a2e3f] last:border-b-0 ${
                    isCurrent
                      ? 'bg-[#f0f4ff] dark:bg-[#5c7cfa]/10 font-semibold'
                      : 'hover:bg-[#f8f9fc] dark:hover:bg-[#1a1c28]'
                  }`}
                >
                  <td className="px-4 py-3">
                    <span
                      className={
                        isCurrent
                          ? 'text-[#5c7cfa] dark:text-[#91a7ff]'
                          : 'text-[#1a1d2e] dark:text-[#eef0f6]'
                      }
                    >
                      {rule.year}
                      {isCurrent && (
                        <span className="ml-2 text-xs px-1.5 py-0.5 rounded bg-[#5c7cfa] text-white font-normal">
                          Current
                        </span>
                      )}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-[#3d4463] dark:text-[#c4c8db]">
                    {rule.propertyCategory}
                    {rule.sector && (
                      <span className="block text-xs text-[#9ca0b8] dark:text-[#5c6180]">
                        {rule.sector}
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-[#1a1d2e] dark:text-[#eef0f6]">
                    {rule.maxIncreasePercentage != null
                      ? `${rule.maxIncreasePercentage}%`
                      : '-'}
                    {rule.indexName && (
                      <span className="block text-xs text-[#9ca0b8] dark:text-[#5c6180]">
                        {rule.indexName}
                        {rule.indexValue != null && `: ${rule.indexValue}`}
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-[#3d4463] dark:text-[#c4c8db]">
                    {rule.maxIncreaseType}
                  </td>
                  <td className="px-4 py-3 text-[#3d4463] dark:text-[#c4c8db]">
                    {rule.effectiveDate ?? '-'}
                  </td>
                  <td className="px-4 py-3 text-[#3d4463] dark:text-[#c4c8db]">
                    {rule.noticePeriodDays != null
                      ? `${rule.noticePeriodDays} days`
                      : '-'}
                  </td>
                  <td className="px-4 py-3">
                    {rule.sourceUrl ? (
                      <a
                        href={rule.sourceUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center gap-1 text-[#5c7cfa] dark:text-[#91a7ff] hover:underline"
                      >
                        <ExternalLink className="h-3.5 w-3.5" />
                        Source
                      </a>
                    ) : (
                      <span className="text-[#9ca0b8] dark:text-[#5c6180]">
                        -
                      </span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {sortedRules.some((r) => r.notes || r.additionalConditions) && (
        <div className="border-t border-[#e2e6f0] dark:border-[#2a2e3f] p-4">
          <h4 className="text-xs font-semibold uppercase tracking-wider text-[#9ca0b8] dark:text-[#5c6180] mb-2">
            Notes
          </h4>
          <div className="space-y-1">
            {sortedRules
              .filter((r) => r.notes || r.additionalConditions)
              .map((r) => (
                <p
                  key={r.identifier}
                  className="text-xs text-[#6b7194] dark:text-[#8b90a8]"
                >
                  <span className="font-medium">{r.year}:</span>{' '}
                  {r.additionalConditions ?? r.notes}
                </p>
              ))}
          </div>
        </div>
      )}
    </div>
  );
};
