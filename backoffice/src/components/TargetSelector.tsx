import { useState, useMemo } from "react";
import { Search, X } from "lucide-react";
import { useTeams } from "../hooks/useTeams";
import { useUsers } from "../hooks/useUsers";

interface TargetSelectorProps {
  selected: string[];
  onChange: (selected: string[]) => void;
}

const LABEL_CLASS =
  "block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1";
const INPUT_CLASS =
  "w-full pl-9 pr-4 py-2 text-sm rounded-lg border border-[#e2e6f0] dark:border-[#2a2e3f] bg-white dark:bg-[#0c0d14] text-[#1a1d2e] dark:text-[#eef0f6] placeholder-[#9ca0b8] focus:outline-none focus:border-[#5c7cfa] focus:ring-2 focus:ring-[#5c7cfa]/20 transition-colors";
const PILL_CLASS =
  "inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium bg-[#5c7cfa]/10 text-[#4263eb] dark:bg-[#5c7cfa]/20 dark:text-[#91a7ff]";
const CHECK_ITEM_CLASS =
  "flex items-center gap-2 px-3 py-1.5 text-sm text-[#3d4463] dark:text-[#c4c8db] hover:bg-[#f8f9fc] dark:hover:bg-[#1a1d28] cursor-pointer rounded transition-colors";

export const TargetTeamSelector = ({ selected, onChange }: TargetSelectorProps) => {
  const [search, setSearch] = useState("");
  const { data } = useTeams({ search: search || undefined, size: 20 });

  const teams = useMemo(() => data?.content ?? [], [data]);

  const toggle = (identifier: string) => {
    if (selected.includes(identifier)) {
      onChange(selected.filter((id) => id !== identifier));
    } else {
      onChange([...selected, identifier]);
    }
  };

  return (
    <div>
      <label className={LABEL_CLASS}>Target Teams</label>

      {selected.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mb-2">
          {selected.map((id) => {
            const team = teams.find((t) => t.identifier === id);
            return (
              <span key={id} className={PILL_CLASS}>
                {team?.teamName ?? id}
                <button
                  type="button"
                  onClick={() => toggle(id)}
                  className="hover:text-red-500 transition-colors"
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            );
          })}
        </div>
      )}

      <div className="relative mb-2">
        <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-[#9ca0b8]" />
        <input
          type="text"
          placeholder="Search teams..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className={INPUT_CLASS}
        />
      </div>

      <div className="max-h-40 overflow-y-auto border border-[#e2e6f0] dark:border-[#2a2e3f] rounded-lg">
        {teams.length === 0 ? (
          <p className="px-3 py-2 text-sm text-[#9ca0b8]">No teams found.</p>
        ) : (
          teams.map((team) => (
            <label key={team.identifier} className={CHECK_ITEM_CLASS}>
              <input
                type="checkbox"
                checked={selected.includes(team.identifier)}
                onChange={() => toggle(team.identifier)}
                className="rounded border-[#cdd3e6] text-[#5c7cfa] focus:ring-[#5c7cfa]/20"
              />
              <span>{team.teamName}</span>
              <span className="text-[#9ca0b8] text-xs ml-auto">
                {team.memberCount} member{team.memberCount !== 1 ? "s" : ""}
              </span>
            </label>
          ))
        )}
      </div>
    </div>
  );
};

export const TargetUserSelector = ({ selected, onChange }: TargetSelectorProps) => {
  const [search, setSearch] = useState("");
  const { data } = useUsers({ search: search || undefined, size: 20 });

  const users = useMemo(() => data?.content ?? [], [data]);

  const toggle = (identifier: string) => {
    if (selected.includes(identifier)) {
      onChange(selected.filter((id) => id !== identifier));
    } else {
      onChange([...selected, identifier]);
    }
  };

  return (
    <div>
      <label className={LABEL_CLASS}>Target Users</label>

      {selected.length > 0 && (
        <div className="flex flex-wrap gap-1.5 mb-2">
          {selected.map((id) => {
            const user = users.find((u) => u.identifier === id);
            return (
              <span key={id} className={PILL_CLASS}>
                {user ? `${user.firstName} ${user.lastName}` : id}
                <button
                  type="button"
                  onClick={() => toggle(id)}
                  className="hover:text-red-500 transition-colors"
                >
                  <X className="h-3 w-3" />
                </button>
              </span>
            );
          })}
        </div>
      )}

      <div className="relative mb-2">
        <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-[#9ca0b8]" />
        <input
          type="text"
          placeholder="Search users..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className={INPUT_CLASS}
        />
      </div>

      <div className="max-h-40 overflow-y-auto border border-[#e2e6f0] dark:border-[#2a2e3f] rounded-lg">
        {users.length === 0 ? (
          <p className="px-3 py-2 text-sm text-[#9ca0b8]">No users found.</p>
        ) : (
          users.map((user) => (
            <label key={user.identifier} className={CHECK_ITEM_CLASS}>
              <input
                type="checkbox"
                checked={selected.includes(user.identifier)}
                onChange={() => toggle(user.identifier)}
                className="rounded border-[#cdd3e6] text-[#5c7cfa] focus:ring-[#5c7cfa]/20"
              />
              <span>
                {user.firstName} {user.lastName}
              </span>
              <span className="text-[#9ca0b8] text-xs ml-auto">{user.email}</span>
            </label>
          ))
        )}
      </div>
    </div>
  );
};
