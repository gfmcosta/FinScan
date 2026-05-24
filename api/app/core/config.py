from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "FinScan API"
    api_v1_prefix: str = "/api/v1"

    secret_key: str = ""
    access_token_expire_minutes: int = 60
    algorithm: str = ""

    database_url: str = ""

    gemini_api_key: str = ""
    gemini_model: str = ""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )


settings = Settings()
