import { useState } from 'react';
import {
  NavLink,
  Outlet,
  useLocation,
  useSearchParams,
} from 'react-router-dom';
import {
  LayoutDashboard,
  Users,
  UserCog,
  UserCheck,
  Smile,
  Bell,
  MessageSquare,
  Radio,
  Flag,
  Layers,
  BookOpen,
  Ticket,
  Timer,
  Database,
  ScrollText,
  Monitor,
  LogOut,
  ChevronsLeft,
  ChevronsRight,
  ChevronDown,
  Loader2,
  ExternalLink,
  Shield,
} from 'lucide-react';
import { SidebarTooltip } from '@buurman/ui';
import { useAuth } from '../contexts/AuthContext';
import {
  GrafanaIcon,
  KeycloakIcon,
  MailpitIcon,
  PrometheusIcon,
  TraefikIcon,
  TwilioIcon,
  MailgunIcon,
  PostHogIcon,
  HetznerIcon,
  BetterStackIcon,
} from './ToolIcons';
import { useGrafanaDashboards } from '../hooks/useGrafanaDashboards';

const STORAGE_KEY = 'buurman-backoffice-sidebar-collapsed';

type NavItem = {
  name: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  indent?: boolean;
};

const navigation: NavItem[] = [
  { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard },
  { name: 'Teams', href: '/teams', icon: Users },
  { name: 'Users', href: '/users', icon: UserCog },
  {
    name: 'Impersonation',
    href: '/impersonation',
    icon: UserCheck,
    indent: true,
  },
  {
    name: 'Invitations',
    href: '/registration-invitations',
    icon: Ticket,
    indent: true,
  },
  { name: 'Notifications', href: '/notifications', icon: Bell },
  {
    name: 'SMS Policy',
    href: '/sms-policy',
    icon: MessageSquare,
    indent: true,
  },
  { name: 'Broadcasts', href: '/broadcasts', icon: Radio },
  { name: 'Rent Regulations', href: '/rent-regulations', icon: BookOpen },
  { name: 'Feature Flags', href: '/feature-flags', icon: Flag },
  { name: 'Segments', href: '/segments', icon: Layers, indent: true },
];

const adminNavigation = [
  { name: 'Buurmies', href: '/buurmies', icon: Smile },
  {
    name: 'System',
    href: '/system',
    icon: Monitor,
    role: 'BACKOFFICE_SYSTEM' as const,
  },
  { name: 'Scheduler', href: '/scheduler', icon: Timer, indent: true },
  { name: 'Caches', href: '/caches', icon: Database, indent: true },
  { name: 'Rate Limits', href: '/rate-limits', icon: Shield, indent: true },
  { name: 'Loggers', href: '/loggers', icon: ScrollText, indent: true },
];

const isLocalEnv = () => window.location.hostname.includes('local.buurman.io');

type ToolItem = {
  name: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  external?: boolean;
};

const getTools = (): ToolItem[] => {
  const local = isLocalEnv();
  return [
    { name: 'Keycloak', href: '/tools/keycloak', icon: KeycloakIcon },
    { name: 'Prometheus', href: '/tools/prometheus', icon: PrometheusIcon },
    ...(local
      ? [{ name: 'Traefik', href: '/tools/traefik', icon: TraefikIcon }]
      : []),
    ...(local
      ? [{ name: 'Mailpit', href: '/tools/mailpit', icon: MailpitIcon }]
      : [
          { name: 'Twilio', href: '/tools/twilio', icon: TwilioIcon },
          {
            name: 'Mailgun',
            href: 'https://app.mailgun.com',
            icon: MailgunIcon,
            external: true,
          },
          {
            name: 'PostHog',
            href: 'https://eu.posthog.com',
            icon: PostHogIcon,
            external: true,
          },
        ]),
    {
      name: 'Hetzner',
      href: 'https://console.hetzner.cloud/projects',
      icon: HetznerIcon,
      external: true,
    },
    {
      name: 'Better Stack',
      href: 'https://telemetry.betterstack.com/team/t505111/tail?s=1735995',
      icon: BetterStackIcon,
      external: true,
    },
  ];
};

