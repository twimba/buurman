export interface GrafanaDashboard {
  uid: string;
  title: string;
  url: string;
}

const getGrafanaUrl = () => {
  const hostname = window.location.hostname;
  const base = hostname.replace(/^backoffice\./, "");
  return `${window.location.protocol}//grafana.${base}`;
};

export const fetchGrafanaDashboards = async (): Promise<GrafanaDashboard[]> => {
  const url = `${getGrafanaUrl()}/api/search?type=dash-db`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Grafana API error: ${res.status}`);
  const data = await res.json();
  return data.map((d: { uid: string; title: string; url: string }) => ({
    uid: d.uid,
    title: d.title,
    url: d.url,
  }));
};
