# GameLog

## Descripción general

GameLog es una API REST para una comunidad de jugadores que quieren llevar el registro de su vida gamer, al estilo de Letterboxd pero para videojuegos. Cada usuario puede marcar los videojuegos que ha jugado, calificarlos de 0.5 a 5 estrellas, escribir reseñas, guardar juegos en su lista de pendientes (backlog) y crear listas temáticas ("Mis RPG favoritos", "Joyas indie"). Cada juego muestra su calificación promedio y sus reseñas más populares.

El catálogo de juegos no se captura a mano: el sistema lo **importa desde una API pública de videojuegos (RAWG)** y lo guarda en su propia base de datos. Así, las consultas de los usuarios son rápidas y no dependen de un servicio externo. El equipo de moderación puede complementar o corregir el catálogo.

El proyecto está construido con **Spring Boot** y aprovecha sus paquetes para seguridad, persistencia, validación, documentación, logs y pruebas.

## Integrantes del equipo

Aviles Lalo Hugo Enrique
Borja Vera Raziel
Castro Villalpando Jose Emmanuel
Guerra Patiño Luis Uriel
Garcia Hernandez Diego Alejandro

## Requerimientos (primeros 5)

| ID | Requerimiento | Descripción | Prioridad |
|----|---------------|-------------|-----------|
| RF-01 | Registro e inicio de sesión | Los usuarios se registran e inician sesión con correo y contraseña; cada uno tiene un perfil público. | Alta |
| RF-02 | Catálogo de videojuegos | El sistema importa juegos desde la API de RAWG a la base de datos local; los usuarios los consultan, buscan y filtran por título, plataforma, género y año. El administrador puede lanzar la importación y editar el catálogo. | Alta |
| RF-03 | Diario de juegos y calificaciones | El usuario registra un juego como jugado, con fecha, calificación (0.5 a 5) y estado (jugando, terminado, abandonado). | Alta |
| RF-04 | Reseñas | El usuario escribe, edita y elimina su reseña de un juego; otros usuarios pueden darle "me gusta". | Alta |
| RF-05 | Backlog y listas | El usuario guarda juegos pendientes y crea listas públicas o privadas con juegos ordenados. | Media |

## Arquitectura

La API se organiza en capas. Cada capa tiene una sola responsabilidad:

```
Cliente (Postman / Swagger / front-end)
        │  HTTP + JSON
        ▼
Controller   → recibe la petición, valida y responde (@RestController)
        ▼
Service      → lógica de negocio (reglas, importación, cálculos)
        ▼
Repository   → acceso a datos (Spring Data JPA)
        ▼
Base de datos (PostgreSQL)

Service de importación ──► RawgClient ──► API de RAWG (servicio externo)
```

### Estructura de paquetes

```
com.gamelog
├── config/        → seguridad, CORS, Swagger, propiedades
├── controller/    → AuthController, GameController, ReviewController...
├── service/       → GameService, ReviewService, ImportService...
├── repository/    → GameRepository, ReviewRepository...
├── client/        → RawgClient (consumo de la API externa)
├── model/         → entidades JPA (User, Game, Review, GameLogEntry, GameList)
├── dto/           → objetos de entrada y salida (propios y de RAWG)
├── exception/     → errores personalizados y manejador global
└── security/      → filtro JWT, UserDetailsService
```

### Entidades principales

| Entidad | Campos clave | Relaciones |
|---------|--------------|------------|
| User | id, username, email, passwordHash, rol | tiene muchas entradas, reseñas y listas |
| Game | id, externalId (único), título, plataformas, géneros, año, portada | tiene muchas reseñas |
| GameLogEntry | usuario, juego, estado, calificación, fecha | une User y Game |
| Review | usuario, juego, texto, calificación, fecha | User y Game |
| GameList | usuario, nombre, pública/privada | muchos Games (ordenados) |
| Follow | seguidor, seguido | User con User |

### Endpoints previstos

