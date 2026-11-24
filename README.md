# Gestor de Tareas — API REST con Spring Boot

[![CI](https://github.com/yarlinson/gestor-tareas-java/actions/workflows/ci.yml/badge.svg)](https://github.com/yarlinson/gestor-tareas-java/actions/workflows/ci.yml)

API REST para gestionar tareas personales. Cada usuario se registra, inicia sesión y administra **únicamente sus propias tareas**. Es la versión en Java del "Sistema de Gestión de Tareas" que antes hice con Django REST Framework.

## Tecnologías

| Área | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5 (Web, Validation, Actuator) |
| Persistencia | Spring Data JPA / Hibernate, PostgreSQL 16 |
| Migraciones | Flyway |
| Seguridad | Spring Security 6, JWT (jjwt), contraseñas con BCrypt, límite de intentos por IP (Bucket4j) |
| Documentación | OpenAPI / Swagger UI (springdoc) |
| Pruebas | JUnit 5, MockMvc, AssertJ, Testcontainers (PostgreSQL real), JaCoCo |
| Contenedores | Docker, Docker Compose |
| CI/CD | GitHub Actions, Dependabot |
| Despliegue | Render (Docker) + Neon (PostgreSQL), plan gratuito |

## Endpoints

| Método | Ruta | Descripción | Requiere token |
|---|---|---|---|
| POST | `/api/auth/registro` | Crea un usuario y devuelve su token | No |
| POST | `/api/auth/login` | Inicia sesión y devuelve un token | No |
| GET | `/api/tareas` | Lista tareas propias (filtros y paginación) | Sí |
| GET | `/api/tareas/{id}` | Obtiene una tarea | Sí |
| POST | `/api/tareas` | Crea una tarea | Sí |
| PUT | `/api/tareas/{id}` | Reemplaza título, descripción y fecha límite | Sí |
| PATCH | `/api/tareas/{id}/estado` | Marca como completada o pendiente | Sí |
| DELETE | `/api/tareas/{id}` | Elimina una tarea | Sí |

Parámetros de `GET /api/tareas`: `completada=true|false`, `q=texto` (busca en título y descripción, sin distinguir mayúsculas), `page`, `size` (máx. 100) y `sort` (por ejemplo `sort=titulo,asc`).

Documentación interactiva: <http://localhost:8080/swagger-ui.html> (el botón **Authorize** acepta el token de `/api/auth/login`).
Estado del servicio: <http://localhost:8080/actuator/health>.

## Cómo ejecutarlo

### Opción 1: Docker Compose (API + base de datos)

```powershell
Copy-Item .env.example .env      # luego edite .env con sus propios valores
docker compose up --build
```

La API queda en `http://localhost:8080` (si ese puerto está ocupado, defina `API_PORT` en `.env`). PostgreSQL se publica en el puerto `5433` del equipo.

### Opción 2: Maven con un PostgreSQL propio

Cree una base de datos `gestor_tareas` y un usuario `gestor`, y defina las variables de entorno:

```powershell
$env:DB_PASSWORD = "su_clave"
$env:JWT_SECRET  = "un-secreto-aleatorio-de-al-menos-32-caracteres"
# Opcionales: DB_URL (por defecto jdbc:postgresql://localhost:5432/gestor_tareas), DB_USER (por defecto gestor)
mvn spring-boot:run
```

### Ejemplo de uso

```bash
# Registro (devuelve un token)
curl -X POST http://localhost:8080/api/auth/registro \
  -H "Content-Type: application/json" \
  -d '{"nombreUsuario":"ana","email":"ana@correo.com","password":"ClaveSegura123"}'

# Crear una tarea
curl -X POST http://localhost:8080/api/tareas \
  -H "Authorization: Bearer <TOKEN>" -H "Content-Type: application/json" \
  -d '{"titulo":"Estudiar Spring Security","descripcion":"Capítulo de JWT"}'

# Buscar pendientes que contengan "spring"
curl "http://localhost:8080/api/tareas?completada=false&q=spring" -H "Authorization: Bearer <TOKEN>"
```

## Despliegue

La aplicación está preparada para publicarse en [Render](https://render.com) (contenedor Docker) con PostgreSQL en [Neon](https://neon.com), ambos con plan gratuito. La guía paso a paso está en [docs/DESPLIEGUE.md](docs/DESPLIEGUE.md) y el servicio se describe en [`render.yaml`](render.yaml).

- Toda la configuración llega por variables de entorno (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`); el puerto lo toma de `PORT`, que define la plataforma.
- La URL raíz redirige a Swagger UI, así que el enlace público abre directamente la documentación interactiva.
- Con el plan gratuito el servicio se duerme tras un rato sin tráfico: la primera petición posterior puede tardar de uno a tres minutos mientras la aplicación vuelve a arrancar.

## Pruebas

```powershell
mvn test      # ejecuta las pruebas
mvn verify    # además genera el informe de cobertura en target/site/jacoco/index.html
```

**Requieren Docker en ejecución.** Las pruebas de integración usan [Testcontainers](https://testcontainers.com/): levantan un PostgreSQL 16 real (la misma imagen que `docker-compose.yml`), arrancan la aplicación completa contra él y lo eliminan al terminar. Un único contenedor se comparte entre todas las clases de prueba para no pagar el arranque varias veces.

Como Flyway y la validación del esquema de Hibernate siguen activos, las pruebas también comprueban que las migraciones coinciden con las entidades sobre la misma base de datos que se usa en producción, sin diferencias de dialecto como las que habría con una base en memoria. Cubren registro, login, validaciones, aislamiento entre usuarios, filtros, búsqueda, paginación, ordenamiento y rechazo de tokens manipulados; además hay pruebas unitarias del servicio JWT.

## Integración continua

El flujo de [`.github/workflows/ci.yml`](.github/workflows/ci.yml) se ejecuta en cada push a `main` y en cada pull request:

1. **Compilar y probar:** `mvn verify` con Java 21. Testcontainers usa el Docker que ya trae el runner de GitHub.
2. **Cobertura:** el resumen de la ejecución muestra el porcentaje de líneas cubiertas y el informe HTML de JaCoCo queda como artefacto descargable.
3. **Imagen Docker:** comprueba que el `Dockerfile` sigue construyendo (no se publica la imagen).

[Dependabot](.github/dependabot.yml) abre pull requests semanales para actualizar dependencias de Maven, acciones de GitHub e imágenes base de Docker.

## Decisiones de diseño

- **JWT sin estado y sin CSRF.** El token viaja en el encabezado `Authorization`, no en cookies, por eso la protección CSRF no aplica y se desactiva de forma explícita.
- **Aislamiento por usuario.** Todas las consultas filtran por el propietario. Una tarea ajena responde `404` igual que una inexistente, para no revelar qué identificadores existen.
- **Esquema versionado.** Flyway crea las tablas (`db/migration`) e Hibernate solo valida (`ddl-auto: validate`).
- **Pruebas contra la base real.** Testcontainers en lugar de H2, para detectar diferencias de SQL y de migraciones antes de llegar a producción.
- **Filtros dinámicos con `Specification`.** Estado y texto son opcionales y se combinan sin escribir una consulta por cada variante; los comodines `%` y `_` de la búsqueda se escapan.
- **Errores estándar (RFC 7807).** Las respuestas de error usan `application/problem+json`; las validaciones incluyen el detalle por campo.
- **Secretos por variables de entorno.** La aplicación no arranca si falta `JWT_SECRET` o si tiene menos de 32 caracteres.
- **Límite de intentos.** `/api/auth/**` admite 10 intentos por minuto y por cliente y, además, el login admite 5 por minuto para cada nombre de usuario sin importar la IP, lo que protege las cuentas de la fuerza bruta (token bucket con Bucket4j); el exceso recibe `429` con `Retry-After`. Se configura con `RATE_LIMIT_*` y está pensado para una sola instancia: con varias habría que compartir el estado, por ejemplo en Redis. Detrás de un proxy la IP de la conexión no es la del visitante, así que hay que indicar la cabecera con la IP real (`RATE_LIMIT_CLIENT_IP_HEADER`); los detalles y la comprobación en [docs/DESPLIEGUE.md](docs/DESPLIEGUE.md).

## Estructura

```
src/main/java/com/yarlinson/gestortareas/
├── auth/       Registro e inicio de sesión
├── tarea/      Entidad, repositorio, filtros, servicio y controlador
├── usuario/    Entidad y repositorio de usuarios
├── security/   JWT, filtro de autenticación y usuario autenticado
├── config/     Configuración de seguridad y OpenAPI
└── common/     Manejo global de errores y respuesta paginada
src/main/resources/db/migration/   Migraciones de Flyway
src/test/java/                     Pruebas de integración (Testcontainers) y unitarias
.github/                           Workflow de CI y configuración de Dependabot
```
