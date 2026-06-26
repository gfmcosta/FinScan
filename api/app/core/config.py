from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "FinScan API"
    api_v1_prefix: str = "/api/v1"

    secret_key: str = ""
    access_token_expire_minutes: int = 60
    refresh_token_expire_days: int = 30
    algorithm: str = ""

    database_url: str = ""

    @property
    def database_url_fixed(self) -> str:
        """SQLAlchemy requires 'postgresql://' but Railway provides 'postgres://'."""
        url = self.database_url
        if url.startswith("postgres://"):
            url = "postgresql://" + url[len("postgres://"):]
        return url

    # Public URL used in email links
    base_url: str = "http://localhost:8000"
#     base_url: str = "https://finscan-production.up.railway.app"

    # Admin key for the notifications panel (set in .env)
    admin_key: str = ""

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
