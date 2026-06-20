import { MetricCard } from '@buurman/ui';
import { TrendingUp, Receipt, Home } from 'lucide-react';

export function Basic() {
  return (
    <div className="grid grid-cols-2 gap-4">
      <MetricCard
        icon={<Home className="h-6 w-6" />}
        label="Properties"
        value="12"
        subtitle="3 with open issues"
      />
      <MetricCard
        icon={<Receipt className="h-6 w-6" />}
        iconBgVariant="accent"
        label="Annual costs"
        value="€48,200"
      />
    </div>
  );
}

export function WithTrend() {
  return (
    <div className="grid grid-cols-2 gap-4">
      <MetricCard
        icon={<TrendingUp className="h-6 w-6" />}
        iconBgVariant="success"
        label="Net worth"
        value="€1.24M"
        trend={{ direction: 'up', label: '+4.2%', sentiment: 'positive' }}
      />
      <MetricCard
        icon={<Receipt className="h-6 w-6" />}
        iconBgVariant="warning"
        label="Outstanding rent"
        value="€2,150"
        trend={{ direction: 'down', label: '-12%', sentiment: 'positive' }}
      />
    </div>
  );
}

export function Alert() {
  return (
    <MetricCard
      icon={<Receipt className="h-6 w-6" />}
      iconBgVariant="error"
      label="Overdue payments"
      value="5"
      subtitle="Action required"
      alert
      alertSeverity="critical"
    />
  );
}
