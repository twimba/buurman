# Dev-only settings override: disables Django password validators
# so we can use simple passwords like "buurmy" for local development.
from app.settings.production import *  # noqa: F401,F403

AUTH_PASSWORD_VALIDATORS = []
