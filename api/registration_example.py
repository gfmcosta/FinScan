"""
Exemplo de lógica de registo contra a API FinScan
Base URL: https://finscan-production.up.railway.app
Rota de registo: POST /api/v1/auth/register
"""

import requests
import json
from typing import Optional, Dict, Any


class FinScanRegistration:
    """Cliente para registo e autenticação na FinScan API"""
    
    BASE_URL = "https://finscan-production.up.railway.app"
    REGISTER_ENDPOINT = "/api/v1/auth/register"
    LOGIN_ENDPOINT = "/api/v1/auth/login"
    
    def __init__(self, base_url: Optional[str] = None):
        """
        Inicializa o cliente
        
        Args:
            base_url: URL base da API (default: https://finscan-production.up.railway.app)
        """
        self.base_url = base_url or self.BASE_URL
    
    def register(self, username: str, email: str, password: str, role: str = "user") -> Dict[str, Any]:
        """
        Regista um novo utilizador
        
        Args:
            username: Nome de utilizador único
            email: Email válido do utilizador
            password: Password da conta
            role: Papel do utilizador ('user' ou 'admin', default: 'user')
        
        Returns:
            Dict contendo resposta da API
            
        Raises:
            ValueError: Se os dados forem inválidos
            requests.exceptions.RequestException: Se o pedido falhar
        
        Exemplo:
            >>> client = FinScanRegistration()
            >>> response = client.register(
            ...     username="joao_silva",
            ...     email="joao@example.com",
            ...     password="senha_segura123",
            ...     role="user"
            ... )
            >>> print(response)
            {'id': 1, 'username': 'joao_silva', 'email': 'joao@example.com', 'is_active': True, 'role': 'user'}
        """
        
        # Validações básicas
        if not username or not username.strip():
            raise ValueError("Username não pode estar vazio")
        
        if not email or not email.strip():
            raise ValueError("Email não pode estar vazio")
        
        if not password or not password.strip():
            raise ValueError("Password não pode estar vazia")
        
        if role not in ["user", "admin"]:
            raise ValueError("Role deve ser 'user' ou 'admin'")
        
        # Validar email
        if "@" not in email or "." not in email.split("@")[1]:
            raise ValueError("Email inválido")
        
        # Preparar dados
        payload = {
            "username": username.strip(),
            "email": email.strip(),
            "password": password,
            "role": role
        }
        
        # Fazer pedido
        url = f"{self.base_url}{self.REGISTER_ENDPOINT}"
        
        try:
            response = requests.post(
                url,
                json=payload,
                timeout=10
            )
            
            # Processar resposta
            if response.status_code == 201:
                # Registo bem-sucedido
                return response.json()
            
            elif response.status_code == 400:
                # Erro de validação
                error_detail = response.json().get("detail", "Erro desconhecido")
                raise ValueError(f"Erro de validação: {error_detail}")
            
            elif response.status_code == 422:
                # Erro de validação de schema
                errors = response.json().get("detail", [])
                error_msg = "; ".join([
                    f"{e.get('loc', ['unknown'])[1]}: {e.get('msg', 'erro desconhecido')}"
                    for e in errors
                ])
                raise ValueError(f"Erro de schema: {error_msg}")
            
            else:
                raise Exception(f"Erro da API (status {response.status_code}): {response.text}")
        
        except requests.exceptions.Timeout:
            raise Exception("Timeout ao conectar à API")
        except requests.exceptions.ConnectionError:
            raise Exception("Erro de conexão com a API")
    
    def login(self, username: str, password: str) -> Dict[str, str]:
        """
        Autentica um utilizador e retorna um token JWT
        
        Args:
            username: Nome de utilizador
            password: Password da conta
        
        Returns:
            Dict contendo o token de acesso
            
        Exemplo:
            >>> client = FinScanRegistration()
            >>> token_response = client.login("joao_silva", "senha_segura123")
            >>> print(token_response)
            {'access_token': 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...', 'token_type': 'bearer'}
        """
        
        if not username or not password:
            raise ValueError("Username e password são obrigatórios")
        
        # Usar OAuth2PasswordRequestForm format
        data = {
            "username": username,
            "password": password
        }
        
        url = f"{self.base_url}{self.LOGIN_ENDPOINT}"
        
        try:
            response = requests.post(
                url,
                data=data,  # OAuth2 usa form data, não JSON
                timeout=10
            )
            
            if response.status_code == 200:
                return response.json()
            
            elif response.status_code == 401:
                raise ValueError("Username ou password incorretos")
            
            else:
                raise Exception(f"Erro da API (status {response.status_code}): {response.text}")
        
        except requests.exceptions.Timeout:
            raise Exception("Timeout ao conectar à API")
        except requests.exceptions.ConnectionError:
            raise Exception("Erro de conexão com a API")


