Feature: Contact management
  As a landlord
  I want to manage my contacts
  So that I can track tenants, service providers, and other people

  Background:
    Given a new team is created
    And I am logged in as admin of that team

  @smoke @contacts
  Scenario: Create an individual contact
    When I create a contact with:
      | contactType | INDIVIDUAL       |
      | firstName   | Jan              |
      | lastName    | de Vries         |
      | email       | jan@example.com  |
    Then the response status should be 201
    And the response should contain an identifier starting with "CTC"
    And the contact first name should be "Jan"
    And the contact last name should be "de Vries"

  @contacts
  Scenario: Create a company contact
    When I create a contact with:
      | contactType | COMPANY               |
      | companyName | Acme Repairs B.V.     |
      | email       | info@acmerepairs.nl   |
    Then the response status should be 201
    And the contact type should be "COMPANY"

  @contacts
  Scenario: List contacts returns created contacts
    Given a contact "Jan de Vries" exists
    And a contact "Maria Jansen" exists
    When I list all contacts
    Then the response status should be 200
    And I should see 2 items in the list

  @contacts
  Scenario: Retrieve a contact by identifier
    Given a contact "Pieter Bakker" exists
    When I retrieve the contact by its identifier
    Then the response status should be 200
    And the contact first name should be "Pieter"

  @contacts
  Scenario: Update a contact
    Given a contact "Jan de Vries" exists
    When I update the contact email to "jan.devries@example.com"
    Then the response status should be 200
    And the contact email should be "jan.devries@example.com"

  @contacts
  Scenario: Delete a contact (soft delete)
    Given a contact "Jan de Vries" exists
    When I delete the contact
    Then the response status should be 204
    When I list all contacts
    Then I should see 0 items in the list

  @contacts
  Scenario: Add a tag to a contact
    Given a contact "Jan de Vries" exists
    When I add the tag "VIP" to the contact
    Then the response status should be 204

  @contacts
  Scenario: Add a note to a contact
    Given a contact "Jan de Vries" exists
    When I add a note "Prefers email communication" to the contact
    Then the response status should be 201
