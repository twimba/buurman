import { useQuery } from '@tanstack/react-query';
import { fetchGrafanaDashboards } from '../api/grafana';

export const useGrafanaDashboards = () => {
  return useQuery({
    queryKey: ['grafana-dashboards'],
    queryFn: fetchGrafanaDashboards,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });
};
