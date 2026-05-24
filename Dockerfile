# Usar o Dockerfile da API
FROM python:3.11-slim

WORKDIR /app

# Instalar dependências de sistema
RUN apt-get update && apt-get install -y gcc libpq-dev && rm -rf /var/lib/apt/lists/*

# Copiar requirements da API
COPY api/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copiar código da API
COPY api/app ./app

# Expor porto
EXPOSE 8000

# Start
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
