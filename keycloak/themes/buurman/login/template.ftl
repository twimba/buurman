<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <#if properties.meta?has_content>
        <#list properties.meta?split(' ') as meta>
            <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
        </#list>
    </#if>
    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico" />
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:opsz,wght@14..32,100..900&display=swap" rel="stylesheet">
    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
</head>

<body>
  <div class="kc-page-container">
    <!-- Left Side - Branding (hidden on mobile) -->
    <div class="kc-branding-panel">
      <div class="kc-branding-content">
        <div class="kc-branding-header">
          <img src="${url.resourcesPath}/img/logo_square.png" alt="Buurman" class="kc-branding-logo" />
          <h1 class="kc-branding-title">Buurman</h1>
        </div>
        <p class="kc-branding-tagline">Property management made simple for small landlords</p>

        <!-- Features -->
        <div class="kc-features">
          <div class="kc-feature">
            <div class="kc-feature-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"/><path d="M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></svg>
            </div>
            <div class="kc-feature-text">
              <h3>Manage Properties</h3>
              <p>Keep track of all your rental properties in one place</p>
            </div>
          </div>

          <div class="kc-feature">
            <div class="kc-feature-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><path d="M16 3.128a4 4 0 0 1 0 7.744"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><circle cx="9" cy="7" r="4"/></svg>
            </div>
            <div class="kc-feature-text">
              <h3>Track Tenants</h3>
              <p>Manage tenant information and lease agreements</p>
            </div>
          </div>

          <div class="kc-feature">
            <div class="kc-feature-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z"/><path d="M14 2v5a1 1 0 0 0 1 1h5"/><path d="M10 9H8"/><path d="M16 13H8"/><path d="M16 17H8"/></svg>
            </div>
            <div class="kc-feature-text">
              <h3>Handle Finances</h3>
              <p>Monitor payments, expenses, and financial reports</p>
            </div>
          </div>

          <div class="kc-feature">
            <div class="kc-feature-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 7h6v6"/><path d="m22 7-8.5 8.5-5-5L2 17"/></svg>
            </div>
            <div class="kc-feature-text">
              <h3>Grow Your Business</h3>
              <p>Scale your rental portfolio with confidence</p>
            </div>
          </div>
        </div>

        <div class="kc-branding-footer">
          © 2026 Buurman. Simple property management.
        </div>
      </div>
    </div>

    <!-- Right Side - Login Form -->
    <div class="kc-form-panel">
      <div class="kc-form-container">
        <!-- Mobile Logo (shown only on mobile) -->
        <div class="kc-mobile-logo">
          <img src="${url.resourcesPath}/img/logo_square.png" alt="Buurman" class="kc-mobile-logo-img" />
          <h1 class="kc-mobile-title">Buurman</h1>
          <p class="kc-mobile-tagline">Property management for small landlords</p>
        </div>

        <div class="kc-card">
          <div class="kc-card-header">
            <#nested "header">
          </div>

          <#-- Display message (success/error/warning/info) -->
          <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
              <div class="kc-alert kc-alert-${message.type}">
                  ${kcSanitize(message.summary)?no_esc}
              </div>
          </#if>

          <#-- Main form content -->
          <#nested "form">

          <#-- Registration link -->
          <#if realm.password>
              <div class="kc-registration">
                  <#if client?? && client.baseUrl?has_content>
                      <span>Don't have an account? <a href="${client.baseUrl}/register" class="kc-link">Create one here</a></span>
                  <#else>
                      <span>Don't have an account? <a href="http://localhost:5173/register" class="kc-link">Create one here</a></span>
                  </#if>
              </div>
          </#if>

          <#-- Security note -->
          <div class="kc-security-note">
            Secure authentication
          </div>

          <#-- Info section -->
          <#if displayInfo>
              <div class="kc-info">
                  <#nested "info">
              </div>
          </#if>
        </div>

        <!-- Additional Info -->
        <div class="kc-help-link">
          Need help? <a href="#" class="kc-link">Contact support</a>
        </div>
      </div>
    </div>
  </div>
</body>
</html>
</#macro>
