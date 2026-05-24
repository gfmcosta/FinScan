FROM python:3.11-slim

WORKDIR /app

# Instalar dependências de sistema necessárias para compilar bibliotecas
RUN apt-get update && apt-get install -y gcc libpq-dev && rm -rf /var/lib/apt/lists/*

# Copiar apenas os requirements primeiro para aproveitar o cache do Docker
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copiar o resto do código da aplicação
COPY . .

# Expor o porto da API
EXPOSE 8000

# Comando para arrancar a app
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]