from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from app import models  # noqa: F401
from app.core.config import settings
from app.db.base_class import Base


connect_args = (
    {"check_same_thread": False} if settings.database_url.startswith("sqlite") else {}
)
engine = create_engine(settings.database_url, connect_args=connect_args)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def init_db() -> None:
    Base.metadata.create_all(bind=engine)

    db = SessionLocal()
    try:
        existing_categories = db.query(models.Category).first()
        if existing_categories is None:
            default_names = [
                "Supermarket", "Restaurant", "Fuel", "Electronics",
                "Clothing", "Cafe", "Sports", "Transport", "Entertainment", "Other",
            ]
            for name in default_names:
                db.add(models.Category(name=name, is_default=True, owner_id=None))
            db.commit()

    finally:
        db.close()
