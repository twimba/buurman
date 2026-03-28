Feature: Role-based access control
  As a team admin
  I want to control what team members can do
  So that viewers cannot accidentally modify data

  Role hierarchy: TEAM_ADMIN > TEAM_EDITOR > TEAM_VIEWER

  @critical @access_control
  Scenario: Admin can create a property
    Given a new team is created
    And I am logged in as admin of that team
    When I create a property with:
      | street       | Herengracht 100  |
      | city         | Amsterdam        |
      | postalCode   | 1015BS           |
      | country      | NL               |
      | category     | RESIDENTIAL      |
      | type         | APARTMENT        |
    Then the response status should be 201

  @critical @access_control
  Scenario: Viewer cannot create a property
    Given a new team is created
    And a viewer member is added to the team
    And I am logged in as viewer of that team
    When I create a property with:
      | street       | Herengracht 100  |
      | city         | Amsterdam        |
      | postalCode   | 1015BS           |
      | country      | NL               |
      | category     | RESIDENTIAL      |
      | type         | APARTMENT        |
    Then the response status should be 403

  @critical @access_control
  Scenario: Viewer can read properties
    Given a new team is created
    And I am logged in as admin of that team
    And a property "Prinsengracht 263" exists in "Amsterdam"
    And a viewer member is added to the team
    And I am logged in as viewer of that team
    When I list all properties
    Then the response status should be 200
    And I should see 1 items in the list

  @access_control
  Scenario: Viewer cannot update a property
    Given a new team is created
    And I am logged in as admin of that team
    And a property "Prinsengracht 263" exists in "Amsterdam"
    And a viewer member is added to the team
    And I am logged in as viewer of that team
    When I try to update the property street to "New Street"
    Then the response status should be 403

  @access_control
  Scenario: Viewer cannot delete a property
    Given a new team is created
    And I am logged in as admin of that team
    And a property "Prinsengracht 263" exists in "Amsterdam"
    And a viewer member is added to the team
    And I am logged in as viewer of that team
    When I try to delete the current property
    Then the response status should be 403

  @access_control
  Scenario: Editor can create a property
    Given a new team is created
    And an editor member is added to the team
    And I am logged in as editor of that team
    When I create a property with:
      | street       | Herengracht 100  |
      | city         | Amsterdam        |
      | postalCode   | 1015BS           |
      | country      | NL               |
      | category     | RESIDENTIAL      |
      | type         | APARTMENT        |
    Then the response status should be 201

  @access_control
  Scenario: Unauthenticated access is rejected
    When I make an unauthenticated request to list properties
    Then the response status should be 401
