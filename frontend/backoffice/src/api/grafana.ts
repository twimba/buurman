export interface GrafanaDashboard {
  uid: string;
  title: string;
  url: string;
}

// Dashboards are statically provisioned from grafana/dashboards/ — no API call needed.
const GRAFANA_DASHBOARDS: GrafanaDashboard[] = [
  {
    uid: "buurman-backend-app",
    title: "Backend Application",
    url: "/d/buurman-backend-app",
  },
  { uid: "buurman-app", title: "App", url: "/d/buurman-app" },
  { uid: "buurman-database", title: "Database", url: "/d/buurman-database" },
  {
    uid: "buurman-business",
    title: "Business Metrics",
    url: "/d/buurman-business",
  },
  {
    uid: "buurman-auth",
    title: "Authentication & Security",
    url: "/d/buurman-auth",
  },
  {
    uid: "buurman-integrations",
    title: "Integrations",
    url: "/d/buurman-integrations",
  },
  {
    uid: "buurman-operations",
    title: "Operations",
    url: "/d/buurman-operations",
  },
  {
    uid: "hetzner-servers",
    title: "Hetzner Servers",
    url: "/d/hetzner-servers",
  },
];

export const fetchGrafanaDashboards = async (): Promise<GrafanaDashboard[]> => {
  return GRAFANA_DASHBOARDS;
};
