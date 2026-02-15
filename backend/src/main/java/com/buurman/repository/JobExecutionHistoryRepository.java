package com.buurman.repository;

import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.PageResponse;
import com.buurman.dto.response.backoffice.JobExecutionHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

@Repository
@RequiredArgsConstructor
public class JobExecutionHistoryRepository {

    private static final Table<Record> TABLE = DSL.table("job_execution_history");
    private static final Field<UUID> ID = DSL.field("id", UUID.class);
    private static final Field<String> JOB_NAME = DSL.field("job_name", String.class);
    private static final Field<String> JOB_GROUP = DSL.field("job_group", String.class);
    private static final Field<String> TRIGGER_NAME = DSL.field("trigger_name", String.class);
    private static final Field<String> TRIGGER_GROUP = DSL.field("trigger_group", String.class);
    private static final Field<Timestamp> STARTED_AT = DSL.field("started_at", Timestamp.class);
    private static final Field<Timestamp> ENDED_AT = DSL.field("ended_at", Timestamp.class);
    private static final Field<Long> DURATION_MS = DSL.field("duration_ms", Long.class);
    private static final Field<String> STATUS = DSL.field("status", String.class);
    private static final Field<String> ERROR_MESSAGE = DSL.field("error_message", String.class);
    private static final Field<String> NODE_ID = DSL.field("node_id", String.class);

    private final DSLContext dsl;

    public UUID insert(String jobName, String jobGroup, String triggerName, String triggerGroup,
                        Instant startedAt, String nodeId) {
        UUID id = UUID.randomUUID();
        dsl.insertInto(TABLE)
                .set(ID, id)
                .set(JOB_NAME, jobName)
                .set(JOB_GROUP, jobGroup)
                .set(TRIGGER_NAME, triggerName)
                .set(TRIGGER_GROUP, triggerGroup)
                .set(STARTED_AT, Timestamp.from(startedAt))
                .set(STATUS, "RUNNING")
                .set(NODE_ID, nodeId)
                .execute();
        return id;
    }

    public void markCompleted(UUID id, Instant endedAt, long durationMs, String status, String errorMessage) {
        dsl.update(TABLE)
                .set(ENDED_AT, Timestamp.from(endedAt))
                .set(DURATION_MS, durationMs)
                .set(STATUS, status)
                .set(ERROR_MESSAGE, errorMessage)
                .where(ID.eq(id))
                .execute();
    }

    public PageResponse<JobExecutionHistoryResponse> findAll(PageRequest pageRequest,
                                                              List<String> jobNameFilter,
                                                              String statusFilter) {
        List<Condition> conditions = new ArrayList<>();
        if (jobNameFilter != null && !jobNameFilter.isEmpty()) {
            conditions.add(JOB_NAME.in(jobNameFilter));
        }
        if (statusFilter != null && !statusFilter.isBlank()) {
            conditions.add(STATUS.eq(statusFilter));
        }

        Condition where = conditions.isEmpty() ? DSL.trueCondition() : DSL.and(conditions);

        int total = dsl.fetchCount(dsl.selectFrom(TABLE).where(where));

        SortField<?> sortField = STARTED_AT.desc();
        if (pageRequest.sort() != null) {
            Field<?> field = switch (pageRequest.sort()) {
                case "jobName" -> JOB_NAME;
                case "status" -> STATUS;
                case "durationMs" -> DURATION_MS;
                default -> STARTED_AT;
            };
            sortField = pageRequest.direction() == SortDirection.ASC ? field.asc() : field.desc();
        }

        var records = dsl.selectFrom(TABLE)
                .where(where)
                .orderBy(sortField)
                .limit(pageRequest.size())
                .offset(pageRequest.offset())
                .fetch();

        List<JobExecutionHistoryResponse> items = records.map(r -> new JobExecutionHistoryResponse(
                r.get(ID).toString(),
                r.get(JOB_NAME),
                r.get(JOB_GROUP),
                formatTimestamp(r.get(STARTED_AT)),
                formatTimestamp(r.get(ENDED_AT)),
                r.get(DURATION_MS),
                r.get(STATUS),
                r.get(ERROR_MESSAGE),
                r.get(NODE_ID)
        ));


        return PageResponse.of(items, pageRequest.page(), pageRequest.size(), total);
    }

    public int deleteOlderThan(Instant cutoff) {
        return dsl.deleteFrom(TABLE)
                .where(STARTED_AT.lt(Timestamp.from(cutoff)))
                .execute();
    }

    private String formatTimestamp(Timestamp ts) {
        if (ts == null) {
            return null;
        }
        return ts.toInstant().atOffset(UTC).format(ISO_OFFSET_DATE_TIME);
    }
}
