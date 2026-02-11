# Buurman Metrics Catalog

All custom application metrics use the `buurman_` prefix. Spring Boot auto-configured metrics (JVM, Tomcat, HikariCP, HTTP server) use their standard prefixes. This catalog is the single source of truth for all monitoring dashboards.

---

## 1. Backend Application Dashboard

### 1.1 HTTP Server Metrics (auto-configured by Spring Boot Actuator)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `http_server_requests_seconds` | Timer (histogram) | `method`, `uri`, `status`, `outcome`, `exception` | Request duration distribution | Request Rate, Latency Percentiles, Error Rate |
| `http_server_requests_seconds_count` | Counter | (same) | Total request count | Request Rate (rate()) |
| `http_server_requests_seconds_sum` | Counter | (same) | Total request time | Avg Latency |
| `http_server_requests_seconds_max` | Gauge | (same) | Max request time in window | Max Latency |

**Dashboard Panels:**
- **Request Rate** - `rate(http_server_requests_seconds_count[5m])` grouped by `uri`
- **Latency P50/P95/P99** - `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))`
- **Error Rate (4xx/5xx)** - `rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m])`
- **Top Endpoints by Latency** - table sorted by P95
- **Status Code Distribution** - pie chart by `status`

### 1.2 JVM Metrics (auto-configured by Micrometer)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `jvm_memory_used_bytes` | Gauge | `area` (heap/nonheap), `id` | Current memory usage | Memory Usage |
| `jvm_memory_max_bytes` | Gauge | `area`, `id` | Max memory | Memory Usage |
| `jvm_memory_committed_bytes` | Gauge | `area`, `id` | Committed memory | Memory Usage |
| `jvm_gc_pause_seconds` | Timer | `action`, `cause` | GC pause duration | GC Activity |
| `jvm_gc_pause_seconds_count` | Counter | `action`, `cause` | GC pause count | GC Rate |
| `jvm_threads_live_threads` | Gauge | - | Current live threads | Thread Usage |
| `jvm_threads_peak_threads` | Gauge | - | Peak thread count | Thread Usage |
| `jvm_threads_daemon_threads` | Gauge | - | Daemon thread count | Thread Usage |
| `jvm_threads_states_threads` | Gauge | `state` | Threads per state | Thread States |
| `jvm_classes_loaded_classes` | Gauge | - | Loaded classes | Class Loading |
| `jvm_buffer_memory_used_bytes` | Gauge | `id` | Buffer pool usage | Buffer Pools |
| `process_cpu_usage` | Gauge | - | Process CPU usage (0-1) | CPU Usage |
| `system_cpu_usage` | Gauge | - | System CPU usage (0-1) | CPU Usage |
| `process_uptime_seconds` | Gauge | - | Process uptime | Uptime stat |

**Dashboard Panels:**
- **Heap Memory Used vs Max** - timeseries with heap area
- **Non-Heap Memory** - timeseries
- **GC Pause Duration** - `rate(jvm_gc_pause_seconds_sum[5m]) / rate(jvm_gc_pause_seconds_count[5m])`
- **GC Rate** - `rate(jvm_gc_pause_seconds_count[5m])`
- **CPU Usage** - process + system overlaid
- **Thread Count** - live, peak, daemon stacked
- **Thread States** - stacked area by state
- **Uptime** - stat panel

### 1.3 Tomcat Metrics (auto-configured)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `tomcat_sessions_active_current_sessions` | Gauge | - | Active sessions | Sessions |
| `tomcat_sessions_active_max_sessions` | Gauge | - | Max active sessions | Sessions |
| `tomcat_threads_current_threads` | Gauge | `name` | Current Tomcat threads | Tomcat Threads |
| `tomcat_threads_busy_threads` | Gauge | `name` | Busy Tomcat threads | Tomcat Threads |
| `tomcat_threads_config_max_threads` | Gauge | `name` | Max configured threads | Tomcat Threads |

**Dashboard Panels:**
- **Tomcat Thread Pool** - current, busy, max
- **Active Sessions** - current vs max

### 1.4 Scheduler Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_scheduler_execution_seconds` | Timer | `job` | Scheduled job duration | Scheduler Duration |
| `buurman_scheduler_execution_total` | Counter | `job`, `result` (success/failure) | Scheduled job executions | Scheduler Runs |
| `buurman_scheduler_last_success_timestamp` | Gauge | `job` | Epoch of last successful run | Last Run |
| `buurman_scheduler_items_processed_total` | Counter | `job`, `item_type` | Items processed per run | Processing Volume |

