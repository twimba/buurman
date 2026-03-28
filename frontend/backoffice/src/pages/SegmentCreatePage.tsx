import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  Save,
  Plus,
  Trash2,
  Loader2,
  ExternalLink,
} from "lucide-react";
import { useCreateSegment } from "../hooks/useSegments";
import type { SegmentCondition } from "../api/segments";

const ATTRIBUTES = [
  { value: "is_demo", label: "Is Demo", type: "boolean" },
  { value: "is_owner", label: "Is Owner", type: "boolean" },
  { value: "is_team", label: "Is Team", type: "boolean" },
  { value: "is_user", label: "Is User", type: "boolean" },
  { value: "role", label: "Role", type: "role" },
  { value: "email", label: "Email", type: "string" },
  { value: "email_verified", label: "Email Verified", type: "boolean" },
  { value: "property_count", label: "Property Count", type: "number" },
  { value: "member_count", label: "Member Count", type: "number" },
  { value: "team_age_days", label: "Team Age (days)", type: "number" },
  { value: "contract_count", label: "Contract Count", type: "number" },
  { value: "contact_count", label: "Contact Count", type: "number" },
  { value: "photo_count", label: "Photo Count", type: "number" },
  { value: "document_count", label: "Document Count", type: "number" },
  { value: "expense_count", label: "Expense Count", type: "number" },
  { value: "payment_count", label: "Payment Count", type: "number" },
  {
    value: "calendar_feed_count",
    label: "Calendar Feed Count",
    type: "number",
  },
];

const OPERATORS = [
  { value: "eq", label: "equals" },
  { value: "neq", label: "not equals" },
  { value: "in", label: "in" },
  { value: "not_in", label: "not in" },
  { value: "gt", label: ">" },
  { value: "gte", label: ">=" },
  { value: "lt", label: "<" },
  { value: "lte", label: "<=" },
  { value: "contains", label: "contains" },
  { value: "not_contains", label: "does not contain" },
  { value: "starts_with", label: "starts with" },
  { value: "ends_with", label: "ends with" },
  { value: "regex", label: "matches regex" },
];

const ROLE_VALUES = ["TEAM_ADMIN", "TEAM_EDITOR", "TEAM_VIEWER"];