| Método | Ruta | Función | Acceso |
|--------|------|---------|--------|
| POST | `/api/auth/register` | Registrar usuario | Público |
| POST | `/api/auth/login` | Iniciar sesión y obtener JWT | Público |
| GET | `/api/games?search=&genre=&page=` | Listar y buscar juegos | Público |
| GET | `/api/games/{id}` | Detalle del juego y promedio | Público |
| POST/PUT/DELETE | `/api/games` | Gestionar catálogo | Admin |
| POST | `/api/admin/games/import?query=` | Importar juegos desde RAWG | Admin |
| POST | `/api/log` | Registrar un juego como jugado | Usuario |
| PUT/DELETE | `/api/log/{id}` | Editar o quitar una entrada | Dueño |
| POST | `/api/games/{id}/reviews` | Escribir reseña | Usuario |
| GET | `/api/games/{id}/reviews` | Ver reseñas (paginadas) | Público |
| PUT/DELETE | `/api/reviews/{id}` | Editar o borrar reseña | Dueño |
| POST/GET | `/api/lists` | Crear y consultar listas | Usuario |
| POST | `/api/backlog/{gameId}` | Agregar a pendientes | Usuario |

## Fuente de datos: API externa de videojuegos

### Estrategia

Las peticiones de los usuarios **nunca** van directo a la API externa. El backend actúa como intermediario:

1. **Importación (principal):** un proceso trae juegos de RAWG y los guarda en la base de datos local. Los usuarios consultan siempre la base propia.
2. **Consulta bajo demanda con caché (complementaria):** si se busca un juego que no existe localmente, el sistema lo pide a RAWG, lo guarda y lo devuelve.

### Comparativa de opciones evaluadas

| | RAWG (elegida) | IGDB |
|---|---|---|
| Autenticación | Una clave (`key`) como parámetro de la URL | Cuenta de Twitch; token por *client credentials* y cabeceras `Client-ID` y `Authorization: Bearer` |
| Método HTTP | GET | Mayoría POST |
| Límite de peticiones | Según el plan gratuito vigente (verificar en rawg.io/apidocs) | 4 peticiones por segundo por Client ID |
| Condición de uso | Citar a RAWG como fuente y enlazarlo donde se usen sus datos | Gratis para uso no comercial bajo el acuerdo de desarrolladores de Twitch |
| Dificultad | Baja | Media (renovar token, límite estricto) |

Se eligió **RAWG** por su simplicidad: una clave y peticiones GET. IGDB queda como alternativa; en ese caso habría que agregar un componente que pida y renueve el token de Twitch, y un límite estricto de peticiones hacia el servicio.

### Configuración

La clave de la API **nunca** se sube al repositorio. Se define como variable de entorno (el archivo `.env` está en el `.gitignore`):

```properties
# application.properties
rawg.api.key=${RAWG_API_KEY}
rawg.api.base-url=https://api.rawg.io/api
```

### Cliente HTTP (`RestClient`)

`RestClient` viene incluido en `spring-boot-starter-web`, por lo que no requiere dependencias adicionales.

```java
@Component
public class RawgClient {

    private final RestClient restClient;
    private final String apiKey;

    public RawgClient(@Value("${rawg.api.base-url}") String baseUrl,
                      @Value("${rawg.api.key}") String apiKey) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public RawgGamesResponse searchGames(String query, int page) {
        return restClient.get()
            .uri(uri -> uri.path("/games")
                .queryParam("key", apiKey)
                .queryParam("search", query)
                .queryParam("page", page)
                .queryParam("page_size", 20)
                .build())
            .retrieve()
            .body(RawgGamesResponse.class);
    }
}
```

DTOs que reflejan solo los campos necesarios de la respuesta externa:

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record RawgGamesResponse(int count, List<RawgGame> results) {}

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawgGame(
    Long id, String name, String released,
    @JsonProperty("background_image") String backgroundImage,
    Double rating) {}
