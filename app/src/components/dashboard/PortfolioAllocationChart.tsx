import { useState, useMemo } from 'react';
import {
  PieChart,
  Pie,
  Cell,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import type { AllocationData, AllocationSlice } from '@/types/portfolio';

const CATEGORY_COLORS: Record<string, string> = {
  RESIDENTIAL: '#3B82F6',
  COMMERCIAL: '#8B5CF6',
  INDUSTRIAL: '#F59E0B',
  AGRICULTURAL: '#10B981',
  MIXED_USE: '#EC4899',
};

const FALLBACK_COLORS = [
  '#3B82F6',
  '#8B5CF6',
  '#F59E0B',
  '#10B981',
  '#EC4899',
  '#06B6D4',
  '#F97316',
  '#6366F1',
  '#14B8A6',
  '#84CC16',
];

function getColor(label: string, index: number, isCategory: boolean): string {
  if (isCategory && CATEGORY_COLORS[label]) {
    return CATEGORY_COLORS[label];
  }
  return FALLBACK_COLORS[index % FALLBACK_COLORS.length];
}

interface PortfolioAllocationChartProps {
  data: AllocationData;
  isDark: boolean;
}

export const PortfolioAllocationChart = ({
  data,
  isDark,
}: PortfolioAllocationChartProps) => {
  const [view, setView] = useState<'category' | 'country'>('category');
  const slices: AllocationSlice[] =
    view === 'category' ? data.byCategory : data.byCountry;
  const isCategory = view === 'category';

  const tooltipStyle = useMemo(
    () => ({
      backgroundColor: isDark ? '#14161f' : '#fff',
      border: `1px solid ${isDark ? '#2a2e3f' : '#e2e6f0'}`,
      borderRadius: '8px',
      fontSize: '12px',
      color: isDark ? '#eef0f6' : '#1a1d2e',
    }),
    [isDark]
  );

  if (slices.length === 0) {
    return (
      <div className="flex items-center justify-center h-80 text-[#6b7194] dark:text-[#8b90a8] text-sm">
        No allocation data available
      </div>
    );
  }

  return (
    <div>
      <div className="flex gap-1 mb-4">
        <button
          onClick={() => setView('category')}
          className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${
            view === 'category'
              ? 'bg-[#5c7cfa] text-white'
              : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:hover:bg-[#3a3f54]'
          }`}
        >
          By Category
        </button>
        <button
          onClick={() => setView('country')}
          className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${
            view === 'country'
              ? 'bg-[#5c7cfa] text-white'
              : 'bg-[#f1f3f9] dark:bg-[#1e2130] text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#e8ecf4] dark:hover:bg-[#3a3f54]'
          }`}
        >
          By Country
        </button>
      </div>
      <div className="flex items-center gap-6">
        <ResponsiveContainer width="60%" height={240}>
          <PieChart>
            <Pie
              data={slices}
              dataKey="value"
              nameKey="label"
              cx="50%"
              cy="50%"
              innerRadius={60}
              outerRadius={100}
              paddingAngle={2}
            >
              {slices.map((entry, idx) => (
                <Cell
                  key={entry.label}
                  fill={getColor(entry.label, idx, isCategory)}
                />
              ))}
            </Pie>
            <Tooltip
              contentStyle={tooltipStyle}
              formatter={(value?: number | string, name?: string) => [
                `${Number(value ?? 0)} (${slices.find((s) => s.label === (name ?? ''))?.percentage.toFixed(1) ?? 0}%)`,
                name ?? '',
              ]}
            />
          </PieChart>
        </ResponsiveContainer>
        <div className="flex-1 space-y-2">
          {slices.map((slice, idx) => (
            <div key={slice.label} className="flex items-center gap-2 text-sm">
              <span
                className="inline-block w-3 h-3 rounded-full shrink-0"
                style={{
                  backgroundColor: getColor(slice.label, idx, isCategory),
                }}
              />
              <span className="text-[#1a1d2e] dark:text-[#eef0f6] truncate">
                {slice.label}
              </span>
              <span className="ml-auto text-[#6b7194] dark:text-[#8b90a8] tabular-nums">
                {slice.percentage.toFixed(1)}%
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
