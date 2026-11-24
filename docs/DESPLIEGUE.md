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
3. En el panel, pulsa **Connect** y copia la cadena de conexión. Tiene esta forma:

   ```
   postgresql://USUARIO:CLAVE@HOST/BASE?sslmode=require
   ```

4. Conviértela en **tres variables** (la aplicación recibe el usuario y la clave por separado):

   | Variable | Valor |
   |---|---|
   | `DB_URL` | `jdbc:postgresql://HOST/BASE?sslmode=require` |
   | `DB_USER` | `USUARIO` |
   | `DB_PASSWORD` | `CLAVE` |

   Si la cadena trae `channel_binding=require`, quítalo: el driver JDBC no lo usa. Deja `sslmode=require`, que Neon exige.

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
4. Comprueba el límite de intentos: más de 10 peticiones seguidas a `/api/auth/login` desde la misma IP deben recibir `429`.

   Si recibes `429` con muy pocas peticiones o desde distintos lugares, la plataforma no está pasando la IP real del cliente y todos comparten un mismo cupo. Aumenta `RATE_LIMIT_CAPACITY` o revisa `server.forward-headers-strategy` en `application.yml`.

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
| `RATE_LIMIT_WINDOW` | No | Ventana, por ejemplo `1m` (por defecto) |
| `JAVA_OPTS` | No | Opciones de la JVM; el `Dockerfile` trae valores para instancias de 512 MB |

## Problemas frecuentes

| Síntoma | Causa probable |
|---|---|
| El despliegue falla o queda reiniciando | Revisa los **Logs** en Render. Casi siempre es `DB_URL` mal formada o una clave incorrecta. |
| Error de SSL o "connection refused" | Falta `?sslmode=require` en `DB_URL`. |
| `JWT_SECRET debe tener al menos 32 caracteres` | Se escribió un valor corto a mano; deja que Render lo genere. |
| Muy lento la primera vez | Es el arranque en frío del plan gratuito (servicio y base dormidos). |
| El contenedor se reinicia por falta de memoria | Reduce la memoria de la JVM con `JAVA_OPTS`, por ejemplo `-XX:MaxRAMPercentage=60`. |

## Otras plataformas

Se descartaron para esta guía por sus condiciones al revisarlas en noviembre de 2026: **Fly.io** (ya sin capa gratuita), **Railway** (créditos de prueba, no un plan gratuito permanente), **Google Cloud Run** (muy generoso, pero exige una cuenta de facturación y no incluye PostgreSQL gratuito) y **Koyeb** (las fuentes consultadas no coinciden sobre si pide tarjeta). Como la aplicación se empaqueta en Docker y se configura por variables de entorno, moverla a otra plataforma es directo.