const navLinkClass = (isActive: boolean, collapsed: boolean) => `
  flex items-center gap-3 px-3 py-2.5 rounded-lg
  transition-all duration-200
  ${collapsed ? 'justify-center' : ''}
  ${
    isActive
      ? 'bg-primary-50 text-primary-700 font-semibold'
      : 'text-text-secondary hover:bg-neutral-50 hover:text-text-primary'
  }
`;

const toolLinkClass = (isActive: boolean, collapsed: boolean) => `
  flex items-center gap-2.5 px-3 py-2 rounded-lg text-[13px]
  transition-all duration-200
  ${collapsed ? 'justify-center' : ''}
  ${
    isActive
      ? 'bg-primary-50 text-primary-700 font-semibold'
      : 'text-text-secondary hover:bg-neutral-50 hover:text-text-secondary'
  }
`;

const dashboardLinkClass = (isActive: boolean) => `
  flex items-center gap-2 pl-9 pr-3 py-1.5 rounded-lg text-[12px]
  transition-all duration-200
  ${
    isActive
      ? 'bg-primary-50 text-primary-700 font-semibold'
      : 'text-text-secondary hover:bg-neutral-50 hover:text-text-secondary'
  }
`;

export const Layout = () => {
  const { logout, keycloak } = useAuth();
  const userEmail = keycloak.tokenParsed?.email || 'Admin';
  const userRoles =
    (keycloak.tokenParsed?.realm_access as { roles?: string[] })?.roles ?? [];
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const isToolRoute = location.pathname.startsWith('/tools/');
  const isGrafanaRoute = location.pathname === '/tools/grafana';
  const { data: dashboards, isLoading: dashboardsLoading } =
    useGrafanaDashboards();

  const [grafanaOpen, setGrafanaOpen] = useState(isGrafanaRoute);

  const [collapsed, setCollapsed] = useState(() => {
    try {
      return localStorage.getItem(STORAGE_KEY) === 'true';
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

  const sidebarWidth = collapsed ? 'w-20' : 'w-64';
  const mainMargin = collapsed ? 'ml-20' : 'ml-64';

  return (
    <div
      className="theme-backoffice flex overflow-hidden bg-surface-page"
      style={{
        height: 'calc(100vh - var(--env-banner-height, 0px))',
        marginTop: 'var(--env-banner-height, 0px)',
      }}
    >
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-[100] focus:rounded-md focus:bg-surface-card focus:px-4 focus:py-2 focus:text-sm focus:font-medium focus:text-text-primary focus:shadow-lg focus-ring"
      >
        Skip to main content
      </a>
      {/* Sidebar — subtle blue-tinted background to distinguish from the app */}
      <aside
        className={`fixed left-0 ${sidebarWidth} z-40 transition-[width] duration-300 ease-in-out`}
        style={{
          top: 'var(--env-banner-height, 0px)',
          height: 'calc(100vh - var(--env-banner-height, 0px))',
          background: 'var(--color-surface-page)',
          borderRight: '1px solid var(--color-border-default)',
        }}
      >
        <div className="flex flex-col h-full overflow-hidden">
          {/* Logo / Title */}
          <div
            className="flex items-center h-16 border-b border-border-default px-3"
            style={{ justifyContent: 'space-between' }}
          >
            <div className="flex items-center gap-2.5" style={{ minWidth: 0 }}>
              <img
                src="/assets/logo/logo_square_no_text.png"
                alt="Buurman"
                className="h-9 w-9 rounded-lg flex-shrink-0"
              />
              {!collapsed && (
                <div className="flex flex-col" style={{ minWidth: 0 }}>
                  <span className="text-sm font-bold text-text-primary leading-tight">
                    Buurman
                  </span>
                  <span className="text-[10px] font-semibold uppercase tracking-widest text-primary-500 leading-tight">
                    Backoffice
                  </span>
                </div>
              )}
            </div>
            <button
              onClick={toggleCollapsed}
              className="flex-shrink-0 p-1 rounded-md text-text-muted hover:text-text-secondary hover:bg-primary-500/5 transition-colors"
              title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
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
                        item.indent
                          ? `flex items-center gap-2.5 ${collapsed ? 'px-3 py-2 justify-center' : 'pl-10 pr-3 py-2'} rounded-lg text-[13px] transition-all duration-200 ${
                              isActive
                                ? 'bg-primary-50 text-primary-700 font-semibold'
                                : 'text-text-secondary hover:bg-neutral-50 hover:text-text-primary'
                            }`
                          : navLinkClass(isActive, collapsed)
                      }
                    >
                      <item.icon
                        className={
                          item.indent
                            ? 'h-4 w-4 flex-shrink-0'
                            : 'h-5 w-5 flex-shrink-0'
                        }
                      />
                      {!collapsed && (
                        <span className="truncate">{item.name}</span>
                      )}
                    </NavLink>
                  </SidebarTooltip>
                </li>
              ))}
            </ul>

            {/* Administration Section */}
            <div className="mt-6 pt-4 border-t border-border-default">
              {!collapsed && (
                <p className="px-3 mb-2 text-[10px] font-semibold uppercase tracking-widest text-text-muted">
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
                            item.indent
                              ? `flex items-center gap-2.5 ${collapsed ? 'px-3 py-2 justify-center' : 'pl-10 pr-3 py-2'} rounded-lg text-[13px] transition-all duration-200 ${
                                  isActive
                                    ? 'bg-primary-50 text-primary-700 font-semibold'
                                    : 'text-text-secondary hover:bg-neutral-50 hover:text-text-primary'
                                }`
                              : navLinkClass(isActive, collapsed)
                          }
                        >
                          <item.icon
                            className={
                              item.indent
                                ? 'h-4 w-4 flex-shrink-0'
                                : 'h-5 w-5 flex-shrink-0'
                            }
                          />
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
            <div className="mt-6 pt-4 border-t border-border-default">
              {!collapsed && (
                <p className="px-3 mb-2 text-[10px] font-semibold uppercase tracking-widest text-text-muted">
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
                          toolLinkClass(isActive, collapsed) + ' flex-1'
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
                          className="p-1 mr-1 rounded text-text-muted hover:text-text-secondary hover:bg-primary-500/5 transition-colors"
                        >
                          <ChevronDown
                            className={`h-3.5 w-3.5 transition-transform duration-200 ${grafanaOpen ? 'rotate-180' : ''}`}
                          />
                        </button>
                      )}
                      {!collapsed && dashboardsLoading && (
                        <Loader2 className="h-3 w-3 mr-2 animate-spin text-text-muted" />
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
                          const currentPath = searchParams.get('path');
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
          <div className="border-t border-border-default p-2 space-y-1">
            {/* User email */}
            {!collapsed && (
              <div className="px-3 py-2 text-xs text-text-secondary truncate">
                {userEmail}
              </div>
            )}
            {/* Logout */}
            <SidebarTooltip label="Logout" show={collapsed}>
              <button
                onClick={logout}
                className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-error-text hover:bg-error-bg transition-all duration-200 ${collapsed ? 'justify-center' : ''}`}
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
        id="main-content"
        className={`flex-1 overflow-auto ${mainMargin} transition-[margin] duration-300 ease-in-out`}
        style={
          isToolRoute ? { display: 'flex', flexDirection: 'column' } : undefined
        }
      >
        {isToolRoute ? (
          <div
            style={{
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              minHeight: 0,
            }}
          >
            <Outlet />
          </div>
        ) : (
          <div className="max-w-screen-2xl mx-auto px-4 py-6 md:px-6 md:py-8 lg:px-8">
            <Outlet />
          </div>
        )}
      </main>
    </div>
  );
};
