import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Legend,
  Tooltip,
} from 'recharts';

type LegendProps = {
  payload?: readonly unknown[];
};

interface PropertyStatusChartProps {
  occupied: number;
  selfOccupied: number;
  vacant: number;
  maintenance: number;
  unavailable: number;
}

const CustomTooltip = ({
  active,
  payload,
  total,
}: {
  active?: boolean;
  payload?: ReadonlyArray<{ value: number; name: string }>;
  total: number;
}) => {
  if (active && payload && payload.length) {
    const value = payload[0].value;
    const percentage = ((value / total) * 100).toFixed(1);
    return (
      <div className="bg-surface-card px-4 py-2 shadow-lg rounded-lg border border-border-default">
        <p className="font-semibold text-text-primary">{payload[0].name}</p>
        <p className="text-sm text-text-secondary">
          {value} {value === 1 ? 'property' : 'properties'} ({percentage}%)

        </p>
      </div>
    );
  }
  return null;
};

export const PropertyStatusChart = ({
  occupied,
  selfOccupied,
  vacant,
  maintenance,
  unavailable,
}: PropertyStatusChartProps) => {
  const { t } = useTranslation('properties');
  const data = useMemo(
    () =>
      [
        { name: t('statusChart.occupied'), value: occupied, color: '#059669' },
        { name: t('statusChart.selfOccupied'), value: selfOccupied, color: '#0284c7' },
        { name: t('statusChart.vacant'), value: vacant, color: '#fbbf24' },
        { name: t('statusChart.maintenance'), value: maintenance, color: '#f59e0b' },
        { name: t('statusChart.unavailable'), value: unavailable, color: '#78716c' },
      ].filter((item) => item.value > 0),
    [occupied, selfOccupied, vacant, maintenance, unavailable, t]
  );

  const total = occupied + selfOccupied + vacant + maintenance + unavailable;

  const renderLegend = (props: LegendProps) => {
    const { payload } = props;
    if (!payload) {
      return null;
    }

    return (
      <div className="flex flex-wrap justify-center gap-4 mt-4">
        {payload.map((item, index) => {
          const entry = item as {
            color?: string;
            value?: string;
            payload?: { value: number };
          };
          const value = entry.payload?.value ?? 0;
          return (
            <div key={`legend-${index}`} className="flex items-center gap-2">
              <div
                className="w-3 h-3 rounded-full"
                style={{ backgroundColor: entry.color }}
              />
              <span className="text-sm text-text-secondary">
                {entry.value} ({((value / total) * 100).toFixed(0)}%)
              </span>
            </div>
          );
        })}
      </div>
    );
  };

  if (total === 0) {
    return (
      <div className="flex items-center justify-center h-64 text-text-secondary">
        <p>{t('statusChart.noProperties')}</p>
      </div>
    );
  }

  return (
    <div className="w-full">
      <ResponsiveContainer width="100%" height={320}>
        <PieChart margin={{ top: 20, right: 20, bottom: 20, left: 20 }}>
          <Pie
            data={data}
            cx="50%"
            cy="50%"
            innerRadius={60}
            outerRadius={90}
            paddingAngle={2}
            dataKey="value"
            label={false}
          >
            {data.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={entry.color} />
            ))}
          </Pie>
          <Tooltip
            content={({ active, payload }) => (
              <CustomTooltip
                active={active}
                payload={
                  payload as ReadonlyArray<{ value: number; name: string }>
                }
                total={total}
              />
            )}
          />
          <Legend content={renderLegend} />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
};
