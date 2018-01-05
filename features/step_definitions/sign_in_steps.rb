Given /^I am on the main screen$/ do
  check_element_exists("* text:'Log in'")
end

When /^I click on the SignIn label$/ do
  tap_when_element_exists("* text:'Log in'")
end

Then /^I enter valid email in email field$/ do
  enter_text("* hint:'Email'", "qa@mail.com")
end

Then /^I enter valid password in password field$/ do
  enter_text("* hint:'Password'", "qwerty")
end

Then /^Done label should be enabled$/ do
  if (!query("* text:'Done'", :enabled).first) then
    fail("Done label is not enabled")
  end
end

Then /^I click on the Done label$/ do
  tap_when_element_exists("* text:'Done'")
end

Then /^I should sign in user successfully$/ do
  wait_for_element_exists("* text:'Feed'")
  wait_for_element_exists("* text:'Connect'")
  wait_for_element_exists("* text:'Highlights'")
end