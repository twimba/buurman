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
        <div>
          <div class="kc-branding-header">
            <img src="${url.resourcesPath}/img/logo_square.png" alt="Buurman" class="kc-branding-logo" />
            <h1 class="kc-branding-title">Buurman</h1>
          </div>
          <span class="kc-branding-badge">Backoffice</span>
          <p class="kc-branding-tagline">Internal administration and platform management</p>
        </div>

        <!-- Features -->
        <div class="kc-features">
          <div class="kc-feature">
            <div class="kc-feature-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            </div>
            <div class="kc-feature-text">
              <h3>Platform Administration</h3>
              <p>Manage teams, users, and system configuration</p>
            </div>
          </div>

          <div class="kc-feature">
            <div class="kc-feature-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2" ry="2"/><line x1="3" y1="9" x2="21" y2="9"/><line x1="9" y1="21" x2="9" y2="9"/></svg>
            </div>
            <div class="kc-feature-text">
              <h3>System Monitoring</h3>
              <p>Dashboards, metrics, and service health at a glance</p>
            </div>
          </div>

          <div class="kc-feature">
            <div class="kc-feature-icon">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
            </div>
            <div class="kc-feature-text">
              <h3>Team Oversight</h3>
              <p>View and manage all tenant organizations</p>
            </div>
          </div>
        </div>

        <div class="kc-branding-footer">
          Internal use only. Authorized personnel.
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
          <span class="kc-mobile-badge">Backoffice</span>
          <p class="kc-mobile-tagline">Internal administration</p>
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

          <#-- Security note -->
          <div class="kc-security-note">
            Restricted access — authorized personnel only
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
          Need access? Contact the platform administrator.
        </div>
      </div>
    </div>
  </div>
</body>
</html>
</#macro>