def main():
    """Exemplo de utilização"""
    
    client = FinScanRegistration()
    
    # Exemplo 1: Registo bem-sucedido
    print("=" * 60)
    print("EXEMPLO 1: Registo de novo utilizador")
    print("=" * 60)
    
    try:
        user_data = {
            "username": "joao_silva",
            "email": "joao@example.com",
            "password": "senha_super_segura123",
            "role": "user"
        }
        
        print(f"\nTentando registar com os seguintes dados:")
        print(json.dumps(user_data, indent=2, ensure_ascii=False))
        
        response = client.register(**user_data)
        
        print(f"\n✓ Registo bem-sucedido!")
        print(f"Resposta da API:")
        print(json.dumps(response, indent=2, ensure_ascii=False))
        
    except ValueError as e:
        print(f"\n✗ Erro de validação: {e}")
    except Exception as e:
        print(f"\n✗ Erro: {e}")
    
    # Exemplo 2: Tentar registar com username duplicado
    print("\n" + "=" * 60)
    print("EXEMPLO 2: Tentativa de registo com username duplicado")
    print("=" * 60)
    
    try:
        response = client.register(
            username="joao_silva",  # Mesmo username do exemplo anterior
            email="outro_email@example.com",
            password="outra_senha123"
        )
        print(f"\n✓ Registo bem-sucedido: {response}")
        
    except ValueError as e:
        print(f"\n✗ Erro esperado: {e}")
    except Exception as e:
        print(f"\n✗ Erro: {e}")
    
    # Exemplo 3: Login
    print("\n" + "=" * 60)
    print("EXEMPLO 3: Login após registo bem-sucedido")
    print("=" * 60)
    
    try:
        token_response = client.login("joao_silva", "senha_super_segura123")
        
        print(f"\n✓ Login bem-sucedido!")
        print(f"Token de acesso recebido:")
        print(json.dumps({
            "access_token": token_response.get("access_token", "")[:50] + "...",
            "token_type": token_response.get("token_type", "")
        }, indent=2, ensure_ascii=False))
        
    except ValueError as e:
        print(f"\n✗ Erro: {e}")
    except Exception as e:
        print(f"\n✗ Erro: {e}")
    
    # Exemplo 4: Validações - Email inválido
    print("\n" + "=" * 60)
    print("EXEMPLO 4: Validação - Email inválido")
    print("=" * 60)
    
    try:
        response = client.register(
            username="novo_user",
            email="email_invalido",  # Email inválido
            password="senha123"
        )
    except ValueError as e:
        print(f"\n✓ Validação funcionou: {e}")
    except Exception as e:
        print(f"\n✗ Erro: {e}")
    
    # Exemplo 5: Validações - Password vazia
    print("\n" + "=" * 60)
    print("EXEMPLO 5: Validação - Password vazia")
    print("=" * 60)
    
    try:
        response = client.register(
            username="novo_user",
            email="novo@example.com",
            password=""  # Password vazia
        )
    except ValueError as e:
        print(f"\n✓ Validação funcionou: {e}")
    except Exception as e:
        print(f"\n✗ Erro: {e}")


if __name__ == "__main__":
    # Descomentar a linha abaixo para executar os exemplos
    # main()
    
    # Ou usar o cliente manualmente:
    client = FinScanRegistration()
    
    # Registo
    try:
        response = client.register(
            username="test_user",
            email="test@example.com",
            password="test_password123"
        )
        print("Registo bem-sucedido:", response)
    except Exception as e:
        print("Erro:", e)
