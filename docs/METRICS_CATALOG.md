# Buurman Metrics Catalog

All custom application metrics use the `buurman_` prefix. Spring Boot auto-configured metrics (JVM, HikariCP, HTTP server, Spring Security) use their standard prefixes. This catalog is the single source of truth for all monitoring dashboards.

> **Legend:** Metrics marked ✅ are implemented and available. Metrics marked 🔲 are planned but not yet instrumented.

---

## 1. Backend Application Dashboard

### 1.1 HTTP Server Metrics (auto-configured by Spring Boot Actuator)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `http_server_requests_seconds` ✅ | Timer (histogram) | `method`, `uri`, `status`, `outcome`, `exception` | Request duration distribution | Request Rate, Latency Percentiles, Error Rate |
| `http_server_requests_seconds_count` ✅ | Counter | (same) | Total request count | Request Rate (rate()) |
| `http_server_requests_seconds_sum` ✅ | Counter | (same) | Total request time | Avg Latency |
| `http_server_requests_seconds_max` ✅ | Gauge | (same) | Max request time in window | Max Latency |
| `http_server_requests_active_seconds` ✅ | Timer | `method`, `uri` | Currently active request duration | Active Requests |

**Dashboard Panels:**
- **Request Rate** - `rate(http_server_requests_seconds_count[5m])` grouped by `uri`
- **Latency P50/P95/P99** - `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))`
- **Error Rate (4xx/5xx)** - `rate(http_server_requests_seconds_count{status=~"4..|5.."}[5m])`
- **Top Endpoints by Latency** - table sorted by P95
- **Status Code Distribution** - pie chart by `status`

### 1.2 JVM Metrics (auto-configured by Micrometer)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `jvm_memory_used_bytes` ✅ | Gauge | `area` (heap/nonheap), `id` | Current memory usage | Memory Usage |
| `jvm_memory_max_bytes` ✅ | Gauge | `area`, `id` | Max memory | Memory Usage |
| `jvm_memory_committed_bytes` ✅ | Gauge | `area`, `id` | Committed memory | Memory Usage |
| `jvm_memory_usage_after_gc` ✅ | Gauge | `area`, `pool` | Memory usage after GC | Memory Usage |
| `jvm_gc_pause_seconds` ✅ | Timer | `action`, `cause` | GC pause duration | GC Activity |
| `jvm_gc_concurrent_phase_time_seconds` ✅ | Timer | `action`, `cause` | GC concurrent phase duration | GC Activity |
| `jvm_gc_live_data_size_bytes` ✅ | Gauge | - | Live data size after GC | GC Health |
| `jvm_gc_max_data_size_bytes` ✅ | Gauge | - | Max data size for GC | GC Health |
| `jvm_gc_memory_allocated_bytes_total` ✅ | Counter | - | Memory allocated since start | GC Allocation |
| `jvm_gc_memory_promoted_bytes_total` ✅ | Counter | - | Memory promoted to old gen | GC Promotion |
| `jvm_gc_overhead` ✅ | Gauge | - | GC overhead estimate (0-1) | GC Health |
| `jvm_threads_live_threads` ✅ | Gauge | - | Current live threads | Thread Usage |
| `jvm_threads_peak_threads` ✅ | Gauge | - | Peak thread count | Thread Usage |
| `jvm_threads_daemon_threads` ✅ | Gauge | - | Daemon thread count | Thread Usage |
| `jvm_threads_states_threads` ✅ | Gauge | `state` | Threads per state | Thread States |
| `jvm_classes_loaded_classes` ✅ | Gauge | - | Loaded classes | Class Loading |
| `jvm_buffer_memory_used_bytes` ✅ | Gauge | `id` | Buffer pool usage | Buffer Pools |
| `jvm_compilation_time_ms_total` ✅ | Counter | - | JIT compilation time | JIT |
| `process_cpu_usage` ✅ | Gauge | - | Process CPU usage (0-1) | CPU Usage |
| `system_cpu_usage` ✅ | Gauge | - | System CPU usage (0-1) | CPU Usage |
| `system_cpu_count` ✅ | Gauge | - | Available processors | CPU Info |
| `system_load_average_1m` ✅ | Gauge | - | System load average | CPU Usage |
| `process_uptime_seconds` ✅ | Gauge | - | Process uptime | Uptime stat |

