<#macro emailLayout>
<!DOCTYPE html>
<html lang="${locale.language}" dir="${(ltr)?then('ltr','rtl')}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 0; background-color: #fafaf9;">
    <div style="max-width: 600px; margin: 0 auto; padding: 40px 20px;">
        <div style="background-color: #0284c7; border-radius: 8px 8px 0 0; padding: 24px 40px; text-align: center;">
            <span style="font-size: 22px; font-weight: 700; color: #ffffff; letter-spacing: -0.5px;">Buurman</span>
        </div>
        <div style="background: #ffffff; border: 1px solid #e7e5e4; border-top: none; border-radius: 0 0 8px 8px; padding: 40px;">
            <#nested>
        </div>
        <p style="color: #57534e; font-size: 12px; text-align: center; margin: 24px 0 0 0;">
            &copy; ${.now?string('yyyy')} Buurman. ${msg("footerText")}
        </p>
    </div>
</body>
</html>
</#macro>
