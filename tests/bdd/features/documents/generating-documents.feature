Feature: Generating contract documents
  As a landlord
  I want to download PDF documents for contract extensions and rent changes
  So that I have formal records of lease adjustments in multiple languages

  Background:
    Given a new team is created
    And I am logged in as admin of that team
    And a property "Prinsengracht 100" exists in "Amsterdam"
    And a contact "Sophie Jansen" exists
    And a draft contract exists for the property with tenant "Sophie Jansen"
    And the contract is activated

  @documents @critical
  Scenario: Download extension addendum in default language
    Given a contract extension exists
    When I download the extension addendum
    Then the response status should be 200
    And the response should be a PDF document

  @documents
  Scenario: Download extension addendum in Dutch
    Given a contract extension exists
    When I download the extension addendum with language "nl"
    Then the response status should be 200
    And the response should be a PDF document
    And the content-disposition should contain "nl"

  @documents @critical
  Scenario: Download rent increase letter in default language
    Given a contract extension exists
    When I download the rent increase letter
    Then the response status should be 200
    And the response should be a PDF document

  @documents
  Scenario: Download rent increase letter in French
    Given a contract extension exists
    When I download the rent increase letter with language "fr"
    Then the response status should be 200
    And the response should be a PDF document
    And the content-disposition should contain "fr"

  @documents @critical
  Scenario: Download rent change document for a rent period
    When I download the rent change document for the first rent period
    Then the response status should be 200
    And the response should be a PDF document

  @documents
  Scenario: Download rent change document in German
    When I download the rent change document for the first rent period with language "de"
    Then the response status should be 200
    And the response should be a PDF document
    And the content-disposition should contain "de"

  @documents
  Scenario: Viewer can download extension addendum
    Given a viewer member is added to the team
    And a contract extension exists
    And I am logged in as viewer of that team
    When I download the extension addendum
    Then the response status should be 200
    And the response should be a PDF document

  @documents
  Scenario: Viewer can download rent change document
    Given a viewer member is added to the team
    And I am logged in as viewer of that team
    When I download the rent change document for the first rent period
    Then the response status should be 200
    And the response should be a PDF document

  @documents
  Scenario: Extension addendum returns 404 for non-existent extension
    When I download the addendum for a non-existent extension
    Then the response status should be 404

  @documents
  Scenario: Rent change document returns 404 for non-existent period
    When I download the rent change document for a non-existent period
    Then the response status should be 404

  @documents
  Scenario: Unauthenticated user cannot download documents
    Given a contract extension exists
    And I clear my authentication
    When I download the extension addendum
    Then the response status should be 401
