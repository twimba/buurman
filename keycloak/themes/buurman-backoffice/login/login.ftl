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

                    <div class="kc-password-wrapper">
                        <input tabindex="2" id="password" class="kc-input" name="password" type="password" autocomplete="current-password"
                               aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"
                               placeholder="${msg("placeholderPassword")}"
                        />
                        <button type="button" class="kc-password-toggle" tabindex="6" aria-label="${msg("togglePasswordVisibility")}" onclick="togglePasswordVisibility(this)">
                            <svg class="eye-open" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0"/><circle cx="12" cy="12" r="3"/></svg>
                            <svg class="eye-closed" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="display:none"><path d="m6.18 6.18a8.994 8.994 0 0 0-4.118 5.472 1 1 0 0 0 0 .696C3.627 16.126 7.51 19 12 19c1.827 0 3.52-.52 4.96-1.42"/><path d="m10 10 4 4"/><path d="M14.307 4.832A9.139 9.139 0 0 0 12 4.5C7.51 4.5 3.627 7.374 2.062 11.148"/><path d="M21.938 12.348a10.62 10.62 0 0 0-2.118-3.528"/><path d="M2 2l20 20"/><path d="M8.415 8.414a3 3 0 0 0 4.243 4.243"/></svg>
                        </button>
                    </div>

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

      <script>
        function togglePasswordVisibility(btn) {
            var input = btn.parentElement.querySelector('input');
            var eyeOpen = btn.querySelector('.eye-open');
            var eyeClosed = btn.querySelector('.eye-closed');
            if (input.type === 'password') {
                input.type = 'text';
                eyeOpen.style.display = 'none';
                eyeClosed.style.display = 'block';
            } else {
                input.type = 'password';
                eyeOpen.style.display = 'block';
                eyeClosed.style.display = 'none';
            }
        }
      </script>

    </#if>

</@layout.registrationLayout>
