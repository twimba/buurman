Feature: Contract lifecycle
  As a landlord
  I want contracts to follow a defined lifecycle
  So that I have a clear record of tenancy status

  The contract state machine:
    DRAFT -> ACTIVE -> EXPIRED | TERMINATED
    DRAFT -> PENDING_SIGNATURE -> ACTIVE

  Background:
    Given a new team is created
    And I am logged in as admin of that team
    And a property "Keizersgracht 42" exists in "Amsterdam"
    And a contact "Jan de Vries" exists

  @critical @contracts
  Scenario: Activate a draft contract
    Given a draft contract exists for the property with tenant "Jan de Vries"
    When I change the contract status to "ACTIVE"
    Then the response status should be 200
    And the contract status should be "ACTIVE"

  @critical @contracts
  Scenario: Terminate an active contract
    Given a draft contract exists for the property with tenant "Jan de Vries"
    And the contract is activated
    When I change the contract status to "TERMINATED" with reason "Mutual agreement"
    Then the response status should be 200
    And the contract status should be "TERMINATED"

  @contracts
  Scenario: Expire an active contract
    Given a draft contract exists for the property with tenant "Jan de Vries"
    And the contract is activated
    When I change the contract status to "EXPIRED"
    Then the response status should be 200
    And the contract status should be "EXPIRED"

  @contracts
  Scenario: Move a draft contract to pending signature
    Given a draft contract exists for the property with tenant "Jan de Vries"
    When I change the contract status to "PENDING_SIGNATURE"
    Then the response status should be 200
    And the contract status should be "PENDING_SIGNATURE"

  @contracts
  Scenario: Activate a pending-signature contract
    Given a draft contract exists for the property with tenant "Jan de Vries"
    And the contract status is changed to "PENDING_SIGNATURE"
    When I change the contract status to "ACTIVE"
    Then the response status should be 200
    And the contract status should be "ACTIVE"

  @contracts
  Scenario: Reopen a terminated contract
    Given a draft contract exists for the property with tenant "Jan de Vries"
    And the contract is activated
    And the contract status is changed to "TERMINATED" with reason "Mistake"
    When I reopen the contract
    Then the response status should be 200
    And the contract status should be "DRAFT"

  @contracts
  Scenario: Retrieve contract details shows full information
    Given a draft contract exists for the property with tenant "Jan de Vries"
    When I retrieve the contract by its identifier
    Then the response status should be 200
    And the contract should reference the property "Keizersgracht 42"
    And the contract should have at least 1 party

  @contracts
  Scenario: Delete a draft contract
    Given a draft contract exists for the property with tenant "Jan de Vries"
    When I delete the contract
    Then the response status should be 204
    When I list all contracts
    Then I should see 0 items in the list
