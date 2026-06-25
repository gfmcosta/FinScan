import json
import re
from datetime import UTC, datetime

from fastapi import HTTPException, status

try:
    import google.generativeai as genai
except ImportError:  # pragma: no cover
    genai = None

from app.core.config import settings
from app.schemas.receipt import ReceiptOCRParsed


BASE_PROMPT = """
Extrai dados do recibo e devolve APENAS JSON válido com este formato:
{
  "store": "nome loja",
  "category": "nome da categoria",
  "total": 12.34,
  "currency": "EUR",
  "purchase_date": "2026-04-09T12:30:00"
}

Regras:
- Sem markdown, sem explicações, só JSON.
- `total` deve ser número decimal.
- Usa formato ISO 8601 para `purchase_date`.
""".strip()


def _build_prompt(existing_categories: list[str] | None = None) -> str:
    if existing_categories:
        cats = ", ".join(f'"{c}"' for c in existing_categories)
        return (
            BASE_PROMPT
            + f"\n- O utilizador já tem estas categorias: [{cats}]. "
            "Escolhe a mais adequada ao recibo. "
            "Só cria uma nova se nenhuma se aplicar.\n"
            "- Se criares uma nova, escreve com a primeira letra maiúscula."
        )
    return (
        BASE_PROMPT
        + "\n- Usa categoria \"Outros\" se não conseguires inferir."
        "\n- Escreve a categoria com a primeira letra maiúscula."
    )


def _extract_json_block(text: str) -> dict:
    match = re.search(r"\{.*\}", text, flags=re.DOTALL)
    if not match:
        raise ValueError("Gemini response does not contain JSON")
    return json.loads(match.group(0))


def _parse_data_uri(payload: str) -> tuple[str | None, str]:
    # Accept both raw base64 and data URI payloads.
    if payload.startswith("data:") and "," in payload:
        header, data = payload.split(",", 1)
        mime_match = re.match(r"^data:([^;]+);base64$", header)
        if mime_match:
            return mime_match.group(1).strip().lower(), data
        return None, data
    return None, payload


def parse_receipt_with_gemini(
    file_base64: str,
    mime_type: str = "image/jpeg",
    existing_categories: list[str] | None = None,
) -> ReceiptOCRParsed:
    detected_mime, raw_payload = _parse_data_uri(file_base64)
    effective_mime = (detected_mime or mime_type or "image/jpeg").lower()

    if effective_mime not in {"image/jpeg", "image/png", "application/pdf"}:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Unsupported mime_type. Use image/jpeg, image/png, or application/pdf",
        )

    if not settings.gemini_api_key:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Missing GEMINI_API_KEY",
        )
    if genai is None:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Missing google-generativeai dependency",
        )

    genai.configure(api_key=settings.gemini_api_key)
    model = genai.GenerativeModel(settings.gemini_model)

    try:
        response = model.generate_content(
            [
                _build_prompt(existing_categories),
                {"mime_type": effective_mime, "data": raw_payload},
            ]
        )
    except Exception as exc:
        message = str(exc)
        if "InvalidArgument" in message:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid file payload for OCR. Check base64 content and mime_type",
            ) from exc
        if "NotFound" in message:
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail="Configured Gemini model is not available for generateContent",
            ) from exc
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Gemini OCR request failed",
        ) from exc

    try:
        payload = _extract_json_block(response.text or "")
        if "purchase_date" not in payload:
            payload["purchase_date"] = datetime.now(UTC).isoformat()
        return ReceiptOCRParsed(**payload)
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Gemini OCR response could not be parsed",
        ) from exc
