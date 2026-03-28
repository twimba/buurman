Feature: Property management
  As a landlord
  I want to manage my rental properties
  So that I can track my real estate portfolio

  Background:
    Given a new team is created
    And I am logged in as admin of that team

  @smoke @properties
  Scenario: Create a residential property
    When I create a property with:
      | street       | Keizersgracht 42 |
      | city         | Amsterdam        |
      | postalCode   | 1015CR           |
      | country      | NL               |
      | category     | RESIDENTIAL      |
      | type         | APARTMENT        |
    Then the response status should be 201
    And the response should contain an identifier starting with "PRO"
    And the property category should be "RESIDENTIAL"
    And the property type should be "APARTMENT"
    And the property status should be "VACANT"

  @properties
  Scenario: Create a commercial property
    When I create a property with:
      | street       | Herengracht 100  |
      | city         | Amsterdam        |
      | postalCode   | 1015BS           |
      | country      | NL               |
      | category     | COMMERCIAL       |
      | type         | OFFICE           |
    Then the response status should be 201
    And the property category should be "COMMERCIAL"
    And the property type should be "OFFICE"

  @properties
  Scenario: List properties returns created properties
    Given a property "Keizersgracht 42" exists in "Amsterdam"
    And a property "Herengracht 100" exists in "Amsterdam"
    When I list all properties
    Then the response status should be 200
    And I should see 2 items in the list

  @smoke @properties
  Scenario: Retrieve a property by identifier
    Given a property "Prinsengracht 263" exists in "Amsterdam"
    When I retrieve the property by its identifier
    Then the response status should be 200
    And the property street should be "Prinsengracht 263"

  @properties
  Scenario: Update a property
    Given a property "Vondelstraat 15" exists in "Amsterdam"
    When I update the property street to "Vondelstraat 15A"
    Then the response status should be 200
    And the property street should be "Vondelstraat 15A"

  @properties
  Scenario: Soft delete a property
    Given a property "Plantage Middenlaan 1" exists in "Amsterdam"
    When I delete the property
    Then the response status should be 204
    When I list all properties
    Then I should see 0 items in the list

  @properties
  Scenario: Retrieve a non-existent property returns 404
    When I try to retrieve a property with identifier "PRO0000000000000000000000000"
    Then the response status should be 404

  @properties
  Scenario: Create a property with missing required fields returns 400
    When I create a property with:
      | street       | Missing Fields   |
      | city         |                  |
      | postalCode   |                  |
      | country      |                  |
      | category     | RESIDENTIAL      |
      | type         | APARTMENT        |
    Then the response status should be 400

  @properties
  Scenario: List properties supports pagination
    Given 15 properties exist
    When I list properties with page 0 and size 5
    Then the response status should be 200
    And I should see 5 items in the list
    And the total elements should be 15
    And the total pages should be 3