**Dashboard Panels:**
- **Heap Memory Used vs Max** - timeseries with heap area
- **Non-Heap Memory** - timeseries
- **GC Pause Duration** - `rate(jvm_gc_pause_seconds_sum[5m]) / rate(jvm_gc_pause_seconds_count[5m])`
- **GC Rate** - `rate(jvm_gc_pause_seconds_count[5m])`
- **GC Overhead** - `jvm_gc_overhead` as percentage
- **CPU Usage** - process + system overlaid
- **Thread Count** - live, peak, daemon stacked
- **Thread States** - stacked area by state
- **Uptime** - stat panel

### 1.3 Tomcat & Application Metrics (auto-configured)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `tomcat_sessions_active_current_sessions` ✅ | Gauge | - | Active sessions | Sessions |
| `tomcat_sessions_active_max_sessions` ✅ | Gauge | - | Max active sessions | Sessions |
| `tomcat_sessions_created_sessions_total` ✅ | Counter | - | Total sessions created | Sessions |
| `application_ready_time_seconds` ✅ | Gauge | - | Application ready time | Startup |
| `application_started_time_seconds` ✅ | Gauge | - | Application started time | Startup |
| `logback_events_total` ✅ | Counter | `level` | Log events by level | Logging |
| `executor_active_threads` ✅ | Gauge | `name` | Active executor threads | Thread Pools |
| `executor_pool_size_threads` ✅ | Gauge | `name` | Executor pool size | Thread Pools |
| `executor_queue_remaining_tasks` ✅ | Gauge | `name` | Executor queue remaining capacity | Thread Pools |

**Dashboard Panels:**
- **Active Sessions** - current vs max
- **Log Event Rate** - by level (ERROR, WARN)
- **Executor Thread Pools** - active, pool size, queue

### 1.4 Scheduler Metrics (custom)

