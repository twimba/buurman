import { useState } from "react";
import {
  NavLink,
  Outlet,
  useLocation,
  useSearchParams,
} from "react-router-dom";
import {
  LayoutDashboard,
  Users,
  UserCog,
  Smile,
  Bell,
  MessageSquare,
  Flag,
  Ticket,
  Timer,
  ScrollText,
  Monitor,
  LogOut,
  Shield,
  ChevronsLeft,
  ChevronsRight,
  ChevronDown,
  Loader2,
  ExternalLink,
} from "lucide-react";
import { SidebarTooltip } from "@buurman/ui";
import { useAuth } from "../contexts/AuthContext";
import {
  GrafanaIcon,
  KeycloakIcon,
  MailpitIcon,
  PrometheusIcon,
  TraefikIcon,
  SeaweedFSIcon,
  TwilioIcon,
  SendGridIcon,
  AwsIcon,
  FlagsmithIcon,
  HetznerIcon,
} from "./ToolIcons";
import { useGrafanaDashboards } from "../hooks/useGrafanaDashboards";

const STORAGE_KEY = "buurman-backoffice-sidebar-collapsed";

const navigation = [
  { name: "Dashboard", href: "/dashboard", icon: LayoutDashboard },
  { name: "Teams", href: "/teams", icon: Users },
  { name: "Users", href: "/users", icon: UserCog },
  { name: "Notifications", href: "/notifications", icon: Bell },
  { name: "SMS Policy", href: "/sms-policy", icon: MessageSquare },
  { name: "Invitations", href: "/registration-invitations", icon: Ticket },
  { name: "Feature Flags", href: "/feature-flags", icon: Flag },
];

const adminNavigation = [
  { name: "Buurmies", href: "/buurmies", icon: Smile },
  { name: "Scheduler", href: "/scheduler", icon: Timer },
  { name: "Loggers", href: "/loggers", icon: ScrollText },
  {
    name: "System",
    href: "/system",
    icon: Monitor,
    role: "BACKOFFICE_SYSTEM" as const,
  },
];

const isLocalEnv = () => window.location.hostname.includes("local.buurman.io");

type ToolItem = {
  name: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  external?: boolean;
};

const getTools = (): ToolItem[] => {
  const local = isLocalEnv();
  return [
    local
      ? { name: "Flagsmith", href: "/tools/flagsmith", icon: FlagsmithIcon }
      : {
          name: "Flagsmith",
          href: "https://app.flagsmith.com/project/34353/environment/QjT99rGBHX7Q8FP8538yZb/features",
          icon: FlagsmithIcon,
          external: true,
        },
    { name: "Keycloak", href: "/tools/keycloak", icon: KeycloakIcon },
    { name: "Prometheus", href: "/tools/prometheus", icon: PrometheusIcon },
    ...(local
      ? [{ name: "Traefik", href: "/tools/traefik", icon: TraefikIcon }]
      : []),
    ...(local
      ? [{ name: "Mailpit", href: "/tools/mailpit", icon: MailpitIcon }]
      : [
          { name: "Twilio", href: "/tools/twilio", icon: TwilioIcon },
          {
            name: "SendGrid",
            href: "https://app.sendgrid.com",
            icon: SendGridIcon,
            external: true,
          },
        ]),

    ...(local
      ? [{ name: "SeaweedFS", href: "/tools/seaweedfs", icon: SeaweedFSIcon }]
      : [
          {
            name: "AWS S3",
            href: "https://console.aws.amazon.com/s3",
            icon: AwsIcon,
            external: true,
          },
        ]),
    {
      name: "Hetzner",
      href: "https://console.hetzner.cloud/projects",
      icon: HetznerIcon,
      external: true,
    },
  ];
};

const navLinkClass = (isActive: boolean, collapsed: boolean) => `
  flex items-center gap-3 px-3 py-2.5 rounded-lg
  transition-all duration-200
  ${collapsed ? "justify-center" : ""}
  ${
    isActive
      ? "bg-[#5c7cfa]/10 text-[#4263eb] dark:text-[#91a7ff] font-semibold border-l-2 border-[#5c7cfa] dark:border-[#748ffc]"
      : "text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#5c7cfa]/5 hover:text-[#1a1d2e] dark:hover:text-[#eef0f6]"
  }
`;