**Jobs to instrument:**
- `contract_expiry_check` - NotificationSchedulerService.checkContractExpiry()
- `payment_reminder_check` - NotificationSchedulerService.checkPaymentReminders()

**Dashboard Panels:**
- **Scheduler Execution Timeline** - execution count over time per job
- **Scheduler Duration** - P95 per job
- **Success/Failure Rate** - stacked bar by result
- **Items Processed** - bar chart per run
- **Last Successful Run** - stat panel per job (with staleness alert)

---

## 2. Frontend Application Dashboard

Metrics observed from the backend HTTP layer for frontend API patterns.

### 2.1 API Call Patterns (derived from http_server_requests_seconds)

| Derived Query | Description | Panel |
|---------------|-------------|-------|
| `rate(http_server_requests_seconds_count{uri=~"/api/.*"}[5m])` | Total API call rate | API Call Rate |
| `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{uri=~"/api/.*"}[5m]))` | API P95 latency | API Latency |
| `rate(http_server_requests_seconds_count{uri=~"/api/.*", status=~"4.."}[5m])` | Client error rate | Client Errors |
| `rate(http_server_requests_seconds_count{uri=~"/api/.*", status=~"5.."}[5m])` | Server error rate | Server Errors |

### 2.2 Endpoint Group Breakdown

| URI Pattern | Resource | Panel Group |
|-------------|----------|-------------|
| `/api/properties/**` | Properties | Resource Usage |
| `/api/tenants/**` | Tenants | Resource Usage |
| `/api/contracts/**` | Contracts | Resource Usage |
| `/api/payments/**` | Payments | Resource Usage |
| `/api/expenses/**` | Expenses | Resource Usage |
| `/api/documents/**` | Documents | Resource Usage |
| `/api/reports/**` | Reports/Exports | Resource Usage |
| `/api/dashboard/**` | Dashboard | Resource Usage |
| `/api/teams/**` | Teams | Resource Usage |
| `/api/auth/**` | Auth | Resource Usage |
| `/api/calendar-feeds/**` | Calendar Feeds | Resource Usage |

**Dashboard Panels:**
- **API Call Rate by Resource** - stacked area per resource group
- **API Latency P95 by Resource** - timeseries per resource group
- **Client Error Rate (4xx) by Resource** - timeseries
- **Server Error Rate (5xx) by Resource** - timeseries
- **Top 10 Slowest Endpoints** - table sorted by P95
- **Top 10 Most Called Endpoints** - table sorted by request rate
- **Error Rate Heatmap** - heatmap of errors by endpoint and time
- **Request Size Distribution** - histogram (if available via Tomcat)

---

## 3. Authentication Dashboard

### 3.1 Auth Endpoint Metrics (derived from HTTP metrics)

| Derived Query | Description | Panel |
|---------------|-------------|-------|
| `rate(http_server_requests_seconds_count{uri="/api/auth/register"}[5m])` | Registration rate | Registration Rate |
| `rate(http_server_requests_seconds_count{uri="/api/auth/register", status=~"4..|5.."}[5m])` | Registration failures | Registration Failures |
| `rate(http_server_requests_seconds_count{uri="/api/auth/me"}[5m])` | Token validation rate | Token Checks |
| `rate(http_server_requests_seconds_count{uri=~"/api/invitations/.*"}[5m])` | Invitation activity | Invitations |

### 3.2 Keycloak Integration Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_keycloak_user_creation_seconds` | Timer | `result` (success/failure) | Keycloak user creation duration | Keycloak Latency |
| `buurman_keycloak_user_creation_total` | Counter | `result` | Total Keycloak user creations | Keycloak Operations |
| `buurman_keycloak_user_deletion_total` | Counter | `result` | Total Keycloak user deletions | Keycloak Operations |

### 3.3 JWT Filter Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_auth_jwt_validation_total` | Counter | `result` (valid/invalid/expired/missing) | JWT validation outcomes | JWT Validation |
| `buurman_auth_jwt_validation_seconds` | Timer | - | JWT validation duration | JWT Latency |
| `buurman_auth_active_sessions` | Gauge | - | Approximate active sessions (unique team_ids in last 5min) | Active Sessions |