Instrumented centrally via `ExecutionHistoryJobListener` — all Quartz jobs are automatically tracked.

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_scheduler_execution_seconds` ✅ | Timer | `job_name` | Scheduled job duration | Scheduler Duration |
| `buurman_scheduler_execution_total` ✅ | Counter | `job_name`, `result` (success/failure) | Scheduled job executions | Scheduler Runs |
| `buurman_scheduler_last_success_timestamp` ✅ | Gauge | `job_name` | Epoch of last successful run | Last Run |
| `buurman_scheduler_items_processed_total` ✅ | Counter | `job_name` | Items processed per run | Processing Volume |

**Dashboard**: Operations → Scheduler Jobs section

**Jobs reporting `itemsProcessed`** (via `context.put("itemsProcessed", count)`):
- NotificationOutboxJob — outbox entries processed
- ThumbnailBackfillJob — photos processed
- ExecutionHistoryCleanupJob — history entries deleted
- ImpersonationSessionCleanupJob — sessions expired
- RateLimitCleanupJob — bucket entries removed
- VerificationCodeCleanupJob — verification codes deleted
- TakeoutCleanupJob — expired takeouts cleaned

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
| `/api/photos/**` | Photos | Resource Usage |
| `/api/reports/**` | Reports/Exports | Resource Usage |
| `/api/dashboard/**` | Dashboard | Resource Usage |
| `/api/teams/**` | Teams | Resource Usage |
| `/api/auth/**` | Auth | Resource Usage |
| `/api/calendar-feeds/**` | Calendar Feeds | Resource Usage |

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
| `buurman_keycloak_user_creation_seconds` ✅ | Timer | `result` (success/failure) | Keycloak user creation duration | Keycloak Latency |
| `buurman_keycloak_user_creation_total` ✅ | Counter | `result` | Total Keycloak user creations | Keycloak Operations |
| `buurman_keycloak_user_deletion_total` ✅ | Counter | `result` | Total Keycloak user deletions | Keycloak Operations |

> Timer recorded in `KeycloakService.createUser()`. Creation counter recorded in `AuthService.register()`. Deletion counter in `KeycloakService.deleteUser()`.

### 3.3 Spring Security Metrics (auto-configured)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `spring_security_authentications_seconds` ✅ | Timer | - | Authentication duration | Auth Latency |
| `spring_security_authorizations_seconds` ✅ | Timer | - | Authorization check duration | Authz Latency |
| `spring_security_filterchains_seconds` ✅ | Timer | - | Full security filter chain duration | Filter Chain |
| `spring_security_filterchains_{FilterName}_before_total` ✅ | Counter | - | Per-filter invocation count | Filter Breakdown |
| `spring_security_http_secured_requests_seconds` ✅ | Timer | - | Secured request total duration | Secured Requests |

> Notable filters tracked: `AuthenticationFilter`, `RateLimitFilter`, `EmailVerificationFilter`, `ImpersonationJwtFilter`, `MdcFilter`.

### 3.4 Team & Authorization Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_team_invitation_total` ✅ | Counter | `result` (sent/accepted) | Team invitation lifecycle | Invitations |
| `buurman_team_registered_total` ✅ | Counter | - | Team registrations | Team Growth |
| `buurman_ratelimit_rejected_total` ✅ | Counter | `endpoint` | Rate limit rejections | Rate Limiting |
| `buurman_ratelimit_error_total` ✅ | Counter | `endpoint` | Rate limit errors | Rate Limiting |

### 3.5 Registration Invitation Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_registration_invitation_created_total` ✅ | Counter | - | Invitations created | Registration |
| `buurman_registration_invitation_revoked_total` ✅ | Counter | - | Invitations revoked | Registration |
| `buurman_registration_invitation_validated_total` ✅ | Counter | `result` | Invitation code validations | Registration |
| `buurman_registration_invitation_used_total` ✅ | Counter | - | Invitations used | Registration |

**Dashboard Panels:**
- **Registration Rate** - rate with success/failure split
- **Keycloak Response Time** - P95 latency
- **Spring Security Auth Duration** - authentication + authorization timeseries
- **Security Filter Chain Duration** - P95
- **Rate Limit Rejections** - timeseries
- **Team Invitation Funnel** - sent vs accepted
- **Auth Endpoint Error Rate** - timeseries for /api/auth/** 4xx/5xx

---

## 4. External Integrations Dashboard

### 4.1 S3 Storage Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_s3_operation_seconds` ✅ | Timer | `operation` (upload/download/delete/presign), `result` (success/failure) | S3 operation duration | S3 Latency |
| `buurman_s3_operation_total` ✅ | Counter | `operation`, `result` | Total S3 operations | S3 Operations |
| `buurman_s3_upload_bytes_total` ✅ | Counter | - | Total bytes uploaded | Upload Volume |

> Recorded in `S3StorageService`. Histograms enabled in `application.yml` for `buurman.s3.operation.seconds`.

### 4.2 Notification Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_notification_send_seconds` ✅ | Timer | `channel` (email/sms), `provider` (sendgrid/twilio/mailpit/local), `result` (success/failure) | Notification send duration | Notification Latency |
| `buurman_notification_send_total` ✅ | Counter | `channel`, `provider`, `result` | Total notifications sent | Notification Volume |

> Recorded via `MetricsService.recordNotificationSend()` in all channel senders (SendGridEmailSender, LocalEmailSender, TwilioSmsSender, LocalSmsSender).

### 4.3 Document & Photo Operations (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_document_upload_total` ✅ | Counter | `entity_type` | Document uploads | Document Uploads |
| `buurman_document_upload_bytes` ✅ | DistributionSummary | `entity_type` | Upload size distribution | Upload Size |
| `buurman_document_download_total` ✅ | Counter | `entity_type` | Document downloads | Document Downloads |
| `buurman_document_delete_total` ✅ | Counter | `entity_type` | Document deletions | Document Deletions |
| `buurman_document_bulk_download_total` ✅ | Counter | - | Bulk zip downloads | Bulk Downloads |
| `buurman_photo_upload_total` ✅ | Counter | `entity_type` | Photo uploads | Photo Uploads |
| `buurman_photo_upload_bytes` ✅ | DistributionSummary | `entity_type` | Photo upload size distribution | Upload Size |
| `buurman_photo_download_total` ✅ | Counter | `entity_type` | Photo downloads | Photo Downloads |
| `buurman_photo_delete_total` ✅ | Counter | `entity_type` | Photo deletions | Photo Deletions |
| `buurman_photo_bulk_download_total` ✅ | Counter | - | Bulk zip downloads | Bulk Downloads |

> Recorded in `DocumentService` and `PhotoService`.

### 4.4 Export/PDF Generation (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_export_generation_seconds` ✅ | Timer | `type`, `result` (success/failure) | Export generation duration | Export Latency |
| `buurman_export_generation_total` ✅ | Counter | `type`, `result` | Total exports generated | Export Volume |
| `buurman_export_size_bytes` ✅ | DistributionSummary | `type` | Export file size distribution | Export Size |

**Export types:** `transaction_csv`, `transaction_pdf`, `property_brochure`, `contract_report`, `tenant_report`, `property_dashboard_pdf`, `property_dashboard_csv`, `portfolio_dashboard_pdf`, `portfolio_dashboard_csv`, `transaction_excel`, `property_dashboard_excel`, `portfolio_dashboard_excel`

> Recorded in `ExportServiceImpl`. Histograms enabled in `application.yml` for `buurman.export.generation.seconds`.

### 4.5 Data Takeout Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_takeout_requested_total` ✅ | Counter | - | Takeout exports requested | Takeout Requests |
| `buurman_takeout_completed_total` ✅ | Counter | `result` (success/failure) | Takeout exports completed | Takeout Completions |

> Recorded in `TakeoutService`.

### 4.6 Geocoding Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_geocoding_seconds` ✅ | Timer | - | Geocoding API call duration | Geocoding Latency |
| `buurman_geocoding_total` ✅ | Counter | `result` (success/failure/not_found), `accuracy` (rooftop/range_interpolated/geometric_center/approximate) | Geocoding results | Geocoding Volume |
| `buurman_geocoding_error_total` ✅ | Counter | - | Geocoding errors | Geocoding Errors |

> Recorded in `GeocodingService`.

**Dashboard Panels:**
- **S3 Operation Rate** - rate by operation type
- **S3 Latency P95** - per operation type
- **S3 Error Rate** - failures / total
- **Upload Volume** - bytes over time
- **Notification Delivery Rate** - by channel and provider
- **Notification Delivery Failures** - failures by provider
- **Notification Latency** - P95 by channel
- **Document/Photo Operations** - uploads/downloads/deletes over time
- **Document Upload Size Distribution** - P50/P95
- **Export Generation Rate** - by type
- **Export Generation Duration** - P95 by type
- **Export File Size Distribution** - histogram
- **Geocoding Duration** - P95
- **Geocoding Results** - by accuracy

---

## 5. PostgreSQL Database Dashboard

### 5.1 HikariCP Connection Pool (auto-configured)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `hikaricp_connections` ✅ | Gauge | `pool` | Total connections | Connection Pool |
| `hikaricp_connections_active` ✅ | Gauge | `pool` | Active connections | Connection Pool |
| `hikaricp_connections_idle` ✅ | Gauge | `pool` | Idle connections | Connection Pool |
| `hikaricp_connections_pending` ✅ | Gauge | `pool` | Pending connection requests | Connection Pool |
| `hikaricp_connections_max` ✅ | Gauge | `pool` | Max pool size | Connection Pool |
| `hikaricp_connections_min` ✅ | Gauge | `pool` | Min pool size | Connection Pool |
| `hikaricp_connections_acquire_seconds` ✅ | Timer | `pool` | Connection acquire time | Acquire Latency |
| `hikaricp_connections_creation_seconds` ✅ | Timer | `pool` | Connection creation time | Create Latency |
| `hikaricp_connections_usage_seconds` ✅ | Timer | `pool` | Connection usage duration | Usage Duration |
| `hikaricp_connections_timeout_total` ✅ | Counter | `pool` | Connection timeouts | Timeouts |

**Dashboard Panels:**
- **Connection Pool Status** - active, idle, pending, max stacked
- **Connection Acquire Latency** - P95
- **Connection Usage Duration** - P95
- **Connection Timeouts** - rate
- **Pool Utilization %** - active / max * 100

### 5.2 PostgreSQL Exporter Metrics (via postgres-exporter sidecar)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `pg_stat_activity_count` ✅ | Gauge | `state`, `datname` | Active connections by state | PG Connections |
| `pg_stat_database_tup_fetched` ✅ | Counter | `datname` | Rows fetched | Row Operations |
| `pg_stat_database_tup_inserted` ✅ | Counter | `datname` | Rows inserted | Row Operations |
| `pg_stat_database_tup_updated` ✅ | Counter | `datname` | Rows updated | Row Operations |
| `pg_stat_database_tup_deleted` ✅ | Counter | `datname` | Rows deleted | Row Operations |
| `pg_stat_database_xact_commit` ✅ | Counter | `datname` | Committed transactions | Transactions |
| `pg_stat_database_xact_rollback` ✅ | Counter | `datname` | Rolled back transactions | Transactions |
| `pg_stat_database_blks_hit` ✅ | Counter | `datname` | Buffer cache hits | Cache Performance |
| `pg_stat_database_blks_read` ✅ | Counter | `datname` | Disk blocks read | Cache Performance |
| `pg_stat_database_deadlocks` ✅ | Counter | `datname` | Deadlocks detected | Deadlocks |
| `pg_stat_database_conflicts` ✅ | Counter | `datname` | Query conflicts | Conflicts |
| `pg_stat_database_numbackends` ✅ | Gauge | `datname` | Connected backends | Backends |
| `pg_stat_database_temp_bytes` ✅ | Counter | `datname` | Temp file bytes written | Temp Usage |
| `pg_stat_database_temp_files` ✅ | Counter | `datname` | Temp files created | Temp Usage |
| `pg_stat_user_tables_seq_scan` ✅ | Counter | `relname`, `schemaname` | Sequential scans | Table Stats |
| `pg_stat_user_tables_idx_scan` ✅ | Counter | `relname`, `schemaname` | Index scans | Table Stats |
| `pg_stat_user_tables_n_tup_ins` ✅ | Counter | `relname`, `schemaname` | Rows inserted per table | Table Stats |
| `pg_stat_user_tables_n_tup_upd` ✅ | Counter | `relname`, `schemaname` | Rows updated per table | Table Stats |
| `pg_stat_user_tables_n_dead_tup` ✅ | Gauge | `relname`, `schemaname` | Dead tuples (vacuum needed) | Vacuum Status |
| `pg_stat_user_tables_n_live_tup` ✅ | Gauge | `relname`, `schemaname` | Live tuples | Table Stats |
| `pg_stat_user_tables_last_autovacuum` ✅ | Gauge | `relname`, `schemaname` | Last autovacuum timestamp | Vacuum Status |
| `pg_stat_user_tables_table_size_bytes` ✅ | Gauge | `relname`, `schemaname` | Table size in bytes | Table Size |
| `pg_stat_user_tables_index_size_bytes` ✅ | Gauge | `relname`, `schemaname` | Index size in bytes | Index Size |
| `pg_database_size_bytes` ✅ | Gauge | `datname` | Database size | Database Size |
| `pg_replication_lag_seconds` ✅ | Gauge | - | Replication lag (if replica) | Replication |
| `pg_locks_count` ✅ | Gauge | `mode`, `datname` | Lock counts by mode | Locks |
| `pg_wal_size_bytes` ✅ | Gauge | - | WAL directory size | WAL |
| `pg_wal_segments` ✅ | Gauge | - | WAL segments count | WAL |

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
- **Table Size Ranking** - bar chart
- **WAL Size** - timeseries
- **Replication Lag** - if applicable

---

## 6. Business Metrics Dashboard

### 6.1 Database Gauge Metrics (custom, refreshed every 15 minutes)

These gauges are backed by `DatabaseMetricsService` + `DatabaseMetricsRepository` queries. They exclude demo teams. Refresh is triggered by cron (`scheduling.metrics.metrics-push-cron: "0 */15 * * * ?"`).

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_properties_count` ✅ | Gauge | - | Total active properties | Properties stat |
| `buurman_contracts_count` ✅ | Gauge | - | Total active contracts | Contracts stat |
| `buurman_tenants_count` ✅ | Gauge | - | Total active tenants | Tenants stat |
| `buurman_payments_count` ✅ | Gauge | - | Total active payments | Payments stat |
| `buurman_teams_count` ✅ | Gauge | - | Total teams | Teams stat |
| `buurman_expenses_count` ✅ | Gauge | - | Total active expenses | Expenses stat |
| `buurman_contracts_by_status` ✅ | MultiGauge | `status` | Contracts grouped by status | Contract Status Distribution |
| `buurman_payments_by_status` ✅ | MultiGauge | `status` | Payments grouped by status | Payment Status Distribution |
| `buurman_properties_by_status` ✅ | MultiGauge | `status` | Properties grouped by status | Property Status Distribution |

### 6.2 Payment Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_payment_total` ✅ | Counter | - | Payments created | Payment Volume |
| `buurman_payment_status_changed_total` ✅ | Counter | `from_status`, `to_status` | Payment status transitions | Status Changes |
| `buurman_payment_marked_paid_total` ✅ | Counter | - | Payments marked as paid | Paid Count |
| `buurman_payment_amount_paid_total` ✅ | Counter | `currency` | Total paid amount (major units) | Revenue |
| `buurman_payment_bulk_generated_total` ✅ | Counter | - | Bulk payment generations | Bulk Generate |
| `buurman_payment_bulk_generated_count` ✅ | DistributionSummary | - | Payments per bulk generation | Bulk Size |
| `buurman_payment_receival_total` ✅ | Counter | `action` (registered/updated/deleted) | Payment receival operations | Receivals |

