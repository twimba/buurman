Feature: Creating contracts
  As a landlord
  I want to create rental contracts
  So that I can formalize tenancy agreements

  Background:
    Given a new team is created
    And I am logged in as admin of that team
    And a property "Keizersgracht 42" exists in "Amsterdam"
    And a contact "Jan de Vries" exists

  @smoke @contracts
  Scenario: Create a fixed-term contract
    When I create a contract with:
      | contractType     | FIXED_TERM    |
      | paymentFrequency | MONTHLY       |
      | rentAmount       | 1250.00       |
      | startDate        | next month    |
      | endDate          | in 12 months  |
    Then the response status should be 201
    And the response should contain an identifier starting with "CON"
    And the contract status should be "DRAFT"
    And the contract type should be "FIXED_TERM"
    And the contract rent amount should be 1250.00

  @contracts
  Scenario: Create an indefinite contract
    When I create a contract with:
      | contractType     | INDEFINITE    |
      | paymentFrequency | MONTHLY       |
      | rentAmount       | 950.00        |
      | startDate        | next month    |
    Then the response status should be 201
    And the contract status should be "DRAFT"
    And the contract type should be "INDEFINITE"

  @contracts
  Scenario: Create a contract with a deposit
    When I create a contract with:
      | contractType     | FIXED_TERM    |
      | paymentFrequency | MONTHLY       |
      | rentAmount       | 1500.00       |
      | depositAmount    | 3000.00       |
      | startDate        | next month    |
      | endDate          | in 12 months  |
    Then the response status should be 201
    And the contract deposit amount should be 3000.00

  @contracts
  Scenario: Create a contract requires a property
    When I create a contract without a property:
      | contractType     | FIXED_TERM    |
      | paymentFrequency | MONTHLY       |
      | rentAmount       | 1250.00       |
      | startDate        | next month    |
    Then the response status should be 400

  @contracts
  Scenario: Create a contract requires at least one party
    When I create a contract without any parties:
      | contractType     | FIXED_TERM    |
      | paymentFrequency | MONTHLY       |
      | rentAmount       | 1250.00       |
      | startDate        | next month    |
    Then the response status should be 400

  @contracts
  Scenario: List contracts returns created contracts
    Given a draft contract exists for the property with tenant "Jan de Vries"
    When I list all contracts
    Then the response status should be 200
    And I should see 1 items in the list
