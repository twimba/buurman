<#import "template.ftl" as layout>
<@layout.emailLayout>
<h1 style="color: #292524; margin: 0 0 24px 0; font-size: 24px; font-weight: 600;">${msg("passwordResetHeading")}</h1>

<p style="color: #292524; line-height: 1.6; margin: 0 0 16px 0;">
    ${msg("passwordResetIntro")}
</p>

<p style="color: #292524; line-height: 1.6; margin: 0 0 24px 0;">
    ${msg("passwordResetInstruction")}
</p>

<div style="margin: 0 0 32px 0;">
    <a href="${link}" style="display: inline-block; background: #0284c7; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 6px; font-weight: 500;">${msg("passwordResetCta")}</a>
</div>

<p style="color: #57534e; font-size: 14px; margin: 0 0 8px 0;">
    ${msg("passwordResetExpiry", linkExpirationFormatter(linkExpiration))}
</p>

<p style="color: #57534e; font-size: 14px; margin: 0;">
    ${msg("passwordResetIgnore")}
</p>
</@layout.emailLayout>