> Recorded in `PaymentService`. Use `buurman_payments_by_status{status="OVERDUE"}` for overdue count.

**Dashboard Panels:**
- **Payments Created Over Time** - rate
- **Payment Status Distribution** - pie chart from `buurman_payments_by_status`
- **Overdue Payments Count** - `buurman_payments_by_status{status="OVERDUE"}` stat
- **Revenue Collected (Paid)** - `rate(buurman_payment_amount_paid_total[5m])`
- **Payment Status Transitions** - stacked bar by from/to status
- **Bulk Generation Activity** - bar chart
- **Receival Activity** - register/update/delete over time

### 6.3 Contract Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_contract_total` ✅ | Counter | - | Contracts created | Contract Volume |
| `buurman_contract_status_changed_total` ✅ | Counter | `from_status`, `to_status` | Status transitions | Status Changes |
| `buurman_contract_duplicated_total` ✅ | Counter | - | Contracts duplicated | Duplications |
| `buurman_contract_reopened_total` ✅ | Counter | - | Contracts reopened | Reopens |
| `buurman_contract_rent_amount` ✅ | DistributionSummary | `currency` | Rent amount distribution | Rent Distribution |

> Recorded in `ContractService`. Use `buurman_contracts_by_status{status="ACTIVE"}` for active count. Terminations: `buurman_contract_status_changed_total{to_status="TERMINATED"}`.

