import httpx
from app.core.config import settings


def _brevo_post(data: dict) -> bool:
    api_key = settings.smtp_password.strip() if settings.smtp_password else ""
    if not api_key:
        print("ERRO: SMTP_PASSWORD vazia.", flush=True)
        return False
    headers = {
        "accept": "application/json",
        "content-type": "application/json",
        "api-key": api_key,
    }
    try:
        with httpx.Client() as client:
            resp = client.post("https://api.brevo.com/v3/smtp/email", headers=headers, json=data, timeout=15)
        if resp.status_code in [200, 201, 202]:
            print(f"BREVO: OK ({resp.status_code})", flush=True)
            return True
        print(f"BREVO ERRO {resp.status_code}: {resp.text}", flush=True)
        return False
    except Exception as e:
        print(f"BREVO EXCEPTION: {e}", flush=True)
        return False


def send_verification_email(email_to: str, token: str, base_url: str):
    link = f"{base_url}/api/v1/auth/verify-email?token={token}"
    html = f"""
    <html><body style="font-family:Arial,sans-serif;color:#333;">
      <div style="max-width:600px;margin:0 auto;padding:20px;border:1px solid #eee;border-radius:10px;">
        <h2 style="color:#3F51B5;text-align:center;">Confirm your FinScan account</h2>
        <p>Thank you for registering! Click the button below to verify your email address.</p>
        <div style="text-align:center;margin:30px 0;">
          <a href="{link}" style="background:#3F51B5;color:#fff;padding:14px 28px;border-radius:8px;text-decoration:none;font-weight:bold;">Verify Email</a>
        </div>
        <p style="color:#999;font-size:12px;">Or copy this link: {link}</p>
      </div>
    </body></html>
    """
    return _brevo_post({
        "sender": {"email": settings.emails_from, "name": "FinScan App"},
        "to": [{"email": email_to}],
        "subject": "FinScan – Verify your email",
        "htmlContent": html,
    })


def send_email_change_email(email_to: str, token: str, base_url: str):
    link = f"{base_url}/api/v1/auth/confirm-email-change?token={token}"
    html = f"""
    <html><body style="font-family:Arial,sans-serif;color:#333;">
      <div style="max-width:600px;margin:0 auto;padding:20px;border:1px solid #eee;border-radius:10px;">
        <h2 style="color:#3F51B5;text-align:center;">Confirm your new email</h2>
        <p>Click below to confirm this address as your new FinScan email.</p>
        <div style="text-align:center;margin:30px 0;">
          <a href="{link}" style="background:#3F51B5;color:#fff;padding:14px 28px;border-radius:8px;text-decoration:none;font-weight:bold;">Confirm new email</a>
        </div>
        <p style="color:#999;font-size:12px;">Or copy this link: {link}</p>
      </div>
    </body></html>
    """
    return _brevo_post({
        "sender": {"email": settings.emails_from, "name": "FinScan App"},
        "to": [{"email": email_to}],
        "subject": "FinScan – Confirm new email address",
        "htmlContent": html,
    })


def send_reset_code_email(email_to: str, code: str):
    print(f"INICIANDO TAREFA: Enviar e-mail para {email_to}", flush=True)
    html = f"""
    <html><body style="font-family:Arial,sans-serif;color:#333;">
      <div style="max-width:600px;margin:0 auto;padding:20px;border:1px solid #eee;border-radius:10px;">
        <h2 style="color:#3F51B5;text-align:center;">Recuperação de Password</h2>
        <p>O seu código de verificação é:</p>
        <div style="background:#f8f9fa;padding:20px;text-align:center;border-radius:5px;margin:20px 0;">
          <span style="font-size:32px;font-weight:bold;color:#3F51B5;letter-spacing:8px;">{code}</span>
        </div>
        <p>Válido por 15 minutos.</p>
      </div>
    </body></html>
    """
    return _brevo_post({
        "sender": {"email": settings.emails_from, "name": "FinScan App"},
        "to": [{"email": email_to}],
        "subject": "FinScan - Código de Recuperação",
        "htmlContent": html,
    })