```

El service de importación convierte cada `RawgGame` en una entidad `Game` propia y evita duplicados mediante el campo único `externalId`.

### Atribución

Los datos de videojuegos y las imágenes provienen de [RAWG](https://rawg.io). Se cita la fuente en este README y en la documentación de Swagger, y se incluirá un enlace a RAWG en cualquier página que muestre sus datos.

## Funcionalidades que pueden resolverse con paquetes de Spring Boot

| Funcionalidad | Paquete / Starter |
|---------------|-------------------|
| API REST | `spring-boot-starter-web` |
| Consumo de la API externa (RAWG) | `RestClient` (incluido en `spring-boot-starter-web`) |
| Autenticación y roles (usuario / moderador / admin) | `spring-boot-starter-security` (+ JWT) |
| Conexión a base de datos y ORM | `spring-boot-starter-data-jpa` + driver PostgreSQL |
| Paginación y ordenamiento de catálogo y reseñas | Spring Data (`Pageable`) |
| Validación (rango de calificación, longitud de reseña) | `spring-boot-starter-validation` |
| Logs | SLF4J + Logback (incluido por defecto) |
| Importación programada del catálogo | `@Scheduled` (Spring Framework) |
| Tolerancia a fallos hacia la API externa (reintentos, límite, circuit breaker) | Resilience4j |
| Caché de rankings y promedios | `spring-boot-starter-cache` |
| Monitoreo y health checks (incluye estado de RAWG) | `spring-boot-starter-actuator` |
| Documentación de la API | `springdoc-openapi` (Swagger UI) |
| Pruebas unitarias y de integración | `spring-boot-starter-test` |
| Pruebas del cliente externo sin llamadas reales | `MockRestServiceServer` o WireMock |
| Configuración por entorno y secretos | Perfiles y variables de entorno |
| Migraciones y datos iniciales | Flyway |
| Límite de peticiones (anti-spam de reseñas) | Bucket4j (integrable con Spring) |

## Funcionalidades necesarias para que la API funcione correctamente

### Imprescindibles

1. **Autenticación y autorización:** contraseñas con BCrypt, login con JWT, reglas por rol y verificación de que solo el dueño edite o borre su reseña.
2. **Persistencia:** entidades JPA, relaciones correctas y restricciones únicas (una reseña por usuario y juego; un `externalId` por juego).
3. **Validación de entrada:** `@NotBlank`, `@Size`, `@Min/@Max` para la calificación y correo válido.
4. **Manejo global de errores:** un `@RestControllerAdvice` que devuelve respuestas consistentes (`400`, `401`, `403`, `404`, `409`) con un JSON de error claro.
5. **DTOs:** nunca se devuelve la entidad `User` directamente, para no exponer el hash de la contraseña. Lo mismo aplica para los datos de RAWG: se mapean al modelo propio.
6. **Paginación** en catálogo y reseñas.
7. **CORS** si se agrega un front-end en otro puerto o dominio.
8. **Clave de RAWG en variables de entorno,** nunca en el código ni en GitHub.
9. **Manejo de errores y timeouts hacia la API externa:** si RAWG falla o tarda, la API responde con un error claro (`502`/`503`) o usa los datos locales, sin quedarse colgada.
10. **Atribución a RAWG** en README, Swagger y páginas que muestren sus datos.

### Importantes

11. **Documentación con Swagger UI** para documentar y probar sin Postman.
12. **Logs** de eventos relevantes: registro, login fallido, importaciones y errores de la API externa.
13. **Pruebas:** unitarias para los services y de integración para los controllers (`MockMvc`), con el cliente de RAWG simulado.
14. **Configuración por perfiles** (`dev`, `prod`).
15. **Migraciones con Flyway** para versionar el esquema.
16. **Base local como caché:** importar una vez y consultar localmente para no gastar la cuota de peticiones.
17. **Importación controlada:** endpoint de administrador (`POST /api/admin/games/import`) y, opcionalmente, una tarea programada con `@Scheduled`.
18. **Límite de peticiones hacia el servicio externo** (Resilience4j: `@RateLimiter`, `@Retry`, `@CircuitBreaker`).
19. **Paginación al importar:** recorrer las páginas de RAWG con una pausa entre peticiones.

### Opcionales

20. Caché para promedios y rankings.
21. Límite de peticiones por usuario para evitar spam de reseñas.
22. Guardar solo la URL de la portada, sin descargar imágenes.
23. `HealthIndicator` de Actuator que avise si RAWG no responde.
24. Soporte alternativo para IGDB (token de Twitch y límite de 4 peticiones por segundo).

## Plan de trabajo

1. **Base:** proyecto con hello world, devcontainer y conexión a PostgreSQL.
2. **Modelo y catálogo:** entidades, repositorios, cliente de RAWG, importación y endpoints de juegos (RF-02).
3. **Seguridad:** registro, login y JWT (RF-01).
4. **Diario y reseñas:** RF-03 y RF-04, con sus validaciones y reglas.
5. **Backlog y listas:** RF-05.
6. **Cierre:** Swagger, pruebas, manejo de errores y README final.

## Cómo ejecutar

1. Abrir el repositorio en VS Code y elegir **Reopen in Container**.
2. Definir la variable de entorno `RAWG_API_KEY` con la clave obtenida en [rawg.io/apidocs](https://rawg.io/apidocs).
3. Ejecutar `mvn spring-boot:run`.
4. Visitar `http://localhost:8080/hello`.
