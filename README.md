# Banca Zapp - Backend

Backend Spring Boot 3.x para consumo por frontend Angular, com API REST versionada em `/api/v1`.

## Requisitos

- Java 21
- Maven 3.9+
- PostgreSQL

## Configuracao

Edite `src/main/resources/application.yml` com as credenciais do banco:

```
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/banca_zapp
    username: postgres
    password: postgres
```

Crie o banco:

```
CREATE DATABASE banca_zapp;
```

## Executar

```
./mvnw spring-boot:run
```

## Endpoints principais

- `/api/v1/categorias`
- `/api/v1/produtos`
- `/api/v1/localidades`
- `/api/v1/clientes`
- `/api/v1/usuarios`
- `/api/v1/visitas` (POST)
- `/api/v1/visitas/cliente/{clienteId}`
- `/api/v1/estoques/cliente/{clienteId}`
- `/api/v1/auth/login`

## Observacoes

- CORS liberado para consumo do frontend.
- DTOs sao usados em todas as respostas.
- Estrutura JWT preparada em `JwtService` para evolucao futura.
- Envio de e-mail usa `spring-boot-starter-mail` com suporte a MailHog.

## E-mail (opcional)

Para ativar:

```
app:
  mail:
    enabled: true
    from: no-reply@bancazapp.local
    default-recipient: seu-email@exemplo.com
spring:
  mail:
    host: localhost
    port: 1025
```

O envio e disparado apos o commit da visita e falhas nao impedem o salvamento.
