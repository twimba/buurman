Feature: Recording payments
  As a landlord
  I want to track rent payments
  So that I know who has paid and who is overdue

  Background:
    Given a new team is created
    And I am logged in as admin of that team
    And a property "Vondelstraat 15" exists in "Amsterdam"
    And a contact "Jan de Vries" exists
    And an active contract exists with monthly rent of 1250.00

  @smoke @payments
  Scenario: Create a payment record
    When I create a payment with:
      | amount   | 1250.00    |
      | currency | EUR        |
      | dueDate  | in 30 days |
    Then the response status should be 201
    And the response should contain an identifier starting with "PAY"
    And the payment status should be "PENDING"
    And the payment amount should be 1250.00

  @critical @payments
  Scenario: Mark a payment as paid
    Given a pending payment of 1250.00 EUR exists due on "in 30 days"
    When I mark the payment as paid
    Then the response status should be 200
    And the payment status should be "PAID"

  @payments
  Scenario: Record a payment receival
    Given a pending payment of 1250.00 EUR exists due on "in 30 days"
    When I record a receival of 1250.00 on "today"
    Then the response status should be 201
    When I retrieve the payment by its identifier
    Then the payment received amount should be 1250.00

  @payments
  Scenario: Record a partial payment receival
    Given a pending payment of 1250.00 EUR exists due on "in 30 days"
    When I record a receival of 800.00 on "today"
    Then the response status should be 201
    When I retrieve the payment by its identifier
    Then the payment received amount should be 800.00
    And the payment balance should be 450.00

  @payments
  Scenario: Create a pre-paid payment
    When I create a payment with:
      | amount      | 1250.00     |
      | currency    | EUR         |
      | dueDate     | in 60 days  |
      | markAsPaid  | true        |
      | paymentDate | today       |
    Then the response status should be 201
    And the payment status should be "PAID"

  @payments
  Scenario: List payments for the team
    Given a pending payment of 1250.00 EUR exists due on "in 30 days"
    And a pending payment of 1250.00 EUR exists due on "in 60 days"
    When I list all payments
    Then the response status should be 200
    And I should see 2 items in the list

  @payments
  Scenario: Delete a payment
    Given a pending payment of 1250.00 EUR exists due on "in 30 days"
    When I delete the payment
    Then the response status should be 204
    When I list all payments
    Then I should see 0 items in the list
