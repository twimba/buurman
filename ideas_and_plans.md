------------------------
  TO Verify
------------------------

 ## Create a backoffice application
   This should be where the admin can manage the application, and where we can have all the features that are not available to the users, etc
   This should be a separate application from the landlord portal and tenant portal, etc
    This should be only accessible to buurman admin users, etc

    Let's create a backoffice application!
The app is the public frontend to be used by the buurman users and the backoffice is to be used by buurman staff to manage the platform.

This backfoffice applciation should be the same technology stack as the app (ie; react) and use the api for the logic.
Main features and requirements:
 * backoffice users (let's call them buurmies or buurmy (singular)) have accounts in Keycloak - but segregated from the buurman.io users
 * Strong security is needed
 * Complete isolation between the backoffice and the app
 * No app user should be able to use anytihing from the backoffice
 * The api/backend is still the source of of all the logic
 * The endpoints neessary for the backoffice should be segreated from the endpoints needed for the app (ie: with a '/backoffice' prefix)
 * the backoffice endpoints can not be shown in the swagger api/ui with the other endpoints
 * There should be a specific bakcoffice swagger api/ui for the backoffice api
 * The Look&feel of both the app and the backoffice should be the same – maybe we can have a shared component for that? 
 * the backoffice is to be deployed as another app (docker compose for example)
 * The only feature for now is to list and manage (edit name and delete) teams 


In the backoffice, add a section where we can manage users. In this section we want at least:
 * List all users (in a table, paged, searcheable, orderable)
 * View user details
 * Force reset password
 * Make user active or inactive
 * View all notifications (in a table, paged, searcheable, orderable); possibility to view details and resend
 * While the data should not be limited to a specific team; the buurmy should be able to filter the data by team
 * Add a dashboard to the backoffice with the main information about the platform from a backoffice PoV
 * Add a section with links to grafana, twilio, sendgrid (mailpit in local development), AWS S3 (localstack?), keycloak, traefik, prometheus
  ** Can these websites open in the area to the right of the navigation panel and lookg like they are "part of the app"??

------------------------
  QUEUE
------------------------

# Notifications & communication
 ## Tenant-Landlord Communication
 Messaging center where tenants and landlods can communicate - like a persistent chat, but ominichannel - email, web, sms, etc
 
# Tenant Portal
  ## New application for tenants
  A new application - Tenant portal where tenants can:
   * see their profile, documents, contracts, paymnents, configure notifications, etc 
   * see the history of their interactions with the landlord (ie: messages, notifications, etc)
   * be able to communicate with the landlord (ie: send messages, etc)
   * see the history of their payments and download invoices, etc
   * Send requests to the landlord (ie: maintenance requests, etc) and track their status
   * View payment instructions
 
# Infrastructure
 
 ## Application domain
 Have the applications be on their own subdomain:
  * Main application (landlord) portal: app.buurman.io
  * tenant portal: tenant.buurman.io
  * backoffice: backoffice.buurman.io
  * public website: www.buurman.io

 ## Feature flags
  Implement a feature flag system to be able to enable/disable features in the application without having to deploy new code. This will allow us to test new features with a subset of users, and to quickly disable features if we find any issues with them.
  The feature flag system should be extensible to support different types of flags (ie: boolean flags, percentage flags, etc) and different providers (ie: launchdarkly, etc) and we want to be able to easily add support for them without having to change the existing code too much.
  The feature flag system should also be integrated with the subscription plans, so that we can enable/disable features based on the user's subscription plan, etc
  It is BEST TO USE AN OFF THE SHELF - open source/free SOLUTION FOR THIS! - First determine which one should be used!

## Build docker images
 User github actions to build docker images for the different applications (ie: landlord portal, tenant portal, backoffice, etc) and push them to a container registry (ie: github registry hub, etc) so that they can be easily deployed to production, etc
 The registry should be private

## 

# User management
  ## Verify phone number
   Verify phone number when user signs up to using SMS verification code using Twilio.
   
# UI/UX
 ## Review Template
  Review the UI template and make sure it is consistent across the different pages and applications, and that it follows good UI/UX practices, etc
  Make the dark mode work across the entire application, and make sure it is consistent across the different pages and applications, etc
 
 ## Multi language, I18n and L10n
  Support multiple langauges int he UI of the application and the tenant portal\
  Tenants and landlords can configure their preferred language in their profile, and the application should be displayed in that language for them and send notifications in that language, etc
  The team has a default language that is set as a default for all users and tenants assocaited to the team
  The default language is english

# Marketing website
 ## review public website 
 Remove footer links

 ## Expose demo
  Add link to demo account in public website
  Make it part of the public website


# Buurman subscriptions
 ## Define subscriptions plans
  Determine the base subscription plans we want to support

 ## Subscription management and backoffice
  M anage subscriptions and their capabilities - ie: which features are available in which subscription plans, etc
  * in the backoffice
  * Have active subscriptions (ie: customers can subscribe) and inactive subscriptions (ie: customers cannot subscribe, but existing ones are stil active)
  * be able to set which features and flags each subscription plan has access to
  * Visualize which customers are in which plan
  * Track over time when customers change their subscription plan, etc
  * Track over time the number of users in each subscription plan, etc
  * Keep history of any change to the subscription plans, and of users moving between them
  * have a special view for users that move from a _free_ plan to a _paid_ plan, to see how many users are converting from free to paid, etc

  ## Enforce subscription plans and feature flags
  Enforce the subscription plans and feature flags in the application, so that users can only access the features that are available in their subscription plan, and that are enabled by the feature flags, etc
  * This should be done in a way that is extensible and maintainable, so that we can easily add new features and subscription plans in the future without having to change the existing code too much, etc
  * This should also be done in a way that is performant and does not add too much overhead to the application, etc
  
  ## Ad based subscription plan
   Have an ad based subscription plan where users can use the application for free but they will see ads in the application, etc
   This should be controlled by a feature flag and a subscription flag, so that we can easily enable/disable it for different users and subscription plans, etc
   We can use an ad provider like google ads or similar to serve ads in the application, etc
   We should also track the performance of the ads and how they are affecting the user experience, etc
   The adds should be on the navigation bar
   Use goolge ads to servce the ads, and track the performance of the ads using google analytics, etc?

# Backoffice application

  ## Feaures of the backoffice application
   Have a backoffice application to manage
    * plans
    * subscriptions
    * users
    * teams
    * notifications
    * execute on behalf of user

# Code & Architecture
 ## Execute a full backend architectural, coding and engineering review
  Produce a report with findings, recomendations and action plan to address any issues found in the review
 ## Execute a full frontend architectural, coding and engineering review
  Produce a report with findings, recomendations and action plan to address any issues found in the review
 ## Execute a full infrasctructure architectural, coding and engineering review
  Produce a report with findings, recomendations and action plan to address any issues found in the review
 ## Execute a full backend security review
  Produce a report with findings, recomendations and action plan to address any issues found in the review
 ## Execute a full frontend security review
  Produce a report with findings, recomendations and action plan to address any issues found in the review
 ## Execute a full infrastructure security review
  Produce a report with findings, recomendations and action plan to address any issues found in the review
 
# Tests
 LOL 😂