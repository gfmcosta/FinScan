import httpx
from app.core.config import settings

def send_reset_code_email(email_to: str, code: str):
    url = "https://api.brevo.com/v3/smtp/email"

    headers = {
        "accept": "application/json",
        "content-type": "application/json",
        "api-key": settings.smtp_password  # Na Brevo, a password SMTP é a API Key
    }

    html_content = f"""
    <html>
        <body style="font-family: Arial, sans-serif; color: #333;">
            <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                <h2 style="color: #3F51B5; text-align: center;">Recuperação de Password</h2>
                <p>Recebemos um pedido para redefinir a sua password no <strong>FinScan</strong>.</p>
                <p>Utilize o código de verificação abaixo:</p>
                <div style="background: #f8f9fa; padding: 20px; text-align: center; border-radius: 5px; margin: 20px 0;">
                    <span style="font-size: 32px; font-weight: bold; color: #3F51B5; letter-spacing: 8px;">{code}</span>
                </div>
                <p>Este código é válido por <strong>15 minutos</strong>.</p>
                <p style="font-size: 12px; color: #777; margin-top: 30px;">Se não solicitou esta alteração, pode ignorar este e-mail em total segurança.</p>
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
        print(f"API Brevo: Tentando enviar e-mail para {email_to} via API HTTP...")
        response = httpx.post(url, headers=headers, json=data, timeout=15)

        if response.status_code in [200, 201, 202]:
            print(f"Sucesso total via API Brevo: {response.status_code}")
            return True
        else:
            print(f"Erro na API Brevo ({response.status_code}): {response.text}")
            return False

    except Exception as e:
        print(f"Erro ao conectar com a API da Brevo: {str(e)}")
        return False
