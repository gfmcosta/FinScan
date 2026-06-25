from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "FinScan API"
    api_v1_prefix: str = "/api/v1"

    secret_key: str = ""
    access_token_expire_minutes: int = 60
    refresh_token_expire_days: int = 30
    algorithm: str = ""

    database_url: str = ""

    # Public URL used in email links (e.g. https://finscan-production.up.railway.app)
    base_url: str = "http://localhost:8000"

    gemini_api_key: str = ""
    gemini_model: str = ""

    # Email Settings (Brevo)
    smtp_server: str = "smtp-relay.brevo.com"
    smtp_port: int = 587
    smtp_user: str = ""
    smtp_password: str = ""
    emails_from: str = ""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )


settings = Settings()
