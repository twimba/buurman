import { useMemo } from 'react';
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
      <div className="bg-white dark:bg-[#14161f] px-4 py-2 shadow-lg rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f]">
        <p className="font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
          {payload[0].name}
        </p>
        <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
          {value} {value === 1 ? 'property' : 'properties'} ({percentage}%)
        </p>
      </div>
    );
  }
  return null;
};

export const PropertyStatusChart = ({
  occupied,
  vacant,
  maintenance,
  unavailable,
}: PropertyStatusChartProps) => {
  const data = useMemo(
    () =>
      [
        { name: 'Occupied', value: occupied, color: '#10b981' },
        { name: 'Vacant', value: vacant, color: '#fcc419' },
        { name: 'Maintenance', value: maintenance, color: '#f59f00' },
        { name: 'Unavailable', value: unavailable, color: '#6b7194' },
      ].filter((item) => item.value > 0),
    [occupied, vacant, maintenance, unavailable]
  );

  const total = occupied + vacant + maintenance + unavailable;

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
              <span className="text-sm text-[#3d4463] dark:text-[#c4c8db]">
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
      <div className="flex items-center justify-center h-64 text-[#6b7194] dark:text-[#8b90a8]">
        <p>No properties to display</p>
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
            content={(props) => <CustomTooltip {...props} total={total} />}
          />
          <Legend content={renderLegend} />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
};