**Dashboard Panels:**
- **Active Contracts** - `buurman_contracts_by_status{status="ACTIVE"}` stat
- **Contract Status Distribution** - pie chart from `buurman_contracts_by_status`
- **Contract Lifecycle** - status transitions over time
- **New Contracts Over Time** - rate of `buurman_contract_total`
- **Rent Amount Distribution** - histogram from `buurman_contract_rent_amount`

### 6.4 Property Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_property_total` ✅ | Counter | - | Properties created | Property Volume |

> Recorded in `PropertyService`. Use `buurman_properties_by_status` MultiGauge for status distribution and derived occupancy rate.

**Dashboard Panels:**
- **Total Properties** - `buurman_properties_count` stat
- **Property Status Distribution** - pie chart from `buurman_properties_by_status`
- **Occupancy Rate** - derived: `buurman_properties_by_status{status="OCCUPIED"} / buurman_properties_count`
- **New Properties Over Time** - rate

### 6.5 Tenant Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_tenant_total` ✅ | Counter | - | Tenants created | Tenant Volume |
| `buurman_tenant_linked_total` ✅ | Counter | - | Tenant-property links | Link Activity |
| `buurman_tenant_unlinked_total` ✅ | Counter | - | Tenant-property unlinks | Unlink Activity |

> Recorded in `TenantService`.

