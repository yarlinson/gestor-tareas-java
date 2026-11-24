# Despliegue con una URL pública (Render + Neon)

Esta guía publica la API sin costo usando dos servicios con plan gratuito:

```
Navegador / Swagger UI ──►  Render (contenedor Docker, plan Free)  ──►  Neon (PostgreSQL, plan Free)
```

- **Render** construye la imagen con el `Dockerfile` del repositorio y la ejecuta. La configuración está en [`render.yaml`](../render.yaml).
- **Neon** aloja la base de datos PostgreSQL. Se usa en lugar de la base gratuita de Render porque esa **expira a los 30 días**, mientras que la de Neon no tiene vencimiento.

> Las condiciones de los planes gratuitos cambian con frecuencia. Datos revisados en noviembre de 2026 en la documentación oficial: [Render – Deploy for Free](https://render.com/docs/free) y [Neon – Plans](https://neon.com/docs/introduction/plans). Confírmalos antes de depender de ellos.

## Qué esperar del plan gratuito

| Aspecto | Comportamiento |
|---|---|
| Reposo del servicio | Render apaga el servicio tras 15 minutos sin tráfico y lo despierta con la siguiente petición. Render habla de cerca de 1 minuto, pero esta aplicación (Spring Boot) tardó **unos 2 minutos y medio** en responder en una prueba local limitada a 0,1 CPU y 512 MB, el caso más lento del plan gratuito. Cuenta con que el primer acceso tras un rato de inactividad sea lento. |
| Reposo de la base | Neon suspende la base tras 5 minutos de inactividad y se reactiva sola en la siguiente consulta. |
| Recursos | Unos 512 MB de RAM y muy poca CPU en Render; 1 GB de almacenamiento por proyecto en Neon. |
| Datos | Cualquiera puede registrarse en la demo. **No guardes datos reales.** |

Para una demo de portafolio es suficiente; para producción real hay que pasar a un plan de pago.

## Paso 1. Crear la base de datos en Neon

1. Crea una cuenta en [neon.com](https://neon.com) y un proyecto nuevo.
2. Elige una **región cercana a la de Render** (si Render queda en Oregón, usa AWS US West 2; si queda en Fráncfort, usa Fráncfort). Menos distancia significa menos latencia.
3. En el panel, pulsa **Connect** y **desactiva la opción "Connection pooling"** para obtener la conexión *directa*: su host no lleva `-pooler`. Neon recomienda la conexión directa para ejecutar migraciones (el pooler no admite los bloqueos de sesión que usan esas herramientas), y la aplicación ya trae su propio pool de conexiones.
4. Copia la cadena de conexión. Tiene esta forma (valores de ejemplo):

   ```
   postgresql://neondb_owner:LaClave123@ep-ejemplo-123456.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```

5. **No la pegues tal cual en Render.** La aplicación necesita el formato JDBC y recibe el usuario y la clave por separado. Divídela en **tres variables**:

   | Variable | Valor (con el ejemplo anterior) |
   |---|---|
   | `DB_URL` | `jdbc:postgresql://ep-ejemplo-123456.us-east-2.aws.neon.tech/neondb?sslmode=require` |
   | `DB_USER` | `neondb_owner` |
   | `DB_PASSWORD` | `LaClave123` |

   - `DB_URL` **debe empezar por `jdbc:postgresql://`** y no lleva usuario ni clave. Si no, la aplicación falla al arrancar con `'url' must start with "jdbc"`.
   - Si la cadena trae `channel_binding=require`, quítalo: el driver JDBC no lo usa. Deja `sslmode=require`, que Neon exige.
   - Si la clave tiene caracteres como `%40`, están codificados para URL: escríbelos decodificados en `DB_PASSWORD` (`%40` es `@`).

No hace falta crear tablas: Flyway las crea al arrancar la aplicación.

## Paso 2. Crear el servicio en Render

1. Crea una cuenta en [render.com](https://render.com) con tu usuario de GitHub y da acceso al repositorio `gestor-tareas-java`.
2. Pulsa **New → Blueprint** y elige el repositorio. Render lee `render.yaml` y propone el servicio `gestor-tareas`.
3. Cuando lo pida, escribe los valores de `DB_URL`, `DB_USER` y `DB_PASSWORD` del paso anterior. `JWT_SECRET` no se escribe: Render lo genera aleatorio.
4. Confirma con **Apply**. La primera construcción tarda varios minutos porque compila con Maven dentro de Docker.
5. Al terminar, Render muestra la URL pública, algo como `https://gestor-tareas-xxxx.onrender.com`.

Los secretos quedan solo en Render; nunca se suben a Git.

## Paso 3. Comprobar que funciona

1. Abre la URL: debe redirigir a **Swagger UI**.
2. Abre `/actuator/health`: debe responder `{"status":"UP"}`.
3. En Swagger, usa `POST /api/auth/registro`, copia el token, pulsa **Authorize** y crea una tarea.
4. Comprueba el límite de intentos: más de 10 peticiones seguidas a `/api/auth/login` desde la misma conexión deben recibir `429`. Si todas devuelven `401`, sigue con la sección siguiente.

## Límite de intentos detrás del proxy de Render

**Probado en un despliegue real:** con la configuración por defecto el límite **no se activa** en Render. Más de 25 intentos fallidos seguidos dieron siempre `401`. La aplicación recibe la IP del proxy (Render y Cloudflare están delante), no la del visitante, y esa IP cambia de una petición a otra; cada intento cae en una cubeta distinta y nunca se agota.

Esto **no afecta al límite por usuario** (`RATE_LIMIT_LOGIN_CAPACITY_PER_USER`, 5 intentos por minuto para cada nombre de usuario): no depende de la IP, así que protege las cuentas de la fuerza bruta aunque la cabecera aún no esté configurada. El límite por IP sigue siendo útil para frenar el registro masivo de cuentas.

La solución es indicar la cabecera que contiene la IP real con `RATE_LIMIT_CLIENT_IP_HEADER`. Esta solución **todavía no está verificada en Render**; hay que comprobar dos condiciones:

1. **La cabecera debe existir.** Define `RATE_LIMIT_DEBUG=true`, haz unas peticiones a `/api/auth/login` y busca en los logs de Render la línea `rate-limit clave=... remoteAddr=... X-Forwarded-For=... CF-Connecting-IP=... True-Client-IP=...`. Elige la cabecera que traiga tu IP pública. Al terminar, vuelve a poner `RATE_LIMIT_DEBUG` en `false`.
2. **El cliente no debe poder falsearla.** Si el visitante pudiera enviar su propia cabecera y esta llegara intacta, cambiaría el valor en cada intento y evadiría el límite. Con la variable ya configurada, haz 12 peticiones seguidas a `/api/auth/login` enviando en cada una un valor distinto, por ejemplo `-H "CF-Connecting-IP: 1.2.3.N"`. Si el proxy la sobrescribe, la aplicación sigue viendo tu IP real y las últimas respuestas son `429`. Si todas son `401`, la cabecera se puede falsear y no sirve como clave.

No uses `X-Forwarded-For` completa como clave: el cliente puede añadir valores al principio de esa lista (Render solo agrega al final).

## Variables de entorno

| Variable | Obligatoria | Descripción |
|---|---|---|
| `DB_URL` | Sí | URL JDBC de PostgreSQL |
| `DB_USER` | Sí | Usuario de la base |
| `DB_PASSWORD` | Sí | Clave de la base |
| `JWT_SECRET` | Sí | Secreto de al menos 32 caracteres para firmar los tokens |
| `PORT` | No | La define la plataforma; por defecto `8080` |
| `JWT_EXPIRATION_MINUTES` | No | Vigencia del token (por defecto `60`) |
| `RATE_LIMIT_ENABLED` | No | `true` por defecto |
| `RATE_LIMIT_CAPACITY` | No | Intentos por IP en la ventana (por defecto `10`) |
| `RATE_LIMIT_LOGIN_CAPACITY_PER_USER` | No | Intentos de login por nombre de usuario en la ventana, vengan de la IP que vengan (por defecto `5`) |
| `RATE_LIMIT_WINDOW` | No | Ventana, por ejemplo `1m` (por defecto) |
| `RATE_LIMIT_CLIENT_IP_HEADER` | No | Cabecera con la IP real del cliente detrás de un proxy (ver la sección sobre el límite de intentos). Vacío usa la IP de la conexión |
| `RATE_LIMIT_DEBUG` | No | `true` registra en el log la IP de la conexión y las cabeceras de reenvío, solo para diagnosticar |
| `ADMIN_USERNAME` y `ADMIN_PASSWORD` | No | Crean el administrador inicial al arrancar si ambas tienen valor y el nombre no existe (clave de 8 a 72 caracteres). Ver la sección sobre administrador y recordatorios |
| `APP_TIMEZONE` | No | Zona horaria que decide qué día es "hoy" para tareas vencidas y recordatorios (por defecto `America/Bogota`) |
| `RECORDATORIOS_ENABLED` | No | Activa el proceso de recordatorios (por defecto `true`) |
| `RECORDATORIOS_CRON` | No | Cuándo se ejecuta, en formato cron de Spring de 6 campos (por defecto cada hora en punto: `0 0 * * * *`) |
| `RECORDATORIOS_DIAS_DE_ANTICIPACION` | No | Se avisa de las tareas que vencen hoy o dentro de estos días (por defecto `1`) |
| `JAVA_OPTS` | No | Opciones de la JVM; el `Dockerfile` trae valores para instancias de 512 MB |

## Administrador y recordatorios

**Administrador.** El rol `ADMIN` no se obtiene registrándose: se crea al arrancar a partir de `ADMIN_USERNAME` y `ADMIN_PASSWORD`. Para tener uno en Render, agrégalas en **Environment** y deja que redespliegue; después inicia sesión con ese usuario en `/api/auth/login` y usa los endpoints de `/api/admin`.

- Si ya existe una cuenta con ese nombre, **no se modifica** (no se le sube el rol ni se le cambia la clave). Así, que alguien se registre antes con ese nombre no le sirve para quedarse con el rol. Si el registro ya está abierto, elige un nombre poco evidente y usa una clave larga y única.
- Pasado el primer arranque puedes borrar `ADMIN_PASSWORD`: la cuenta ya existe y la variable no se vuelve a usar.

**Recordatorios.** Un proceso genera avisos (`/api/notificaciones`) de las tareas que vencen hoy o mañana y de las vencidas. Se ejecuta cada hora, y también **una vez al arrancar**. Eso último importa en el plan gratuito de Render: el servicio duerme tras 15 minutos sin tráfico y una tarea horaria no corre mientras duerme, así que al despertar se ponen al día los avisos pendientes. Un administrador también puede lanzarlo cuando quiera con `POST /api/admin/recordatorios/generar`. Está pensado para una sola instancia de la aplicación.

## Problemas frecuentes

| Síntoma | Causa probable |
|---|---|
| El despliegue falla o queda reiniciando | Revisa los **Logs** en Render. Casi siempre es `DB_URL` mal formada o una clave incorrecta. |
| `'url' must start with "jdbc"` | `DB_URL` tiene la cadena de Neon sin convertir (`postgresql://...`). Debe ser `jdbc:postgresql://HOST/BASE?sslmode=require`, sin usuario ni clave. |
| Error de SSL o "connection refused" | Falta `?sslmode=require` en `DB_URL`. |
| `JWT_SECRET debe tener al menos 32 caracteres` | Se escribió un valor corto a mano; deja que Render lo genere. |
| Muy lento la primera vez | Es el arranque en frío del plan gratuito (servicio y base dormidos). |
| El contenedor se reinicia por falta de memoria | Reduce la memoria de la JVM con `JAVA_OPTS`, por ejemplo `-XX:MaxRAMPercentage=60`. |

## Otras plataformas

Se descartaron para esta guía por sus condiciones al revisarlas en noviembre de 2026: **Fly.io** (ya sin capa gratuita), **Railway** (créditos de prueba, no un plan gratuito permanente), **Google Cloud Run** (muy generoso, pero exige una cuenta de facturación y no incluye PostgreSQL gratuito) y **Koyeb** (las fuentes consultadas no coinciden sobre si pide tarjeta). Como la aplicación se empaqueta en Docker y se configura por variables de entorno, moverla a otra plataforma es directo.
