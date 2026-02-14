<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>
    <#if section = "header">
        <h2 class="kc-page-title">Backoffice sign in</h2>
        <p class="kc-page-subtitle">Enter your credentials to access the admin panel</p>
    <#elseif section = "form">
    <div id="kc-form">
      <div id="kc-form-wrapper">
        <#if realm.password>
            <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                <div class="kc-form-group">
                    <label for="username" class="kc-label">
                        <#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if>
                    </label>

                    <input tabindex="1" id="username" class="kc-input" name="username" value="${(login.username!'')}"  type="text" autofocus autocomplete="username"
                           aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                           placeholder="<#if !realm.loginWithEmailAllowed>Username<#elseif !realm.registrationEmailAsUsername>Username or email<#else>Email</#if>"
                    />

                    <#if messagesPerField.existsError('username','password')>
                        <span class="kc-error-message" aria-live="polite">
                                ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                        </span>
                    </#if>

                </div>

                <div class="kc-form-group">
                    <label for="password" class="kc-label">${msg("password")}</label>

                    <input tabindex="2" id="password" class="kc-input" name="password" type="password" autocomplete="current-password"
                           aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                           placeholder="Enter your password"
                    />

                    <#if usernameHidden?? && messagesPerField.existsError('username','password')>
                        <span class="kc-error-message" aria-live="polite">
                                ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                        </span>
                    </#if>

                </div>

                <div class="kc-form-group kc-form-options-wrapper">
                    <div class="kc-form-options">
                        <#if realm.rememberMe && !usernameHidden??>
                            <div class="kc-checkbox">
                                <label>
                                    <#if login.rememberMe??>
                                        <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" checked> ${msg("rememberMe")}
                                    <#else>
                                        <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"> ${msg("rememberMe")}
                                    </#if>
                                </label>
                            </div>
                        </#if>
                        </div>
                        <div class="kc-forgot-password">
                            <#if realm.resetPasswordAllowed>
                                <a tabindex="5" href="${url.loginResetCredentialsUrl}" class="kc-link">${msg("doForgotPassword")}</a>
                            </#if>
                        </div>
                  </div>

                  <div class="kc-form-buttons">
                        <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                        <button tabindex="4" class="kc-button-primary" name="login" id="kc-login" type="submit">
                            <svg class="kc-button-icon" xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                            ${msg("doLogIn")}
                        </button>
                  </div>
            </form>
        </#if>
        </div>
      </div>

    </#if>

</@layout.registrationLayout>
