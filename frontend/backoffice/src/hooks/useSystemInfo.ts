import { useQuery } from "@tanstack/react-query";
import { systemApi } from "../api/system";

export const useSystemInfo = () => {
  return useQuery({
    queryKey: ["system-info"],
    queryFn: () => systemApi.info().then((res) => res.data),
    staleTime: 5 * 60 * 1000,
    refetchOnWindowFocus: false,
  });
};

interface AppBuildInfo {
  version: string;
  gitCommit: string;
  gitCommitFull?: string;
  gitBranch: string;
  buildTime: string;
}

const appBuildInfoUrl = `${window.location.protocol}//app.${window.location.hostname.replace(/^backoffice\./, "")}/build-info.json`;

export const useAppBuildInfo = () => {
  return useQuery({
    queryKey: ["app-build-info"],
    queryFn: async (): Promise<AppBuildInfo | null> => {
      try {
        const res = await fetch(appBuildInfoUrl);
        if (!res.ok) {
          return null;
        }
        return res.json();
      } catch {
        return null;
      }
    },
    staleTime: 5 * 60 * 1000,
    refetchOnWindowFocus: false,
  });
};
