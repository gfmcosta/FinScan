# FinScan API - Documentação de Registo e Autenticação

## Base URL
```
https://finscan-production.up.railway.app
```

## Endpoints de Autenticação

### 1. Registo de Novo Utilizador
**POST** `/api/v1/auth/register`

#### Request Body
```json
{
  "username": "joao_silva",
  "email": "joao@example.com",
  "password": "senha_segura123",
  "role": "user"  // "user" ou "admin", default: "user"
}
```

#### Validações
- ✓ `username`: Não pode estar vazio, deve ser único
- ✓ `email`: Deve ser um email válido, deve ser único
- ✓ `password`: Não pode estar vazia
- ✓ `role`: Deve ser "user" ou "admin"

#### Response (201 Created)
```json
{
  "id": 1,
  "username": "joao_silva",
  "email": "joao@example.com",
  "role": "user",
  "is_active": true
}
```

#### Errors

**400 Bad Request** - Validação falhou
```json
{
  "detail": "Username or email already exists"
}
```

**422 Unprocessable Entity** - Erro de schema
```json
{
  "detail": [
    {
      "loc": ["body", "username"],
      "msg": "field required",
      "type": "value_error.missing"
    }
  ]
}
```

### 2. Login
**POST** `/api/v1/auth/login`

#### Request Body (Form Data)
```
username=joao_silva&password=senha_segura123
```

Ou em JSON (alguns clientes):
```json
{
  "username": "joao_silva",
  "password": "senha_segura123"
}
```

#### Response (200 OK)
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer"
}
```

#### Errors

**401 Unauthorized** - Credenciais incorretas
```json
{
  "detail": "Incorrect username or password"
}
```

## Exemplos de Utilização

### Python
```python
import requests

# Registo
response = requests.post(
    "https://finscan-production.up.railway.app/api/v1/auth/register",
    json={
        "username": "joao_silva",
        "email": "joao@example.com",
        "password": "senha_segura123",
        "role": "user"
    }
)

if response.status_code == 201:
    user = response.json()
    print(f"Utilizador criado: {user}")
else:
    print(f"Erro: {response.json()}")

# Login
login_response = requests.post(
    "https://finscan-production.up.railway.app/api/v1/auth/login",
    data={
        "username": "joao_silva",
        "password": "senha_segura123"
    }
)

if login_response.status_code == 200:
    token = login_response.json()
    access_token = token["access_token"]
    print(f"Token: {access_token}")
```

### JavaScript/TypeScript
```javascript
// Registo
const registerResponse = await fetch(
  "https://finscan-production.up.railway.app/api/v1/auth/register",
  {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      username: "joao_silva",
      email: "joao@example.com",
      password: "senha_segura123",
      role: "user"
    })
  }
);

if (registerResponse.status === 201) {
  const user = await registerResponse.json();
  console.log("Utilizador criado:", user);
}

// Login
const loginResponse = await fetch(
  "https://finscan-production.up.railway.app/api/v1/auth/login",
  {
    method: "POST",
    body: new URLSearchParams({
      username: "joao_silva",
      password: "senha_segura123"
    })
  }
);

if (loginResponse.status === 200) {
  const token = await loginResponse.json();
  localStorage.setItem("access_token", token.access_token);
  console.log("Login bem-sucedido!");
}
```

### cURL
```bash
# Registo
curl -X POST https://finscan-production.up.railway.app/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "joao_silva",
    "email": "joao@example.com",
    "password": "senha_segura123",
    "role": "user"
  }'

# Login
curl -X POST https://finscan-production.up.railway.app/api/v1/auth/login \
  -d "username=joao_silva&password=senha_segura123"
```

## Usando o Token JWT

Após login, use o `access_token` em requests autenticados:

```javascript
// Exemplo: Fazer request autenticado
const response = await fetch(
  "https://finscan-production.up.railway.app/api/v1/users/me",
  {
    method: "GET",
    headers: {
      "Authorization": `Bearer ${access_token}`
    }
  }
);
```

## Ficheiros de Exemplo

- `registration_example.py` - Exemplo completo em Python
- `registration_example.js` - Exemplo completo em JavaScript

## Fluxo de Autenticação Típico

```
1. Utilizador preenche formulário de registo
   ↓
2. POST /api/v1/auth/register
   ↓
3. API cria o utilizador e retorna os dados (201)
   ↓
4. Utilizador faz login
   ↓
5. POST /api/v1/auth/login
   ↓
6. API retorna JWT access_token (200)
   ↓
7. Cliente guarda o token (localStorage, cookie, etc)
   ↓
8. Próximos requests incluem: Authorization: Bearer {token}
```