### 3.4 Team & Authorization Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_auth_access_denied_total` | Counter | `role`, `endpoint` | Access denied events | Access Denied |
| `buurman_team_invitation_total` | Counter | `result` (sent/accepted/expired) | Team invitation lifecycle | Invitations |

**Dashboard Panels:**
- **Registration Rate** - rate with success/failure split
- **Keycloak Response Time** - P95 latency
- **JWT Validation Outcomes** - stacked bar (valid/invalid/expired/missing)
- **JWT Validation Latency** - timeseries
- **Active Sessions** - stat panel
- **Access Denied Events** - timeseries
- **Team Invitation Funnel** - sent vs accepted vs expired
- **Auth Endpoint Error Rate** - timeseries for /api/auth/** 4xx/5xx

---

## 4. External Integrations Dashboard

### 4.1 S3 Storage Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_s3_operation_seconds` | Timer | `operation` (upload/download/delete/presign), `result` (success/failure) | S3 operation duration | S3 Latency |
| `buurman_s3_operation_total` | Counter | `operation`, `result` | Total S3 operations | S3 Operations |
| `buurman_s3_upload_bytes_total` | Counter | - | Total bytes uploaded | Upload Volume |
| `buurman_s3_download_bytes_total` | Counter | - | Total bytes downloaded | Download Volume |

### 4.2 Email/Mailhog Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_email_sent_total` | Counter | `template` (welcome/team-invitation/invitation-accepted/password-changed/payment-reminder/contract-expiry), `result` (success/failure) | Emails sent | Email Volume |
| `buurman_email_send_seconds` | Timer | `template` | Email send duration | Email Latency |

### 4.3 Document Operations (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_document_upload_total` | Counter | `entity_type` (PROPERTY/TENANT/CONTRACT/PAYMENT/EXPENSE), `category` (PHOTO/DOCUMENT) | Document uploads | Document Uploads |
| `buurman_document_upload_bytes_total` | Counter | `entity_type` | Total upload size | Upload Size |
| `buurman_document_download_total` | Counter | `entity_type` | Document downloads | Document Downloads |
| `buurman_document_delete_total` | Counter | `entity_type` | Document deletions | Document Deletions |
| `buurman_document_bulk_download_total` | Counter | - | Bulk zip downloads | Bulk Downloads |

### 4.4 Export/PDF Generation (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_export_generation_seconds` | Timer | `type` (transaction_csv/transaction_pdf/property_brochure/contract_report/tenant_report), `result` | Export generation duration | Export Latency |
| `buurman_export_generation_total` | Counter | `type`, `result` | Total exports generated | Export Volume |
| `buurman_export_size_bytes` | DistributionSummary | `type` | Export file size distribution | Export Size |

**Dashboard Panels:**
- **S3 Operation Rate** - rate by operation type
- **S3 Latency P95** - per operation type
- **S3 Error Rate** - failures / total
- **Upload/Download Volume** - bytes over time
- **Email Delivery Rate** - by template
- **Email Delivery Failures** - failures per template
- **Email Latency** - P95
- **Document Operations** - uploads/downloads/deletes over time
- **Export Generation Rate** - by type
- **Export Generation Duration** - P95 by type
- **Export File Size Distribution** - histogram

---

## 5. PostgreSQL Database Dashboard

### 5.1 HikariCP Connection Pool (auto-configured)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `hikaricp_connections` | Gauge | `pool` | Total connections | Connection Pool |
| `hikaricp_connections_active` | Gauge | `pool` | Active connections | Connection Pool |
| `hikaricp_connections_idle` | Gauge | `pool` | Idle connections | Connection Pool |
| `hikaricp_connections_pending` | Gauge | `pool` | Pending connection requests | Connection Pool |
| `hikaricp_connections_max` | Gauge | `pool` | Max pool size | Connection Pool |
| `hikaricp_connections_min` | Gauge | `pool` | Min pool size | Connection Pool |
| `hikaricp_connections_acquire_seconds` | Timer | `pool` | Connection acquire time | Acquire Latency |
| `hikaricp_connections_creation_seconds` | Timer | `pool` | Connection creation time | Create Latency |
| `hikaricp_connections_usage_seconds` | Timer | `pool` | Connection usage duration | Usage Duration |
| `hikaricp_connections_timeout_total` | Counter | `pool` | Connection timeouts | Timeouts |

**Dashboard Panels:**
- **Connection Pool Status** - active, idle, pending, max stacked
- **Connection Acquire Latency** - P95
- **Connection Usage Duration** - P95
- **Connection Timeouts** - rate
- **Pool Utilization %** - active / max * 100

### 5.2 PostgreSQL Exporter Metrics (via postgres-exporter sidecar)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `pg_stat_activity_count` | Gauge | `state`, `datname` | Active connections by state | PG Connections |
| `pg_stat_database_tup_fetched` | Counter | `datname` | Rows fetched | Row Operations |
| `pg_stat_database_tup_inserted` | Counter | `datname` | Rows inserted | Row Operations |
| `pg_stat_database_tup_updated` | Counter | `datname` | Rows updated | Row Operations |
| `pg_stat_database_tup_deleted` | Counter | `datname` | Rows deleted | Row Operations |
| `pg_stat_database_xact_commit` | Counter | `datname` | Committed transactions | Transactions |
| `pg_stat_database_xact_rollback` | Counter | `datname` | Rolled back transactions | Transactions |
| `pg_stat_database_blks_hit` | Counter | `datname` | Buffer cache hits | Cache Performance |
| `pg_stat_database_blks_read` | Counter | `datname` | Disk blocks read | Cache Performance |
| `pg_stat_database_deadlocks` | Counter | `datname` | Deadlocks detected | Deadlocks |
| `pg_stat_database_conflicts` | Counter | `datname` | Query conflicts | Conflicts |
| `pg_stat_database_numbackends` | Gauge | `datname` | Connected backends | Backends |
| `pg_stat_user_tables_seq_scan` | Counter | `relname`, `schemaname` | Sequential scans | Table Stats |
| `pg_stat_user_tables_idx_scan` | Counter | `relname`, `schemaname` | Index scans | Table Stats |
| `pg_stat_user_tables_n_tup_ins` | Counter | `relname`, `schemaname` | Rows inserted per table | Table Stats |
| `pg_stat_user_tables_n_tup_upd` | Counter | `relname`, `schemaname` | Rows updated per table | Table Stats |
| `pg_stat_user_tables_n_dead_tup` | Gauge | `relname`, `schemaname` | Dead tuples (vacuum needed) | Vacuum Status |
| `pg_stat_user_tables_last_autovacuum` | Gauge | `relname`, `schemaname` | Last autovacuum timestamp | Vacuum Status |
| `pg_database_size_bytes` | Gauge | `datname` | Database size | Database Size |
| `pg_stat_replication_lag` | Gauge | `application_name` | Replication lag (if replica) | Replication |
| `pg_locks_count` | Gauge | `mode`, `datname` | Lock counts by mode | Locks |
| `pg_slow_queries` | Gauge | - | Long-running queries (>1s) | Slow Queries |

**Dashboard Panels:**
- **Database Size** - stat panel
- **Active Connections by State** - stacked area
- **Transaction Rate** - commit vs rollback
- **Cache Hit Ratio** - `blks_hit / (blks_hit + blks_read) * 100`
- **Row Operations Rate** - fetch/insert/update/delete
- **Deadlocks** - counter with alert threshold
- **Sequential vs Index Scans** - per table (top 10)
- **Dead Tuples** - top tables needing vacuum
- **Lock Distribution** - by mode
- **Slow Queries Count** - timeseries
- **Table Size Ranking** - bar chart
- **Replication Lag** - if applicable

---

## 6. Business Metrics Dashboard

### 6.1 Payment Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_payment_created_total` | Counter | `team_id` | Payments created | Payment Volume |
| `buurman_payment_status_changed_total` | Counter | `from_status`, `to_status` | Payment status transitions | Status Changes |
| `buurman_payment_marked_paid_total` | Counter | - | Payments marked as paid | Paid Count |
| `buurman_payment_overdue_current` | Gauge | `team_id` | Currently overdue payments | Overdue Gauge |
| `buurman_payment_amount_total` | Counter | `currency`, `status` | Total payment amounts | Revenue |
| `buurman_payment_bulk_generated_total` | Counter | - | Bulk payment generations | Bulk Generate |
| `buurman_payment_bulk_generated_count` | DistributionSummary | - | Payments per bulk generation | Bulk Size |
| `buurman_payment_receival_total` | Counter | `action` (registered/updated/deleted) | Payment receival operations | Receivals |
| `buurman_payment_days_overdue` | DistributionSummary | - | Distribution of days overdue | Overdue Distribution |

**Dashboard Panels:**
- **Payments Created Over Time** - rate
- **Payment Status Distribution** - pie chart (current snapshot)
- **Overdue Payments Count** - stat with alert threshold
- **Revenue Collected (Paid)** - cumulative line
- **Payment Status Transitions** - sankey or stacked bar
- **Bulk Generation Activity** - bar chart
- **Receival Activity** - register/update/delete over time
- **Average Days Overdue** - trend

### 6.2 Contract Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_contract_created_total` | Counter | - | Contracts created | Contract Volume |
| `buurman_contract_status_changed_total` | Counter | `from_status`, `to_status` | Status transitions | Status Changes |
| `buurman_contract_active_current` | Gauge | `team_id` | Currently active contracts | Active Contracts |
| `buurman_contract_expiring_soon` | Gauge | `team_id` | Contracts expiring within 30 days | Expiring Soon |
| `buurman_contract_duplicated_total` | Counter | - | Contracts duplicated | Duplications |
| `buurman_contract_reopened_total` | Counter | - | Contracts reopened | Reopens |
| `buurman_contract_rent_amount` | DistributionSummary | `currency` | Rent amount distribution | Rent Distribution |

**Dashboard Panels:**
- **Active Contracts** - stat panel
- **Contract Status Distribution** - pie chart
- **Contracts Expiring Soon** - stat with alert
- **Contract Lifecycle** - status transitions over time
- **New Contracts Over Time** - rate
- **Rent Amount Distribution** - histogram
- **Contract Duration Distribution** - histogram

### 6.3 Property Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_property_created_total` | Counter | - | Properties created | Property Volume |
| `buurman_property_status_current` | Gauge | `status` (occupied/vacant/maintenance/unavailable), `team_id` | Properties per status | Status Distribution |
| `buurman_property_occupancy_rate` | Gauge | `team_id` | Occupancy rate (0-1) | Occupancy Rate |

**Dashboard Panels:**
- **Total Properties** - stat
- **Property Status Distribution** - pie chart
- **Occupancy Rate** - gauge (0-100%)
- **Occupancy Rate Trend** - timeseries
- **New Properties Over Time** - rate

### 6.4 Tenant Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_tenant_created_total` | Counter | - | Tenants created | Tenant Volume |
| `buurman_tenant_linked_total` | Counter | - | Tenant-property links | Link Activity |
| `buurman_tenant_unlinked_total` | Counter | - | Tenant-property unlinks | Unlink Activity |
| `buurman_tenant_active_current` | Gauge | `team_id` | Tenants with active property links | Active Tenants |

**Dashboard Panels:**
- **Total Tenants** - stat
- **New Tenants Over Time** - rate
- **Link/Unlink Activity** - stacked bar
- **Active Tenants** - stat

### 6.5 Document Metrics (custom - see also section 4.3)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_document_total_current` | Gauge | `entity_type`, `category` | Current document count | Document Inventory |
| `buurman_document_storage_bytes_current` | Gauge | `entity_type` | Current storage used | Storage Usage |

**Dashboard Panels:**
- **Document Inventory** - bar chart by entity type and category
- **Storage Usage** - per entity type
- **Upload Trend** - documents uploaded over time (from section 4.3)

### 6.6 Export & Report Metrics (custom - see also section 4.4)

**Dashboard Panels:**
- **Export Generation Rate** - by type (CSV, PDF brochure, tenant report, contract report)
- **Most Popular Export Type** - pie chart
- **Export Generation Duration** - P95

### 6.7 Team Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_team_members_current` | Gauge | `team_id`, `role` | Members per team per role | Team Size |
| `buurman_team_registered_total` | Counter | - | Teams registered | Team Growth |
| `buurman_team_settings_updated_total` | Counter | - | Team settings changes | Settings Activity |

**Dashboard Panels:**
- **Total Teams** - stat
- **Team Growth** - cumulative over time
- **Team Size Distribution** - histogram
- **Member Role Distribution** - pie chart (ADMIN/EDITOR/VIEWER)

### 6.8 Audit Trail Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_audit_log_total` | Counter | `entity_type`, `action` (CREATE/UPDATE/DELETE) | Audit log entries | Audit Activity |

**Dashboard Panels:**
- **Audit Activity Over Time** - stacked area by action type
- **Operations by Entity Type** - bar chart
- **Create vs Update vs Delete Ratio** - pie chart

---

## Alerting Rules (for Prometheus alertmanager)

### Critical Alerts
| Alert | Expression | Duration | Severity |
|-------|-----------|----------|----------|
| HighErrorRate | `rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.05` | 5m | critical |
| HighLatency | `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 2` | 5m | critical |
| DatabaseConnectionPoolExhausted | `hikaricp_connections_pending > 5` | 2m | critical |
| DatabaseConnectionTimeout | `rate(hikaricp_connections_timeout_total[5m]) > 0` | 1m | critical |
| HeapMemoryHigh | `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.9` | 5m | critical |
| ServiceDown | `up{job="buurman-backend"} == 0` | 1m | critical |

### Warning Alerts
| Alert | Expression | Duration | Severity |
|-------|-----------|----------|----------|
| ElevatedErrorRate | `rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.01` | 10m | warning |
| SlowEndpoint | `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1` | 10m | warning |
| PoolUtilizationHigh | `hikaricp_connections_active / hikaricp_connections_max > 0.8` | 5m | warning |
| HeapMemoryWarning | `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.75` | 10m | warning |
| GCPressure | `rate(jvm_gc_pause_seconds_sum[5m]) > 0.5` | 5m | warning |
| S3OperationSlow | `histogram_quantile(0.95, rate(buurman_s3_operation_seconds_bucket[5m])) > 5` | 5m | warning |
| EmailDeliveryFailure | `rate(buurman_email_sent_total{result="failure"}[5m]) > 0` | 5m | warning |
| SchedulerStale | `time() - buurman_scheduler_last_success_timestamp > 90000` | 5m | warning |
| OverduePayments | `buurman_payment_overdue_current > 10` | 30m | warning |
| LowCacheHitRatio | `pg_stat_database_blks_hit / (pg_stat_database_blks_hit + pg_stat_database_blks_read) < 0.95` | 10m | warning |
| DatabaseDeadlocks | `rate(pg_stat_database_deadlocks[5m]) > 0` | 1m | warning |
| HighDeadTuples | `pg_stat_user_tables_n_dead_tup > 10000` | 30m | warning |
| KeycloakSlow | `histogram_quantile(0.95, rate(buurman_keycloak_user_creation_seconds_bucket[5m])) > 3` | 5m | warning |

---

## Implementation Notes

### Metric Naming Conventions
- All custom metrics: `buurman_<domain>_<metric>_<unit>`
- Units follow Prometheus conventions: `_seconds`, `_bytes`, `_total`
- Counter names end in `_total`
- Gauges use `_current` suffix for snapshot values
- Timers produce `_seconds`, `_seconds_count`, `_seconds_sum`, `_seconds_bucket`

### Tag Cardinality Guidelines
- `team_id` label: only on gauges and low-cardinality counters (avoid on high-throughput timers)
- `uri` label: use Spring MVC templated URIs (e.g., `/api/properties/{identifier}`) not actual paths
- `status`: HTTP status code (200, 400, 404, 500 etc.)
- Avoid unbounded label values (no user IDs, entity IDs, or free-text in tags)

### Instrumentation Approach
- Use `MeterRegistry` injected via constructor in service classes
- Timers: wrap operations with `Timer.builder("name").tag(...).register(registry).record(supplier)`
- Counters: `Counter.builder("name").tag(...).register(registry).increment()`
- Gauges: register gauge backed by AtomicInteger/AtomicLong or collection size
- Use `@Timed` annotation where appropriate for simpler instrumentation
- For scheduled jobs: wrap with `Timer.Sample` for precise measurement

### Dashboard Datasource
- All dashboards use `Prometheus` datasource (uid: `prometheus`)
- Template variable `$interval` for aggregation window (default: `5m`)
- Template variable `$job` for Prometheus job selector (default: `buurman-backend`)
- Database dashboard additionally uses postgres-exporter job