**Dashboard Panels:**
- **Total Tenants** - `buurman_tenants_count` stat
- **New Tenants Over Time** - rate
- **Link/Unlink Activity** - stacked bar

### 6.6 Export & Report Metrics (custom - see section 4.4)

**Dashboard Panels:**
- **Export Generation Rate** - by type (CSV, PDF brochure, tenant report, contract report, Excel)
- **Most Popular Export Type** - pie chart
- **Export Generation Duration** - P95

### 6.7 Team Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_team_registered_total` ✅ | Counter | - | Teams registered | Team Growth |
| `buurman_team_invitation_total` ✅ | Counter | `result` (sent/accepted) | Team invitations | Invitation Activity |
| `buurman_team_settings_updated_total` ✅ | Counter | - | Team settings changes | Settings Activity |

> Recorded in `AuthService` (registered), `TeamService` (invitation, settings).

**Dashboard Panels:**
- **Total Teams** - `buurman_teams_count` stat
- **Team Growth** - cumulative over time
- **Team Invitation Flow** - sent vs accepted

### 6.8 Audit Trail Metrics (custom)

| Metric | Type | Labels | Description | Panel |
|--------|------|--------|-------------|-------|
| `buurman_audit_log_total` ✅ | Counter | `entity_type`, `action` (CREATE/UPDATE/DELETE) | Audit log entries | Audit Activity |

