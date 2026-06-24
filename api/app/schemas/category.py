import re

from pydantic import BaseModel, ConfigDict, field_validator


class CategoryCreate(BaseModel):
    name: str
    icon: str = "Category"

    @field_validator("name")
    @classmethod
    def validate_name(cls, v: str) -> str:
        if re.search(r"\d", v):
            raise ValueError("Category name cannot contain numbers")
        return v


class CategoryUpdate(BaseModel):
    name: str
    icon: str | None = None

    @field_validator("name")
    @classmethod
    def validate_name(cls, v: str) -> str:
        if re.search(r"\d", v):
            raise ValueError("Category name cannot contain numbers")
        return v


class CategoryRead(BaseModel):
    id: int
    name: str
    icon: str = "Category"
    owner_id: int | None = None
    is_default: bool = False

    model_config = ConfigDict(from_attributes=True)
