Feature: "Sign In" feature
  Scenario: As a valid user I can sign in into app
    Given I am on the main screen
    When I click on the SignIn label
    And I enter valid email in email field
    And I enter valid password in password field
    And Done label should be enabled
    And I click on the Done label
    Then I should sign in user successfully