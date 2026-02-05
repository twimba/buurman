# Fix Progress Tracker

## Critical Issues (8)
- [ ] 1.1 TenantAddressRepository UPDATE missing team_id in WHERE
- [ ] 1.2 TeamMemberRepository/TeamInvitationRepository findById without team_id
- [ ] 2.1 TeamRepository.save() sets CREATED_BY instead of UPDATED_BY
- [ ] 4.1 DocumentController missing @PreAuthorize
- [ ] 4.2 GlobalExceptionHandler leaks exception messages
- [ ] 4.3 Hardcoded credentials in application.yml
- [ ] 5.1 Response DTOs expose UUIDs (Large — deferred to Phase 4)
- [ ] 4.6 DocumentController bulkDownload no size limit (High per report, Critical per roadmap)

## High Priority Issues (17)
- [ ] 1.3 TenantController address endpoints ignore tenantId
- [ ] 2.2 Hard deletes in 4 repositories (Team, User, TeamMember, TeamInvitation)
- [ ] 2.3 AuthService registration no compensation on DB failure
- [ ] 3.1 PropertyService missing @Transactional on writes
- [ ] 3.2 PaymentSchedulingService giant @Transactional
- [ ] 4.4 Duplicate CORS config, wildcard headers
- [ ] 4.5 MIME type validation relies on Content-Type header only
- [ ] 4.6 bulkDownload no size limit / streams ZIP in memory
- [ ] 6.1 N+1 in PaymentService.enrichPaymentResponse
- [ ] 6.2 N+1 in TeamService.getTeamMembers
- [ ] 6.3 N+1 in ReportService
- [ ] 7.1 No pagination on any endpoint (Large — deferred)
- [ ] 10.1 CreatePaymentRequest currency missing @NotNull
- [ ] 10.5 No cross-field date validation on DTOs
- [ ] 10.7 Financial BigDecimal fields missing @Positive
- [ ] 11.2 Update mappers missing nullValuePropertyMappingStrategy=IGNORE
- [ ] 12.2 Outdated Keycloak/AWS SDK dependencies
- [ ] 14.1 Zero test coverage (Large — deferred)
