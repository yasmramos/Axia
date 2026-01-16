# Configuración del Sistema Axia

## Visión General

El sistema Axia utiliza un gestor de configuración seguro que permite cargar parámetros desde múltiples fuentes con una prioridad definida. Esto facilita el despliegue en diferentes entornos (desarrollo, pruebas, producción) sin modificar el código.

## Fuentes de Configuración

La configuración se carga en el siguiente orden de prioridad (de mayor a menor):

1. **Variables de entorno** - Máxima prioridad
2. **Archivo de configuración externo** -路径 especificado por `AXIA_CONFIG_PATH`
3. **Archivo de propiedades en classpath** - `application.properties`
4. **Valores por defecto** - Si no se encuentra ninguna fuente

## Configuración de Base de Datos

### Método 1: Variables de Entorno (Recomendado para Producción)

Establece las siguientes variables de entorno antes de ejecutar la aplicación:

```bash
# Configuración completa por URL
export AXIA_DB_URL="jdbc:postgresql://localhost:5432/axia"
export AXIA_DB_USER="tu_usuario"
export AXIA_DB_PASSWORD="tu_password_seguro"

# O configuración individual
export AXIA_DB_HOST="localhost"
export AXIA_DB_PORT="5432"
export AXIA_DB_NAME="axia"
export AXIA_DB_USER="tu_usuario"
export AXIA_DB_PASSWORD="tu_password_seguro"
```

### Método 2: Archivo de Configuración Externo

Crea un archivo de configuración personalizado y especifica su ruta:

```bash
# Linux/macOS
export AXIA_CONFIG_PATH="/etc/axia/axia.properties"

# Windows (Command Prompt)
set AXIA_CONFIG_PATH=C:\ProgramData\axia\config.properties

# Windows (PowerShell)
$env:AXIA_CONFIG_PATH="C:\ProgramData\axia\config.properties"
```

Ejemplo de archivo de configuración externo:

```properties
datasource.db.url=jdbc:postgresql://db.example.com:5432/axia_production
datasource.db.username=admin
datasource.db.password=secure_password_here
datasource.db.minConnections=5
datasource.db.maxConnections=20
```

### Método 3: Archivo application.properties

Para desarrollo, puedes modificar directamente el archivo `src/main/resources/application.properties`:

```properties
datasource.db.url=jdbc:postgresql://localhost:5432/axia
datasource.db.username=postgres
datasource.db.password=tu_password
```

## Variables de Entorno Soportadas

| Variable | Descripción | Valor por Defecto |
|----------|-------------|-------------------|
| `AXIA_DB_URL` | URL JDBC completa | `jdbc:postgresql://localhost:5432/axia` |
| `AXIA_DB_USER` | Usuario de base de datos | `postgres` |
| `AXIA_DB_PASSWORD` | Password de base de datos | (vacío) |
| `AXIA_DB_HOST` | Servidor de base de datos | `localhost` |
| `AXIA_DB_PORT` | Puerto de base de datos | `5432` |
| `AXIA_DB_NAME` | Nombre de la base de datos | `axia` |
| `AXIA_CONFIG_PATH` | Ruta a archivo de configuración externo | (no definido) |

## Configuración de Conexión

### Pool de Conexiones

```properties
# Número mínimo de conexiones
datasource.db.minConnections=1

# Número máximo de conexiones
datasource.db.maxConnections=10
```

### Ebean ORM

```properties
# Generar DDL automáticamente
ebean.db.ddl.generate=true

# Ejecutar DDL automáticamente
ebean.db.ddl.run=true

# Solo crear tablas (no modificar) - útil para producción
ebean.db.ddl.createOnly=false

# Nivel de logging de queries
ebean.db.queryLogLevel=SQL
```

## Configuración para Contenedores (Docker)

Al ejecutar en contenedores, utiliza variables de entorno:

```dockerfile
FROM openjdk:17-jdk-slim

ENV AXIA_DB_HOST=db
ENV AXIA_DB_PORT=5432
ENV AXIA_DB_NAME=axia
ENV AXIA_DB_USER=axia_user
ENV AXIA_DB_PASSWORD=${DB_PASSWORD}

COPY target/axia-accounting-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Seguridad

### Buenas Prácticas

1. **Nunca guards credenciales en el código fuente**
2. **Utiliza variables de entorno para producción**
3. **Archivo `.gitignore` debe excluir `application.properties`**
4. **Rota las contraseñas periódicamente**
5. **Limita los permisos del usuario de base de datos**

### Archivos a Excluir de Git

Asegúrate de que tu `.gitignore` incluya:

```gitignore
# Configuration files with credentials
application.properties
*.secret
*.credentials
config/

# Environment-specific files
.env
.env.local
```

## Verificación de Configuración

La aplicación registrará la fuente de configuración al iniciar:

```
INFO: Configuration loaded from environment
INFO: Database connection established
```

Para verificar la configuración en tiempo de ejecución, el `ConfigManager` proporciona un método seguro:

```java
ConfigManager config = ConfigManager.getInstance();
String source = config.getConfigSource();  // Indica qué fuente se usó
String maskedUrl = config.getDatabaseUrl(); // URL con password oculto
```

## Solución de Problemas

### Error: "Database connection failed"

1. Verifica que la base de datos esté ejecutándose
2. Confirma las credenciales usando variables de entorno
3. Revisa los logs para ver el origen de configuración usado

### Error: "Configuration not found"

1. Asegúrate de que `application.properties` existe en classpath
2. Verifica la variable `AXIA_CONFIG_PATH`
3. Consulta los logs para ver qué fuente de configuración se intentó usar

### Contraseña No Funciona

1. Verifica que no haya espacios en blanco extra
2. Confirma que la variable de entorno está correctamente exportada
3. Para caracteres especiales, usa comillas en la variable de entorno
