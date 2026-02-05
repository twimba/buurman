# Fix Progress Tracker

## Critical Issues (8)
- [x] 1.1 TenantAddressRepository UPDATE missing team_id in WHERE ✅ commit 6dca8ff
- [x] 1.2 TeamMemberRepository/TeamInvitationRepository findById without team_id ✅ commit fa884da
- [x] 2.1 TeamRepository.save() sets CREATED_BY instead of UPDATED_BY ✅ commit 7a7a435
- [x] 4.1 DocumentController missing @PreAuthorize ✅ commit fcbf3a0
- [x] 4.2 GlobalExceptionHandler leaks exception messages ✅ commit 1493477
- [x] 4.3 Hardcoded credentials in application.yml ✅ commit 7349126 (also 4.8)
- [ ] 5.1 Response DTOs expose UUIDs (Large — deferred to Phase 4)
- [x] 4.6 DocumentController bulkDownload no size limit ✅ commit 5350e1d

## High Priority Issues (17)
- [x] 1.3 TenantController address endpoints ignore tenantId ✅ commit 9a99d26
- [x] 2.2 Hard deletes in 4 repositories ✅ commit 55f85ae
- [x] 2.3 AuthService registration no compensation on DB failure ✅ commit 8a9d804
- [x] 3.1 PropertyService missing @Transactional on writes ✅ commit 485876c
- [x] 3.2 PaymentSchedulingService giant @Transactional ✅ commit 4d2ab08
- [x] 4.4 Duplicate CORS config, wildcard headers ✅ commit 9cb3853
- [x] 4.5 MIME type validation relies on Content-Type header only ✅ commit a8f5797
- [x] 10.1 CreatePaymentRequest currency missing @NotNull ✅ commit 87aa72c
- [x] 10.5 No cross-field date validation on DTOs ✅ commit 7e22fab
- [x] 10.7 Financial BigDecimal fields missing @Positive ✅ commit 8e654a5
- [x] 11.2 Update mappers missing nullValuePropertyMappingStrategy=IGNORE ✅ commit 7a66ed7 (also 11.3)
- [x] 6.1 N+1 in PaymentService.enrichPaymentResponse ✅
- [x] 6.2 N+1 in TeamService.getTeamMembers ✅
- [x] 6.3 N+1 in ReportService ✅
- [ ] 7.1 No pagination on any endpoint (Large — deferred)
- [ ] 12.2 Outdated Keycloak/AWS SDK dependencies (Medium — TODO)
- [ ] 14.1 Zero test coverage (Large — deferred)
