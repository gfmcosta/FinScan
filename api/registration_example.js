/**
 * Cliente FinScan Registration
 * Base URL: https://finscan-production.up.railway.app
 * Rota de registo: POST /api/v1/auth/register
 */

class FinScanRegistration {
  constructor(baseUrl = "https://finscan-production.up.railway.app") {
    this.baseUrl = baseUrl;
    this.registerEndpoint = "/api/v1/auth/register";
    this.loginEndpoint = "/api/v1/auth/login";
  }

  /**
   * Valida um email
   * @param {string} email - Email a validar
   * @returns {boolean} - True se o email é válido
   */
  validateEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  /**
   * Regista um novo utilizador
   * @param {string} username - Nome de utilizador único
   * @param {string} email - Email válido
   * @param {string} password - Password da conta
   * @param {string} role - Papel do utilizador ('user' ou 'admin', default: 'user')
   * @returns {Promise<Object>} - Dados do utilizador criado
   * @throws {Error} - Se houver erro na validação ou na API
   *
   * @example
   * const client = new FinScanRegistration();
   * try {
   *   const user = await client.register(
   *     "joao_silva",
   *     "joao@example.com",
   *     "senha_segura123",
   *     "user"
   *   );
   *   console.log("Utilizador criado:", user);
   * } catch (error) {
   *   console.error("Erro:", error.message);
   * }
   */
  async register(username, email, password, role = "user") {
    // Validações básicas
    if (!username || !username.trim()) {
      throw new Error("Username não pode estar vazio");
    }

    if (!email || !email.trim()) {
      throw new Error("Email não pode estar vazio");
    }

    if (!password || !password.trim()) {
      throw new Error("Password não pode estar vazia");
    }

    if (!["user", "admin"].includes(role)) {
      throw new Error("Role deve ser 'user' ou 'admin'");
    }

    // Validar email
    if (!this.validateEmail(email)) {
      throw new Error("Email inválido");
    }

    // Preparar payload
    const payload = {
      username: username.trim(),
      email: email.trim(),
      password: password,
      role: role,
    };

    try {
      const url = `${this.baseUrl}${this.registerEndpoint}`;
      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      const data = await response.json();

      if (response.status === 201) {
        // Registo bem-sucedido
        return data;
      } else if (response.status === 400) {
        // Erro de validação
        throw new Error(`Erro de validação: ${data.detail}`);
      } else if (response.status === 422) {
        // Erro de schema
        const errorMsg = data.detail
          .map((e) => `${e.loc[1]}: ${e.msg}`)
          .join("; ");
        throw new Error(`Erro de schema: ${errorMsg}`);
      } else {
        throw new Error(
          `Erro da API (status ${response.status}): ${data.detail || "erro desconhecido"}`
        );
      }
    } catch (error) {
      if (error instanceof Error && error.message.startsWith("Erro")) {
        throw error;
      }
      throw new Error(`Erro de conexão: ${error.message}`);
    }
  }

  /**
   * Autentica um utilizador
   * @param {string} username - Nome de utilizador
   * @param {string} password - Password
   * @returns {Promise<Object>} - Token de acesso JWT
   * @throws {Error} - Se houver erro na autenticação
   *
   * @example
   * const client = new FinScanRegistration();
   * try {
   *   const token = await client.login("joao_silva", "senha_segura123");
   *   console.log("Token:", token.access_token);
   *   // Guardar o token no localStorage
   *   localStorage.setItem("access_token", token.access_token);
   * } catch (error) {
   *   console.error("Erro de autenticação:", error.message);
   * }
   */
  async login(username, password) {
    if (!username || !password) {
      throw new Error("Username e password são obrigatórios");
    }

    try {
      const url = `${this.baseUrl}${this.loginEndpoint}`;

      // OAuth2PasswordRequestForm usa form data
      const formData = new URLSearchParams();
      formData.append("username", username);
      formData.append("password", password);

      const response = await fetch(url, {
        method: "POST",
        body: formData,
      });

      const data = await response.json();

      if (response.status === 200) {
        return data;
      } else if (response.status === 401) {
        throw new Error("Username ou password incorretos");
      } else {
        throw new Error(
          `Erro da API (status ${response.status}): ${data.detail || "erro desconhecido"}`
        );
      }
    } catch (error) {
      if (error instanceof Error && error.message.startsWith("Erro")) {
        throw error;
      }
      throw new Error(`Erro de conexão: ${error.message}`);
    }
  }
}