const toolLinkClass = (isActive: boolean, collapsed: boolean) => `
  flex items-center gap-2.5 px-3 py-2 rounded-lg text-[13px]
  transition-all duration-200
  ${collapsed ? "justify-center" : ""}
  ${
    isActive
      ? "bg-[#5c7cfa]/10 text-[#4263eb] dark:text-[#91a7ff] font-semibold border-l-2 border-[#5c7cfa] dark:border-[#748ffc]"
      : "text-[#6b7194] dark:text-[#8b90a8] hover:bg-[#5c7cfa]/5 hover:text-[#3d4463] dark:hover:text-[#c4c8db]"
  }
`;

const dashboardLinkClass = (isActive: boolean) => `
  flex items-center gap-2 pl-9 pr-3 py-1.5 rounded-lg text-[12px]
  transition-all duration-200
  ${
    isActive
      ? "bg-[#5c7cfa]/10 text-[#4263eb] dark:text-[#91a7ff] font-semibold"
      : "text-[#6b7194] dark:text-[#8b90a8] hover:bg-[#5c7cfa]/5 hover:text-[#3d4463] dark:hover:text-[#c4c8db]"
  }
`;

export const Layout = () => {
  const { logout, keycloak } = useAuth();
  const userEmail = keycloak.tokenParsed?.email || "Admin";
  const userRoles =
    (keycloak.tokenParsed?.realm_access as { roles?: string[] })?.roles ?? [];
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const isToolRoute = location.pathname.startsWith("/tools/");
  const isGrafanaRoute = location.pathname === "/tools/grafana";
  const { data: dashboards, isLoading: dashboardsLoading } =
    useGrafanaDashboards();

  const [grafanaOpen, setGrafanaOpen] = useState(isGrafanaRoute);

  const [collapsed, setCollapsed] = useState(() => {
    try {
      return localStorage.getItem(STORAGE_KEY) === "true";
    } catch {
      return false;
    }
  });

  const toggleCollapsed = () => {
    const next = !collapsed;
    setCollapsed(next);
    try {
      localStorage.setItem(STORAGE_KEY, String(next));
    } catch {
      /* noop */
    }
  };

  const sidebarWidth = collapsed ? "w-16" : "w-64";
  const mainMargin = collapsed ? "ml-16" : "ml-64";

  return (
    <div
      className="flex overflow-hidden bg-[#f8f9fc] dark:bg-[#0c0d14]"
      style={{
        height: "calc(100vh - var(--env-banner-height, 0px))",
        marginTop: "var(--env-banner-height, 0px)",
      }}
    >
      {/* Sidebar — subtle blue-tinted background to distinguish from the app */}
      <aside
        className={`fixed left-0 ${sidebarWidth} z-40 transition-all duration-300 ease-in-out`}
        style={{
          top: "var(--env-banner-height, 0px)",
          height: "calc(100vh - var(--env-banner-height, 0px))",
          background: "var(--bo-sidebar-bg)",
          borderRight: "1px solid var(--bo-sidebar-border)",
        }}
      >
        <style>{`
          :root {
            --bo-sidebar-bg: #e4e8f4;
            --bo-sidebar-border: #cdd3e6;
          }
          .dark {
            --bo-sidebar-bg: #12152a;
            --bo-sidebar-border: #1c2040;
          }
          @media (prefers-color-scheme: dark) {
            :root:not(.light) {
              --bo-sidebar-bg: #111425;
              --bo-sidebar-border: #1c2040;
            }
          }
        `}</style>
        <div className="flex flex-col h-full overflow-hidden">
          {/* Logo / Title */}
          <div
            className="flex items-center h-16 border-b border-[#cdd3e6] dark:border-[#1c2040] px-3"
            style={{ justifyContent: "space-between" }}
          >
            <div className="flex items-center gap-2.5" style={{ minWidth: 0 }}>
              <div className="h-8 w-8 rounded-lg bg-gradient-to-br from-[#5c7cfa] to-[#4263eb] flex items-center justify-center flex-shrink-0">
                <Shield className="h-4.5 w-4.5 text-white" />
              </div>
              {!collapsed && (
                <div className="flex flex-col" style={{ minWidth: 0 }}>
                  <span className="text-sm font-bold text-[#1a1d2e] dark:text-[#eef0f6] leading-tight">
                    Buurman
                  </span>
                  <span className="text-[10px] font-semibold uppercase tracking-widest text-[#5c7cfa] dark:text-[#748ffc] leading-tight">
                    Backoffice
                  </span>
                </div>
              )}
            </div>
            <button
              onClick={toggleCollapsed}
              className="flex-shrink-0 p-1 rounded-md text-[#9ca0b8] dark:text-[#5c6180] hover:text-[#6b7194] dark:hover:text-[#8b90a8] hover:bg-[#5c7cfa]/5 transition-colors"
              title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
            >
              {collapsed ? (
                <ChevronsRight className="h-4 w-4" />
              ) : (
                <ChevronsLeft className="h-4 w-4" />
              )}
            </button>
          </div>

          {/* Navigation */}
          <nav className="flex-1 overflow-y-auto py-4 px-2">
            <ul className="space-y-1">
              {navigation.map((item) => (
                <li key={item.name}>
                  <SidebarTooltip label={item.name} show={collapsed}>
                    <NavLink
                      to={item.href}
                      className={({ isActive }) =>
                        navLinkClass(isActive, collapsed)
                      }
                    >
                      <item.icon className="h-5 w-5 flex-shrink-0" />
                      {!collapsed && (
                        <span className="truncate">{item.name}</span>
                      )}
                    </NavLink>
                  </SidebarTooltip>
                </li>
              ))}
            </ul>

            {/* Administration Section */}
            <div className="mt-6 pt-4 border-t border-[#cdd3e6] dark:border-[#1c2040]">
              {!collapsed && (
                <p className="px-3 mb-2 text-[10px] font-semibold uppercase tracking-widest text-[#9ca0b8] dark:text-[#5c6180]">
                  Administration
                </p>
              )}
              <ul className="space-y-1">
                {adminNavigation
                  .filter((item) => !item.role || userRoles.includes(item.role))
                  .map((item) => (
                    <li key={item.name}>
                      <SidebarTooltip label={item.name} show={collapsed}>
                        <NavLink
                          to={item.href}
                          className={({ isActive }) =>
                            navLinkClass(isActive, collapsed)
                          }
                        >
                          <item.icon className="h-5 w-5 flex-shrink-0" />
                          {!collapsed && (
                            <span className="truncate">{item.name}</span>
                          )}
                        </NavLink>
                      </SidebarTooltip>
                    </li>
                  ))}
              </ul>
            </div>

            {/* Tools Section */}
            <div className="mt-6 pt-4 border-t border-[#cdd3e6] dark:border-[#1c2040]">
              {!collapsed && (
                <p className="px-3 mb-2 text-[10px] font-semibold uppercase tracking-widest text-[#9ca0b8] dark:text-[#5c6180]">
                  Tools
                </p>
              )}
              <ul className="space-y-0.5">
                {/* Grafana — expandable with dashboards */}
                <li>
                  <SidebarTooltip label="Grafana" show={collapsed}>
                    <div className="flex items-center">
                      <NavLink
                        to="/tools/grafana"
                        className={({ isActive }) =>
                          toolLinkClass(isActive, collapsed) + " flex-1"
                        }
                      >
                        <GrafanaIcon className="h-4 w-4 flex-shrink-0" />
                        {!collapsed && (
                          <span className="truncate">Grafana</span>
                        )}
                      </NavLink>
                      {!collapsed && dashboards && dashboards.length > 0 && (
                        <button
                          onClick={() => setGrafanaOpen(!grafanaOpen)}
                          className="p-1 mr-1 rounded text-[#9ca0b8] dark:text-[#5c6180] hover:text-[#6b7194] dark:hover:text-[#8b90a8] hover:bg-[#5c7cfa]/5 transition-colors"
                        >
                          <ChevronDown
                            className={`h-3.5 w-3.5 transition-transform duration-200 ${grafanaOpen ? "rotate-180" : ""}`}
                          />
                        </button>
                      )}
                      {!collapsed && dashboardsLoading && (
                        <Loader2 className="h-3 w-3 mr-2 animate-spin text-[#9ca0b8]" />
                      )}
                    </div>
                  </SidebarTooltip>
                  {/* Dashboard sub-items */}
                  {!collapsed &&
                    grafanaOpen &&
                    dashboards &&
                    dashboards.length > 0 && (
                      <ul className="mt-0.5 space-y-0.5">
                        {dashboards.map((db) => {
                          const dbPath = `/tools/grafana?path=${encodeURIComponent(db.url)}`;
                          const currentPath = searchParams.get("path");
                          const isActive =
                            isGrafanaRoute && currentPath === db.url;
                          return (
                            <li key={db.uid}>
                              <NavLink
                                to={dbPath}
                                className={() => dashboardLinkClass(isActive)}
                              >
                                <span className="truncate">{db.title}</span>
                              </NavLink>
                            </li>
                          );
                        })}
                      </ul>
                    )}
                </li>
                {/* Other tools */}
                {getTools().map((item) => (
                  <li key={item.name}>
                    <SidebarTooltip label={item.name} show={collapsed}>
                      {item.external ? (
                        <a
                          href={item.href}
                          target="_blank"
                          rel="noopener noreferrer"
                          className={toolLinkClass(false, collapsed)}
                        >
                          <item.icon className="h-4 w-4 flex-shrink-0" />
                          {!collapsed && (
                            <>
                              <span className="truncate">{item.name}</span>
                              <ExternalLink className="h-3 w-3 ml-auto flex-shrink-0 opacity-50" />
                            </>
                          )}
                        </a>
                      ) : (
                        <NavLink
                          to={item.href}
                          className={({ isActive }) =>
                            toolLinkClass(isActive, collapsed)
                          }
                        >
                          <item.icon className="h-4 w-4 flex-shrink-0" />
                          {!collapsed && (
                            <span className="truncate">{item.name}</span>
                          )}
                        </NavLink>
                      )}
                    </SidebarTooltip>
                  </li>
                ))}
              </ul>
            </div>
          </nav>

          {/* Footer */}
          <div className="border-t border-[#cdd3e6] dark:border-[#1c2040] p-2 space-y-1">
            {/* User email */}
            {!collapsed && (
              <div className="px-3 py-2 text-xs text-[#6b7194] dark:text-[#8b90a8] truncate">
                {userEmail}
              </div>
            )}
            {/* Logout */}
            <SidebarTooltip label="Logout" show={collapsed}>
              <button
                onClick={logout}
                className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-red-600 dark:text-red-400 hover:bg-red-50/50 dark:hover:bg-red-900/20 transition-all duration-200 ${collapsed ? "justify-center" : ""}`}
              >
                <LogOut className="h-5 w-5 flex-shrink-0" />
                {!collapsed && <span className="truncate">Logout</span>}
              </button>
            </SidebarTooltip>
          </div>
        </div>
      </aside>

      {/* Main content */}
      <main
        className={`flex-1 overflow-auto ${mainMargin} transition-all duration-300 ease-in-out`}
        style={
          isToolRoute ? { display: "flex", flexDirection: "column" } : undefined
        }
      >
        {isToolRoute ? (
          <div
            style={{
              flex: 1,
              display: "flex",
              flexDirection: "column",
              minHeight: 0,
            }}
          >
            <Outlet />
          </div>
        ) : (
          <div className="p-4 lg:p-8">
            <Outlet />
          </div>
        )}
      </main>
    </div>
  );
};