> Recorded in `AuditService`.

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
| NotificationDeliveryFailure | `rate(buurman_notification_send_total{result="failure"}[5m]) > 0` | 5m | warning |
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
- Timers produce `_seconds`, `_seconds_count`, `_seconds_sum`, `_seconds_bucket`
- Micrometer uses dots (e.g., `buurman.s3.operation.seconds`), Prometheus scrape converts to underscores

### Tag Cardinality Guidelines
- `team_id` label: NOT used (avoid high-cardinality tags)
- `uri` label: use Spring MVC templated URIs (e.g., `/api/properties/{identifier}`) not actual paths
- `status`: HTTP status code (200, 400, 404, 500 etc.)
- Avoid unbounded label values (no user IDs, entity IDs, or free-text in tags)

### Instrumentation Approach
- Central `MetricsService` with helper methods: `incrementCounter`, `incrementCounterBy`, `recordTimer`, `recordHistogram`, `registerGauge`
- Database gauges: `DatabaseMetricsService` with `AtomicLong` + `MultiGauge`, refreshed via cron every 15 minutes
- Timers: wrap operations with `metricsService.recordTimer("name", duration, "tag", "value")`
- Counters: `metricsService.incrementCounter("name", "tag", "value")`
- Byte counters: `metricsService.incrementCounterBy("name", byteCount)`
- Histograms: `metricsService.recordHistogram("name", value, "tag", "value")`
- Notification metrics: dedicated `metricsService.recordNotificationSend(start, channel, provider, result)`

### Actuator Configuration
- Management port: `8082` (separate from app port 8081)
- Exposed endpoints: `health`, `info`, `metrics`, `prometheus`
- Global tag: `application: buurman`
- Prometheus scrape interval: `30s`
- Percentile histograms enabled for: `http.server.requests`, `buurman.s3.operation.seconds`, `buurman.export.generation.seconds`, `buurman.notification.send.seconds`

### Dashboard Datasource
- All dashboards use `Prometheus` datasource (uid: `prometheus`)
- Template variable `$job` for Prometheus job selector (default: `buurman-backend`)
- Database dashboard uses postgres-exporter job
- Hetzner dashboard uses `hetzner-nodes` job
