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
      <div className="bg-white px-4 py-2 shadow-lg rounded-lg border border-gray-200">
        <p className="font-semibold">{payload[0].name}</p>
        <p className="text-sm text-gray-600">
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
        { name: 'Vacant', value: vacant, color: '#f59e0b' },
        { name: 'Maintenance', value: maintenance, color: '#f97316' },
        { name: 'Unavailable', value: unavailable, color: '#6b7280' },
      ].filter((item) => item.value > 0),
    [occupied, vacant, maintenance, unavailable]
  );

  const total = occupied + vacant + maintenance + unavailable;

  const renderLegend = (props: LegendProps) => {
    const { payload } = props;
    if (!payload) return null;

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
              <span className="text-sm text-gray-700">
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
      <div className="flex items-center justify-center h-64 text-gray-500">
        <p>No properties to display</p>
      </div>
    );
  }

  return (
    <div className="w-full h-80">
      <ResponsiveContainer width="100%" height="100%">
        <PieChart>
          <Pie
            data={data}
            cx="50%"
            cy="45%"
            innerRadius={60}
            outerRadius={100}
            paddingAngle={2}
            dataKey="value"
            label={({ name, percent }) =>
              `${name}: ${((percent || 0) * 100).toFixed(0)}%`
            }
            labelLine={true}
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
