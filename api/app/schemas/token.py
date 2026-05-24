from pydantic import BaseModel


class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"
    name: str | None = None


class TokenPayload(BaseModel):
    sub: str
    role: str
    exp: int
