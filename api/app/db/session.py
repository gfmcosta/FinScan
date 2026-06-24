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
        from sqlalchemy import inspect, text
        inspector = inspect(engine)
        columns = [c["name"] for c in inspector.get_columns("category")]
        if "icon" not in columns:
            db.execute(text("ALTER TABLE category ADD COLUMN icon VARCHAR DEFAULT 'Category'"))
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
