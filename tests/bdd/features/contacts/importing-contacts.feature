Feature: Importing contacts
  As a landlord
  I want to import contacts from a CSV file
  So that I can quickly onboard many tenants and service providers at once

  Background:
    Given a new team is created
    And I am logged in as admin of that team

  @smoke @imports
  Scenario: Upload a CSV file with header row
    When I upload a CSV file with headers:
      | firstName | lastName  | email               | phone          |
      | Jan       | de Vries  | jan@example.com     | +31612345678   |
      | Maria     | Jansen    | maria@example.com   | +31687654321   |
      | Pieter    | Bakker    | pieter@example.com  | +31611223344   |
    Then the response status should be 200
    And the upload response should contain columns "firstName,lastName,email,phone"
    And the upload response should have 3 total rows
    And the upload response file format should be "CSV"

  @imports
  Scenario: Upload a CSV file without header row
    When I upload a CSV file without headers:
      | Jan    | de Vries  | jan@example.com     |
      | Maria  | Jansen    | maria@example.com   |
    Then the response status should be 200
    And the upload response should contain columns "Column A,Column B,Column C"
    And the upload response should have 2 total rows

  @smoke @imports
  Scenario: Preview import shows CREATE actions for new contacts
    Given a CSV file is uploaded with headers:
      | firstName | lastName  | email               |
      | Jan       | de Vries  | jan@example.com     |
      | Maria     | Jansen    | maria@example.com   |
    When I preview the import with contact type "INDIVIDUAL" and mappings:
      | firstName | firstName |
      | lastName  | lastName  |
      | email     | email     |
    Then the response status should be 200
    And the preview should show 2 to create
    And the preview should show 0 to skip
    And the preview should show 0 errors

  @smoke @imports
  Scenario: Execute import creates contacts
    Given a CSV file is uploaded with headers:
      | firstName | lastName  | email                  |
      | Jan       | de Vries  | jan-imp1@example.com   |
      | Maria     | Jansen    | maria-imp1@example.com |
      | Pieter    | Bakker    | pieter-imp1@example.com|
    When I execute the import with contact type "INDIVIDUAL" and mappings:
      | firstName | firstName |
      | lastName  | lastName  |
      | email     | email     |
    Then the response status should be 200
    And the execute response should have imported 3 contacts
    And the execute response should have skipped 0 contacts
    And the execute response should have 0 errors
    And the execute response should contain an identifier starting with "IMP"
    When I list all contacts
    Then the response status should be 200
    And I should see 3 items in the list

  @imports
  Scenario: Import with duplicate emails skips existing contacts
    Given a contact "Jan de Vries" exists
    And a CSV file is uploaded with headers:
      | firstName | lastName  | email              |
      | Maria     | Jansen    | maria@example.com  |
    When I execute the import with contact type "INDIVIDUAL" and mappings:
      | firstName | firstName |
      | lastName  | lastName  |
      | email     | email     |
    Then the response status should be 200
    And the execute response should have imported 1 contacts
    And the execute response should have skipped 0 contacts
    When I list all contacts
    Then the response status should be 200
    And I should see 2 items in the list

  @imports
  Scenario: Preview detects duplicate emails with existing contacts
    Given a contact "Jan de Vries" exists
    And the existing contact email is stored
    And a CSV file is uploaded with duplicate email
    When I preview the import with the duplicate email mapping
    Then the response status should be 200
    And the preview should show 0 to create
    And the preview should show 1 to skip
    And the preview should show 0 errors

  @imports
  Scenario: Import with invalid data counts errors
    Given a CSV file is uploaded with headers:
      | firstName | lastName | email                  |
      |           |          | noname@example.com     |
      | Valid     | Person   | valid@example.com      |
    When I execute the import with contact type "INDIVIDUAL" and mappings:
      | firstName | firstName |
      | lastName  | lastName  |
      | email     | email     |
    Then the response status should be 200
    And the execute response should have imported 1 contacts
    And the execute response should have 1 errors

  @imports
  Scenario: List imports shows import history
    Given a CSV file is uploaded with headers:
      | firstName | lastName  | email                   |
      | Jan       | de Vries  | jan-list@example.com    |
    And the import is executed with contact type "INDIVIDUAL"
    When I list all imports
    Then the response status should be 200
    And the imports list should contain at least 1 item

  @imports
  Scenario: Get import detail shows imported items
    Given a CSV file is uploaded with headers:
      | firstName | lastName  | email                     |
      | Jan       | de Vries  | jan-detail@example.com    |
      | Maria     | Jansen    | maria-detail@example.com  |
    And the import is executed with contact type "INDIVIDUAL"
    When I get the import detail
    Then the response status should be 200
    And the import detail status should be "COMPLETED"
    And the import detail should have 2 imported rows
    And the import detail should have 2 items

  @smoke @imports
  Scenario: Revert import deletes imported contacts
    Given a CSV file is uploaded with headers:
      | firstName | lastName  | email                     |
      | Jan       | de Vries  | jan-revert@example.com    |
      | Maria     | Jansen    | maria-revert@example.com  |
    And the import is executed with contact type "INDIVIDUAL"
    When I revert the import
    Then the response status should be 200
    And the revert response should show 2 deleted contacts
    When I list all contacts
    Then the response status should be 200
    And I should see 0 items in the list

  @imports
  Scenario: Revert an already-reverted import returns 400
    Given a CSV file is uploaded with headers:
      | firstName | lastName  | email                        |
      | Jan       | de Vries  | jan-double-rev@example.com   |
    And the import is executed with contact type "INDIVIDUAL"
    And the import is reverted
    When I revert the import
    Then the response status should be 400

  @imports
  Scenario: Viewer cannot execute an import
    Given a CSV file is uploaded with headers:
      | firstName | lastName | email                    |
      | Jan       | de Vries | jan-viewer@example.com   |
    And a viewer member is added to the team
    And I am logged in as viewer of that team
    When I execute the import with contact type "INDIVIDUAL" and mappings:
      | firstName | firstName |
      | lastName  | lastName  |
      | email     | email     |
    Then the response status should be 403
