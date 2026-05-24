# FinScan API

Bem-vindo à documentação do backend do **FinScan**! Esta é uma API desenvolvida em **FastAPI** responsável por gerir toda a lógica de negócio, persistência de dados e integração com Inteligência Artificial.

## 📌 O que é e para que serve?

A **FinScan API** é a espinha dorsal de uma aplicação focada na gestão e digitalização de talões/recibos. As principais funcionalidades oferecidas por esta API incluem:

- **Autenticação e Segurança (JWT)**: Registo, login e gestão de utilizadores de forma segura.
- **Processamento de Recibos (OCR)**: Utilização da API do Google Gemini para extrair e interpretar dados de talões fiscais.
- **Assistente Virtual (Chatbot)**: Um serviço interativo (movido pelo Gemini) para responder a questões sobre despesas e recibos associados ao utilizador.
- **Gestão de Base de Dados**: Armazenamento seguro de utilizadores, recibos e histórico de conversas (utilizando SQLAlchemy e, por defeito, SQLite).

---

## 🛠️ Tecnologias Utilizadas

- [FastAPI](https://fastapi.tiangolo.com/) - Framework web para a construção da API.
- [SQLAlchemy](https://www.sqlalchemy.org/) - ORM para interação com a Base de Dados.
- [Pydantic](https://docs.pydantic.dev/) - Validação de dados e gestão de configurações.
- [Google Generative AI (Gemini)](https://ai.google.dev/) - Motor de Inteligência Artificial para Chatbot e OCR.
- **Passlib & Python-Jose**: Hash de passwords e gestão de Tokens JWT.

---

## ⚙️ Configuração das Variáveis de Ambiente (.env)

Para que a API funcione corretamente, é **obrigatório** ter um ficheiro chamado `.env` na raiz da pasta `api`.

Fornecemos um ficheiro `env.example` que pode usar como base. Basta copiá-lo e renomeá-lo para `.env`:

```bash
cp env.example .env
```

O ficheiro contém as seguintes variáveis que deve preencher com os seus valores reais:

```env
# URL de ligação à base de dados. Por defeito usa SQLite para desenvolvimento.
DATABASE_URL=caminho_da_bd

# Configurações de Segurança e JWT
# Gere uma chave robusta para o ambiente de produção
SECRET_KEY=sua-secret-key-super-segura-aqui
ALGORITHM=algortimo_hash
ACCESS_TOKEN_EXPIRE_MINUTES=10

# Configurações de IA (Google Gemini) para OCR e Chatbot
GEMINI_API_KEY=sua_chave_de_api_da_google_aqui
GEMINI_MODEL=modelo_google
```

> **Aviso:** Nunca partilhe o seu ficheiro `.env` publicamente nem o submeta para o GitHub (certifique-se de que está no `.gitignore`).

---

## 🚀 Como correr o projeto localmente

Siga estes passos recomendados para correr a API no seu ambiente de desenvolvimento:

**1. Navegar para a pasta da API:**
```bash
cd FinScan/api
```

**2. Criar e Ativar um Ambiente Virtual (Recomendado):**
No macOS / Linux:
```bash
python3 -m venv venv
source venv/bin/activate
```
No Windows:
```bash
python -m venv venv
venv\Scripts\activate
```

**3. Instalar as dependências:**
```bash
pip install -r requirements.txt
```

**4. Configurar as variáveis de ambiente (.env):**
Copie o ficheiro de exemplo e preencha-o com as suas credenciais reais (ex: `GEMINI_API_KEY`).
No macOS / Linux:
```bash
cp env.example .env
```
No Windows:
```bash
copy env.example .env
```

**5. Iniciar o Servidor:**
```bash
uvicorn app.main:app --reload
```

---

## 📚 Documentação da API (Swagger UI)

Uma vez que o servidor esteja a correr, o FastAPI gera automaticamente a documentação interativa para testar todos os endpoints. 

Aceda no seu browser a:
- **Swagger UI:** [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs)
- **ReDoc:** [http://127.0.0.1:8000/redoc](http://127.0.0.1:8000/redoc)
