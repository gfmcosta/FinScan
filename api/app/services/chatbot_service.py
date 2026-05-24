from fastapi import HTTPException, status

try:
    import google.generativeai as genai
except ImportError:  # pragma: no cover
    genai = None

from app.core.config import settings


SYSTEM_PROMPT = """
Responde em português europeu (exceto quando a pergunta é feita em inglês) sobre despesas, hábitos de consumo e estatísticas do utilizador.
Responde apenas ao que foi perguntado, de forma curta e objetiva.
Nao dês conselhos, nao sugiras proximos passos e nao peças ao utilizador para fazer mais nada.
Se nao houver dados suficientes, responde apenas: "Sem dados suficientes para responder."
""".strip()


def ask_expense_chatbot(user_context: str, question: str) -> str:
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

    prompt = f"{SYSTEM_PROMPT}\n\nContexto do utilizador:\n{user_context}\n\nPergunta:\n{question}"  # noqa: E501
    response = model.generate_content(prompt)
    return response.text.strip()
