Feature: Multi-tenant data isolation
  As the system
  I must guarantee that data from one team
  is never accessible to another team
  So that landlord data remains private and secure

  This is the single most critical invariant in the system.
  Every entity type must be verified for cross-team isolation.

  @critical @multi_tenancy
  Scenario: Properties are isolated between teams
    Given a new team "Alpha" is created
    And I am logged in as admin of team "Alpha"
    And a property "Alpha Building" exists in "Amsterdam"
    And a new team "Beta" is created
    And I am logged in as admin of team "Beta"
    When I list all properties
    Then I should see 0 items in the list

  @critical @multi_tenancy
  Scenario: Direct property access by identifier is blocked across teams
    Given a new team "Alpha" is created
    And I am logged in as admin of team "Alpha"
    And a property "Alpha Building" exists in "Amsterdam"
    And I store the property identifier
    And a new team "Beta" is created
    And I am logged in as admin of team "Beta"
    When I try to retrieve a property with the stored identifier
    Then the response status should be 404

  @critical @multi_tenancy
  Scenario: Contacts are isolated between teams
    Given a new team "Alpha" is created
    And I am logged in as admin of team "Alpha"
    And a contact "Alpha Tenant" exists
    And a new team "Beta" is created
    And I am logged in as admin of team "Beta"
    When I list all contacts
    Then I should see 0 items in the list

  @critical @multi_tenancy
  Scenario: Direct contact access by identifier is blocked across teams
    Given a new team "Alpha" is created
    And I am logged in as admin of team "Alpha"
    And a contact "Alpha Tenant" exists
    And I store the contact identifier
    And a new team "Beta" is created
    And I am logged in as admin of team "Beta"
    When I try to retrieve a contact with the stored identifier
    Then the response status should be 404

  @critical @multi_tenancy
  Scenario: Contracts are isolated between teams
    Given a new team "Alpha" is created
    And I am logged in as admin of team "Alpha"
    And a property "Alpha Building" exists in "Amsterdam"
    And a contact "Alpha Tenant" exists
    And a draft contract exists for the property with tenant "Alpha Tenant"
    And I store the contract identifier
    And a new team "Beta" is created
    And I am logged in as admin of team "Beta"
    When I list all contracts
    Then I should see 0 items in the list

  @critical @multi_tenancy
  Scenario: Payments are isolated between teams
    Given a new team "Alpha" is created
    And I am logged in as admin of team "Alpha"
    And a property "Alpha Building" exists in "Amsterdam"
    And a contact "Alpha Tenant" exists
    And an active contract exists with monthly rent of 1250.00
    And a pending payment of 1250.00 EUR exists due on "in 30 days"
    And a new team "Beta" is created
    And I am logged in as admin of team "Beta"
    When I list all payments
    Then I should see 0 items in the list

  @critical @multi_tenancy
  Scenario: Expenses are isolated between teams
    Given a new team "Alpha" is created
    And I am logged in as admin of team "Alpha"
    And a property "Alpha Building" exists in "Amsterdam"
    And an expense of 500.00 EUR exists for the property
    And a new team "Beta" is created
    And I am logged in as admin of team "Beta"
    When I list all expenses
    Then I should see 0 items in the list

  @multi_tenancy
  Scenario: Cross-team property modification is blocked
    Given a new team "Alpha" is created
    And I am logged in as admin of team "Alpha"
    And a property "Alpha Building" exists in "Amsterdam"
    And I store the property identifier
    And a new team "Beta" is created
    And I am logged in as admin of team "Beta"
    When I try to update a property with the stored identifier
    Then the response status should be 404

  @multi_tenancy
  Scenario: Cross-team property deletion is blocked
    Given a new team "Alpha" is created
    And I am logged in as admin of team "Alpha"
    And a property "Alpha Building" exists in "Amsterdam"
    And I store the property identifier
    And a new team "Beta" is created
    And I am logged in as admin of team "Beta"
    When I try to delete a property with the stored identifier
    Then the response status should be 404

  @multi_tenancy
  Scenario: Cross-team deletion does not affect original team data
    Given a new team "Alpha" is created
    And I am logged in as admin of team "Alpha"
    And a property "Alpha Building" exists in "Amsterdam"
    And I store the property identifier
    And a new team "Beta" is created
    And I am logged in as admin of team "Beta"
    When I try to delete a property with the stored identifier
    Then the response status should be 404
    Given I am logged in as admin of team "Alpha"
    When I list all properties
    Then I should see 1 items in the list
