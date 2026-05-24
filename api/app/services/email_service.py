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
        <body>
            <h2>Password Reset Request</h2>
            <p>You requested to reset your password. Use the code below to proceed:</p>
            <h1 style="color: #3F51B5; letter-spacing: 5px;">{code}</h1>
            <p>This code will expire in 15 minutes.</p>
            <p>If you didn't request this, please ignore this email.</p>
        </body>
    </html>
    """
    message.attach(MIMEText(body, "html"))

    try:
        with smtplib.SMTP(settings.smtp_server, settings.smtp_port) as server:
            server.starttls()
            server.login(settings.smtp_user, settings.smtp_password)
            server.send_message(message)
        return True
    except Exception as e:
        print(f"Error sending email: {e}")
        return False
