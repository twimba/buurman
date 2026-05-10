<#ftl output_format="plainText">
${msg("passwordResetHeading")}

${msg("passwordResetIntro")}

${msg("passwordResetInstruction")}

${link}

${msg("passwordResetExpiry", linkExpirationFormatter(linkExpiration))}

${msg("passwordResetIgnore")}
