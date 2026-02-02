import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Legend,
  Tooltip,
} from 'recharts';

interface PropertyStatusChartProps {
  occupied: number;
  vacant: number;
  maintenance: number;
  unavailable: number;
}

export const PropertyStatusChart = ({
  occupied,
  vacant,
  maintenance,
  unavailable,
}: PropertyStatusChartProps) => {
  const data = [
    { name: 'Occupied', value: occupied, color: '#10b981' },
    { name: 'Vacant', value: vacant, color: '#f59e0b' },
    { name: 'Maintenance', value: maintenance, color: '#f97316' },
    { name: 'Unavailable', value: unavailable, color: '#6b7280' },
  ].filter((item) => item.value > 0);

  const total = occupied + vacant + maintenance + unavailable;

  if (total === 0) {
    return (
      <div className="flex items-center justify-center h-64 text-gray-500">
        <p>No properties to display</p>
      </div>
    );
  }

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const percentage = ((payload[0].value / total) * 100).toFixed(1);
      return (
        <div className="bg-white px-4 py-2 shadow-lg rounded-lg border border-gray-200">
          <p className="font-semibold">{payload[0].name}</p>
          <p className="text-sm text-gray-600">
            {payload[0].value}{' '}
            {payload[0].value === 1 ? 'property' : 'properties'} ({percentage}%)
          </p>
        </div>
      );
    }
    return null;
  };

  const renderLegend = (props: any) => {
    const { payload } = props;
    return (
      <div className="flex flex-wrap justify-center gap-4 mt-4">
        {payload.map((entry: any, index: number) => (
          <div key={`legend-${index}`} className="flex items-center gap-2">
            <div
              className="w-3 h-3 rounded-full"
              style={{ backgroundColor: entry.color }}
            />
            <span className="text-sm text-gray-700">
              {entry.value} ({((entry.payload.value / total) * 100).toFixed(0)}
              %)
            </span>
          </div>
        ))}
      </div>
    );
  };

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
          <Tooltip content={<CustomTooltip />} />
          <Legend content={renderLegend} />
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
};