// ============================================
// EXEMPLOS DE UTILIZAÇÃO
// ============================================

/**
 * Exemplo 1: Registo simples
 */
async function exemploRegisto() {
  const client = new FinScanRegistration();

  try {
    console.log("Registando novo utilizador...");
    const user = await client.register(
      "joao_silva",
      "joao@example.com",
      "senha_segura123",
      "user"
    );

    console.log("✓ Registo bem-sucedido!");
    console.log("Dados do utilizador:", user);
    // {
    //   id: 1,
    //   username: "joao_silva",
    //   email: "joao@example.com",
    //   role: "user",
    //   is_active: true
    // }
  } catch (error) {
    console.error("✗ Erro:", error.message);
  }
}

/**
 * Exemplo 2: Login após registo
 */
async function exemploLogin() {
  const client = new FinScanRegistration();

  try {
    console.log("Fazendo login...");
    const token = await client.login("joao_silva", "senha_segura123");

    console.log("✓ Login bem-sucedido!");
    console.log("Token:", token.access_token);

    // Guardar token no localStorage (para frontend)
    localStorage.setItem("access_token", token.access_token);
    localStorage.setItem("token_type", token.token_type);
  } catch (error) {
    console.error("✗ Erro:", error.message);
  }
}

/**
 * Exemplo 3: Fluxo completo de registo + login
 */
async function exemploFluxoCompleto() {
  const client = new FinScanRegistration();

  try {
    // Passo 1: Registo
    console.log("Passo 1: Registando novo utilizador...");
    const user = await client.register(
      "maria_santos",
      "maria@example.com",
      "senha_super_segura123"
    );
    console.log("✓ Utilizador criado:", user.username);

    // Passo 2: Login
    console.log("\nPasso 2: Fazendo login...");
    const token = await client.login("maria_santos", "senha_super_segura123");
    console.log("✓ Login bem-sucedido!");

    // Passo 3: Usar o token para fazer requests autenticados
    console.log("\nPasso 3: Usando token para request autenticado...");
    const response = await fetch(
      "https://finscan-production.up.railway.app/api/v1/users/me",
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token.access_token}`,
        },
      }
    );

    const userData = await response.json();
    console.log("✓ Dados do utilizador:", userData);
  } catch (error) {
    console.error("✗ Erro:", error.message);
  }
}

/**
 * Exemplo 4: Tratamento de erros
 */
async function exemploTratamentoErros() {
  const client = new FinScanRegistration();

  // Erro 1: Email inválido
  try {
    await client.register("user1", "email_invalido", "senha123");
  } catch (error) {
    console.log("Erro esperado:", error.message);
    // "Email inválido"
  }

  // Erro 2: Username duplicado (já registado)
  try {
    await client.register(
      "joao_silva",
      "novo_email@example.com",
      "senha123"
    );
  } catch (error) {
    console.log("Erro esperado:", error.message);
    // "Erro de validação: Username or email already exists"
  }

  // Erro 3: Password incorreta no login
  try {
    await client.login("joao_silva", "password_errada");
  } catch (error) {
    console.log("Erro esperado:", error.message);
    // "Username ou password incorretos"
  }
}

// Exportar para usar em módulos
if (typeof module !== "undefined" && module.exports) {
  module.exports = FinScanRegistration;
}
