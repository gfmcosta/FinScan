import httpx
from app.core.config import settings

def send_reset_code_email(email_to: str, code: str):
    print(f"INICIANDO TAREFA: Enviar e-mail para {email_to}", flush=True)

    # Debug da chave (seguro)
    api_key = settings.smtp_password.strip() if settings.smtp_password else ""
    key_len = len(api_key)
    print(f"DEBUG: Tamanho da chave carregada: {key_len} caracteres", flush=True)

    if key_len == 0:
        print("ERRO: A variável SMTP_PASSWORD está vazia! Verifique as variáveis no Railway.", flush=True)
        return False

    url = "https://api.brevo.com/v3/smtp/email"

    headers = {
        "accept": "application/json",
        "content-type": "application/json",
        "api-key": api_key,
        "x-sib-api-key": api_key  # Header alternativo para redundância
    }

    html_content = f"""
    <html>
        <body style="font-family: Arial, sans-serif; color: #333;">
            <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                <h2 style="color: #3F51B5; text-align: center;">Recuperação de Password</h2>
                <p>O seu código de verificação é:</p>
                <div style="background: #f8f9fa; padding: 20px; text-align: center; border-radius: 5px; margin: 20px 0;">
                    <span style="font-size: 32px; font-weight: bold; color: #3F51B5; letter-spacing: 8px;">{code}</span>
                </div>
                <p>Válido por 15 minutos.</p>
            </div>
        </body>
    </html>
    """

    data = {
        "sender": {"email": settings.emails_from, "name": "FinScan App"},
        "to": [{"email": email_to}],
        "subject": "FinScan - Código de Recuperação",
        "htmlContent": html_content
    }

    try:
        print(f"HTTP REQUEST: A enviar para Brevo API (Sender: {settings.emails_from})...", flush=True)
        with httpx.Client() as client:
            response = client.post(url, headers=headers, json=data, timeout=15)

        if response.status_code in [200, 201, 202]:
            print(f"RESPOSTA BREVO: Status {response.status_code} - SUCESSO!", flush=True)
            return True
        else:
            print(f"ERRO API BREVO (Status {response.status_code}): {response.text}", flush=True)
            return False

    except Exception as e:
        print(f"ERRO CRÍTICO NA TAREFA DE E-MAIL: {str(e)}", flush=True)
        return False
