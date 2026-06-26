from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app import models  # noqa: F401
from app.core.config import settings
from app.db.base_class import Base


_db_url = settings.database_url_fixed
connect_args = {"check_same_thread": False} if _db_url.startswith("sqlite") else {}
engine = create_engine(_db_url, connect_args=connect_args)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def init_db() -> None:
    Base.metadata.create_all(bind=engine)

    db = SessionLocal()
    try:
        from sqlalchemy import inspect, text
        is_sqlite = _db_url.startswith("sqlite")
        inspector = inspect(engine)

        # category.icon — only needed for SQLite (existing DBs); Postgres gets it from create_all
        if is_sqlite:
            columns = [c["name"] for c in inspector.get_columns("category")]
            if "icon" not in columns:
                db.execute(text("ALTER TABLE category ADD COLUMN icon VARCHAR DEFAULT 'Category'"))
                db.commit()

            # User table new columns (SQLite only — Postgres gets them from create_all)
            user_table = "\"user\""
            user_columns = [c["name"] for c in inspector.get_columns("user")]
            user_migrations = [
                ("avatar", f"ALTER TABLE {user_table} ADD COLUMN avatar TEXT"),
                ("is_verified", f"ALTER TABLE {user_table} ADD COLUMN is_verified BOOLEAN"),
                ("verification_token", f"ALTER TABLE {user_table} ADD COLUMN verification_token VARCHAR"),
                ("pending_email", f"ALTER TABLE {user_table} ADD COLUMN pending_email VARCHAR"),
                ("email_change_token", f"ALTER TABLE {user_table} ADD COLUMN email_change_token VARCHAR"),
            ]
            for col_name, sql in user_migrations:
                if col_name not in user_columns:
                    db.execute(text(sql))
            db.commit()

        existing_categories = db.query(models.Category).first()
        if existing_categories is None:
            default_categories = [
                ("Supermarket", "ShoppingCart"),
                ("Restaurant", "Restaurant"),
                ("Fuel", "LocalGasStation"),
                ("Electronics", "Devices"),
                ("Clothing", "Checkroom"),
                ("Cafe", "LocalCafe"),
                ("Sports", "SportsEsports"),
                ("Transport", "DirectionsCar"),
                ("Entertainment", "Movie"),
                ("Other", "Category"),
            ]
            for name, icon in default_categories:
                db.add(models.Category(name=name, icon=icon, is_default=True, owner_id=None))
            db.commit()

    finally:
        db.close()
