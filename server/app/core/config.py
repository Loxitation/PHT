"""Application configuration via environment variables (.env)."""
from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # Database
    postgres_host: str = "postgres"
    postgres_port: int = 5432
    postgres_db: str = "pht"
    postgres_user: str = "pht"
    postgres_password: str = "changeme"

    # JWT auth (dashboard login)
    jwt_secret: str = "changeme-generate-a-real-secret"
    jwt_algorithm: str = "HS256"
    access_token_expire_minutes: int = 15
    refresh_token_expire_days: int = 30

    # Rate limiting on login endpoint
    login_rate_limit: str = "5/minute"

    # Gotify notifications (optional, configured later)
    gotify_url: str = ""
    gotify_token: str = ""
    gotify_enabled: bool = False

    # App metadata
    app_name: str = "PHT Server"
    log_level: str = "INFO"

    @property
    def database_url(self) -> str:
        return (
            f"postgresql+psycopg://{self.postgres_user}:{self.postgres_password}"
            f"@{self.postgres_host}:{self.postgres_port}/{self.postgres_db}"
        )


@lru_cache
def get_settings() -> Settings:
    return Settings()