export const SegmentCreatePage = () => {
  const navigate = useNavigate();
  const createSegment = useCreateSegment();

  const [key, setKey] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState(0);
  const [conditions, setConditions] = useState<SegmentCondition[]>([]);
  const [error, setError] = useState<string | null>(null);

  const handleSave = () => {
    setError(null);
    if (!key.trim() || !name.trim()) {
      setError("Key and name are required");
      return;
    }

    createSegment.mutate(
      {
        key: key.trim(),
        name: name.trim(),
        description: description.trim() || undefined,
        priority,
        conditions,
      },
      {
        onSuccess: (data) => navigate(`/segments/${data.key}`),
        onError: (err: unknown) => {
          if (err && typeof err === "object" && "response" in err) {
            const axiosErr = err as {
              response?: { data?: { detail?: string } };
            };
            setError(
              axiosErr.response?.data?.detail ?? "Failed to create segment",
            );
          } else {
            setError("Failed to create segment");
          }
        },
      },
    );
  };

  const addCondition = () => {
    setConditions([
      ...conditions,
      { attribute: "is_demo", operator: "eq", value: "true" },
    ]);
  };

  const removeCondition = (index: number) => {
    setConditions(conditions.filter((_, i) => i !== index));
  };

  const updateCondition = (
    index: number,
    field: keyof SegmentCondition,
    value: string,
  ) => {
    const updated = [...conditions];
    updated[index] = { ...updated[index], [field]: value };

    if (field === "attribute") {
      const newType = ATTRIBUTES.find((a) => a.value === value)?.type;
      if (newType === "boolean") {
        updated[index].value = "true";
        updated[index].operator = "eq";
      } else if (newType === "role") {
        updated[index].value = "TEAM_ADMIN";
        updated[index].operator = "eq";
      } else if (newType === "number") {
        updated[index].value = "0";
        updated[index].operator = "gte";
      } else {
        // string
        updated[index].value = "";
        updated[index].operator = "eq";
      }
    }

    setConditions(updated);
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate("/segments")}
            className="p-1.5 rounded-md text-text-muted hover:text-text-primary hover:bg-surface-inset transition-colors"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <h1 className="text-2xl font-bold text-text-primary">New Segment</h1>
        </div>
        <button
          onClick={handleSave}
          disabled={createSegment.isPending}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-primary-500 text-white text-sm font-medium hover:bg-primary-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          {createSegment.isPending ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <Save className="h-4 w-4" />
          )}
          Create
        </button>
      </div>

      {error && (
        <div className="p-3 rounded-lg bg-error-bg border border-error-border text-sm text-error-text">
          {error}
        </div>
      )}

      {/* Metadata */}
      <section className="bg-surface-card rounded-lg border border-border-default p-6 space-y-4">
        <h2 className="text-sm font-semibold text-text-primary">Details</h2>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-medium text-text-secondary mb-1">
              Key <span className="text-error-text">*</span>
            </label>
            <input
              type="text"
              value={key}
              onChange={(e) =>
                setKey(e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, "_"))
              }
              placeholder="my_segment"
              className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-sm font-mono text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30 focus:border-primary-500 transition-all"
            />
            <p className="text-[11px] text-text-muted mt-1">
              Unique identifier, lowercase with underscores
            </p>
          </div>
          <div>
            <label className="block text-xs font-medium text-text-secondary mb-1">
              Name <span className="text-error-text">*</span>
            </label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="My Segment"
              className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30 focus:border-primary-500 transition-all"
            />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-medium text-text-secondary mb-1">
              Description
            </label>
            <input
              type="text"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Optional description"
              className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30 focus:border-primary-500 transition-all"
            />
          </div>
          <div>
            <label className="block text-xs font-medium text-text-secondary mb-1">
              Priority
            </label>
            <input
              type="number"
              value={priority}
              onChange={(e) => setPriority(parseInt(e.target.value) || 0)}
              className="w-full px-3 py-2 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30 focus:border-primary-500 transition-all"
            />
            <p className="text-[11px] text-text-muted mt-1">
              Lower number = higher precedence
            </p>
          </div>
        </div>
      </section>

      {/* Conditions */}
      <section className="bg-surface-card rounded-lg border border-border-default p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-text-primary">
            Conditions
          </h2>
          <button
            onClick={addCondition}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium text-primary-600 hover:bg-primary-50 transition-colors"
          >
            <Plus className="h-3.5 w-3.5" />
            Add Condition
          </button>
        </div>
        <p className="text-xs text-text-muted">
          All conditions must match (AND logic). For OR logic, create separate
          segments.
        </p>
        {conditions.length > 0 ? (
          <div className="space-y-3">
            {conditions.map((condition, index) => (
              <div key={index} className="space-y-2">
                <div className="flex items-center gap-3 p-3 rounded-lg bg-surface-inset border border-border-default">
                  <select
                    value={condition.attribute}
                    onChange={(e) =>
                      updateCondition(index, "attribute", e.target.value)
                    }
                    className="px-3 py-1.5 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30"
                  >
                    {ATTRIBUTES.map((a) => (
                      <option key={a.value} value={a.value}>
                        {a.label}
                      </option>
                    ))}
                  </select>
                  <select
                    value={condition.operator}
                    onChange={(e) =>
                      updateCondition(index, "operator", e.target.value)
                    }
                    className="px-3 py-1.5 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30"
                  >
                    {OPERATORS.filter((op) => {
                      const attrType = ATTRIBUTES.find(
                        (a) => a.value === condition.attribute,
                      )?.type;
                      if (attrType === "boolean") {
                        return op.value === "eq" || op.value === "neq";
                      }
                      if (attrType === "string") {
                        return [
                          "eq",
                          "neq",
                          "in",
                          "not_in",
                          "contains",
                          "not_contains",
                          "starts_with",
                          "ends_with",
                          "regex",
                        ].includes(op.value);
                      }
                      if (attrType === "role") {
                        return ["eq", "neq", "in", "not_in"].includes(op.value);
                      }
                      // number type: all except string operators
                      return ![
                        "contains",
                        "not_contains",
                        "starts_with",
                        "ends_with",
                      ].includes(op.value);
                    }).map((op) => (
                      <option key={op.value} value={op.value}>
                        {op.label}
                      </option>
                    ))}
                  </select>
                  {(() => {
                    const attrType = ATTRIBUTES.find(
                      (a) => a.value === condition.attribute,
                    )?.type;
                    if (attrType === "boolean") {
                      return (
                        <select
                          value={condition.value}
                          onChange={(e) =>
                            updateCondition(index, "value", e.target.value)
                          }
                          className="px-3 py-1.5 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30"
                        >
                          <option value="true">true</option>
                          <option value="false">false</option>
                        </select>
                      );
                    }
                    if (attrType === "role") {
                      return condition.operator === "in" ||
                        condition.operator === "not_in" ? (
                        <input
                          type="text"
                          value={condition.value}
                          onChange={(e) =>
                            updateCondition(index, "value", e.target.value)
                          }
                          placeholder="TEAM_ADMIN,TEAM_EDITOR"
                          className="flex-1 px-3 py-1.5 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30"
                        />
                      ) : (
                        <select
                          value={condition.value}
                          onChange={(e) =>
                            updateCondition(index, "value", e.target.value)
                          }
                          className="px-3 py-1.5 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30"
                        >
                          {ROLE_VALUES.map((r) => (
                            <option key={r} value={r}>
                              {r}
                            </option>
                          ))}
                        </select>
                      );
                    }
                    if (attrType === "number") {
                      return (
                        <input
                          type="number"
                          value={condition.value}
                          onChange={(e) =>
                            updateCondition(index, "value", e.target.value)
                          }
                          className="flex-1 px-3 py-1.5 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30"
                        />
                      );
                    }
                    // string type
                    return (
                      <input
                        type="text"
                        value={condition.value}
                        onChange={(e) =>
                          updateCondition(index, "value", e.target.value)
                        }
                        placeholder={
                          condition.attribute === "email"
                            ? "user@buurman.io"
                            : ""
                        }
                        className="flex-1 px-3 py-1.5 rounded-lg border border-border-default bg-surface-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-500/30"
                      />
                    );
                  })()}
                  <button
                    onClick={() => removeCondition(index)}
                    className="p-1.5 rounded-md text-text-muted hover:text-error-text hover:bg-error-bg transition-colors"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
                {condition.operator === "regex" && (
                  <div className="ml-3 bg-surface-inset border border-border-subtle rounded-lg p-3 text-xs text-text-muted space-y-1.5">
                    <p className="font-medium text-text-secondary">
                      Regex Help
                    </p>
                    <p>
                      Matches the <span className="font-medium">entire</span>{" "}
                      value. Use{" "}
                      <code className="bg-surface-card px-1 rounded">
                        .*pattern.*
                      </code>{" "}
                      for partial match.
                    </p>
                    <div className="flex flex-wrap gap-x-4 gap-y-1">
                      <span>
                        <code className="bg-surface-card px-1 rounded">
                          .*@gmail\.com
                        </code>{" "}
                        ends with @gmail.com
                      </span>
                      <span>
                        <code className="bg-surface-card px-1 rounded">
                          (admin|editor)
                        </code>{" "}
                        admin or editor
                      </span>
                      <span>
                        <code className="bg-surface-card px-1 rounded">
                          ^[A-Z].*
                        </code>{" "}
                        starts with uppercase
                      </span>
                    </div>
                    <a
                      href="https://regex101.com/"
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1 text-primary-600 hover:underline"
                    >
                      Test on regex101.com <ExternalLink className="h-3 w-3" />
                    </a>
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          <div className="text-center py-6 text-text-muted text-sm">
            No conditions defined. Add conditions to define segment membership.
          </div>
        )}
      </section>
    </div>
  );
};
