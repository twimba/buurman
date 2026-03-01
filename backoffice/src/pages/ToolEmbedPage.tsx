import { useParams, useSearchParams } from "react-router-dom";
import { ExternalLink } from "lucide-react";

const isLocalEnv = () => window.location.hostname.includes("local.buurman.io");

const getToolUrl = (subdomain: string) => {
  const hostname = window.location.hostname;
  const base = hostname.replace(/^backoffice\./, "");
  return `${window.location.protocol}//${subdomain}.${base}`;
};

type ToolConfig = { name: string } & (
  | { subdomain: string; url?: never }
  | { url: string; subdomain?: never }
);

const getToolsConfig = (): Record<string, ToolConfig> => {
  const local = isLocalEnv();
  return {
    grafana: { name: "Grafana", subdomain: "grafana" },
    keycloak: { name: "Keycloak", subdomain: "keycloak" },
    prometheus: { name: "Prometheus", subdomain: "prometheus" },
    traefik: { name: "Traefik", subdomain: "traefik" },
    flagsmith: { name: "Flagsmith", subdomain: "flagsmith" },
    ...(local
      ? {
          mailpit: { name: "Mailpit", subdomain: "mailpit" },
          seaweedfs: { name: "SeaweedFS", subdomain: "seaweedfs-ui" },
        }
      : {
          twilio: { name: "Twilio", url: "https://console.twilio.com" },
        }),
  };
};

export const ToolEmbedPage = () => {
  const { toolKey } = useParams<{ toolKey: string }>();
  const [searchParams] = useSearchParams();
  const config = getToolsConfig();
  const tool = toolKey ? config[toolKey] : undefined;

  if (!tool) {
    return (
      <div className="flex items-center justify-center h-full">
        <p className="text-[#6b7194] dark:text-[#8b90a8]">Tool not found.</p>
      </div>
    );
  }

  const baseUrl = tool.subdomain ? getToolUrl(tool.subdomain) : tool.url;
  const path = searchParams.get("path");
  const url = path ? `${baseUrl}${path}?kiosk` : baseUrl;

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100%" }}>
      {/* Toolbar */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          flexShrink: 0,
        }}
        className="px-4 py-2 border-b border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#14161f]"
      >
        <span className="text-sm font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
          {tool.name}
        </span>
        <a
          href={url}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-[#5c7cfa] dark:text-[#91a7ff] bg-[#f0f4ff] dark:bg-[#5c7cfa]/10 rounded-lg hover:bg-[#e0e7ff] dark:hover:bg-[#5c7cfa]/20 transition-colors"
        >
          Open in new tab
          <ExternalLink className="h-3 w-3" />
        </a>
      </div>
      {/* Iframe */}
      <iframe
        src={url}
        title={tool.name}
        style={{ flex: 1, width: "100%", border: "none" }}
        sandbox="allow-same-origin allow-scripts allow-popups allow-forms allow-modals"
      />
    </div>
  );
};
