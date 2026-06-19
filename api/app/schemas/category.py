from pydantic import BaseModel, ConfigDict


class CategoryCreate(BaseModel):
    name: str


class CategoryUpdate(BaseModel):
    name: str


class CategoryRead(BaseModel):
    id: int
    name: str
    owner_id: int | None = None
    is_default: bool = False

    model_config = ConfigDict(from_attributes=True)
