import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from app.core.config import settings

def send_reset_code_email(email_to: str, code: str):
    message = MIMEMultipart()
    message["From"] = settings.emails_from
    message["To"] = email_to
    message["Subject"] = "FinScan - Password Reset Code"

    body = f"""
    <html>
        <body style="font-family: sans-serif;">
            <h2>Recuperação de Password</h2>
            <p>Solicitou a redefinição da sua password no FinScan. Utilize o código abaixo:</p>
            <h1 style="color: #3F51B5; letter-spacing: 5px; background: #f4f4f4; padding: 10px; text-align: center;">{code}</h1>
            <p>Este código expira em 15 minutos.</p>
            <p>Se não solicitou isto, ignore este e-mail.</p>
        </body>
    </html>
    """
    message.attach(MIMEText(body, "html"))

    try:
        with smtplib.SMTP(settings.smtp_server, settings.smtp_port, timeout=10) as server:
            server.set_debuglevel(1)  # Ativa logs detalhados no terminal/logs do Railway
            server.starttls()
            server.login(settings.smtp_user, settings.smtp_password)
            server.send_message(message)
        print(f"E-mail enviado com sucesso para {email_to}")
        return True
    except smtplib.SMTPAuthenticationError:
        print("Erro de Autenticação SMTP: Verifique o SMTP_USER e SMTP_PASSWORD.")
        return False
    except Exception as e:
        print(f"Erro ao enviar e-mail: {type(e).__name__}: {e}")
        return False
