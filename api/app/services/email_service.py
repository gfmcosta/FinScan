import smtplib
import ssl
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from app.core.config import settings

def send_reset_code_email(email_to: str, code: str):
    message = MIMEMultipart()
    message["From"] = settings.emails_from
    message["To"] = email_to
    message["Subject"] = "FinScan - Código de Recuperação"

    body = f"""
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
    message.attach(MIMEText(body, "html"))

    # Brevo funciona melhor na porta 587 com STARTTLS
    try:
        print(f"Railway: Tentando enviar para {email_to} via {settings.smtp_server}:587")
        # Usamos o contexto SSL padrão
        context = ssl.create_default_context()

        with smtplib.SMTP(settings.smtp_server, 587, timeout=20) as server:
            server.ehlo()
            server.starttls(context=context)
            server.ehlo()
            server.login(settings.smtp_user, settings.smtp_password)
            server.send_message(message)
            print(f"Sucesso absoluto no envio para {email_to}")
        return True
    except Exception as e:
        print(f"Erro no envio via 587: {str(e)}")
        # Última tentativa na porta 2525 (Brevo suporta esta porta alternativa para evitar bloqueios)
        try:
            print(f"Tentando porta alternativa 2525 para {email_to}...")
            with smtplib.SMTP(settings.smtp_server, 2525, timeout=20) as server:
                server.starttls(context=ssl.create_default_context())
                server.login(settings.smtp_user, settings.smtp_password)
                server.send_message(message)
                print(f"Sucesso via porta 2525")
            return True
        except Exception as e2:
            print(f"Todas as tentativas falharam (587, 465, 2525). Erro final: {str(e2)}")
            return False
