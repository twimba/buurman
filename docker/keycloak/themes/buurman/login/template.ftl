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
  <div id="kc-container" class="${properties.kcContainerClass!}">
      <div id="kc-container-wrapper" class="${properties.kcContainerWrapperClass!}">

        <div id="kc-content">
          <div id="kc-content-wrapper">

            <#-- App name / Logo -->
            <div class="kc-logo-text">
              Buurman
            </div>

            <#-- Page title -->
            <#nested "header">

            <#-- Display message (success/error/warning/info) -->
            <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                <div class="alert alert-${message.type}">
                    <#if message.type = 'success'><span class="${properties.kcFeedbackSuccessIcon!}"></span></#if>
                    <#if message.type = 'warning'><span class="${properties.kcFeedbackWarningIcon!}"></span></#if>
                    <#if message.type = 'error'><span class="${properties.kcFeedbackErrorIcon!}"></span></#if>
                    <#if message.type = 'info'><span class="${properties.kcFeedbackInfoIcon!}"></span></#if>
                    <span class="kc-feedback-text">${kcSanitize(message.summary)?no_esc}</span>
                </div>
            </#if>

            <#-- Main form content -->
            <#nested "form">

            <#-- Registration link - always show and redirect to application registration page -->
            <#if realm.password>
                <div id="kc-registration">
                    <#-- Use client's base URL if available, otherwise fallback to localhost -->
                    <#if client?? && client.baseUrl?has_content>
                        <span>${msg("noAccount")} <a tabindex="6" href="${client.baseUrl}/register">${msg("doRegister")}</a></span>
                    <#else>
                        <span>${msg("noAccount")} <a tabindex="6" href="http://localhost:5173/register">${msg("doRegister")}</a></span>
                    </#if>
                </div>
            </#if>

            <#-- Info section -->
            <#if displayInfo>
                <div id="kc-info" class="${properties.kcSignUpClass!}">
                    <div id="kc-info-wrapper" class="${properties.kcInfoAreaWrapperClass!}">
                        <#nested "info">
                    </div>
                </div>
            </#if>

          </div>
        </div>

      </div>
  </div>
</body>
</html>
</#macro>
