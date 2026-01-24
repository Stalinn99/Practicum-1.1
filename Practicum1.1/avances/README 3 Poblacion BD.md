# Documentación: Población de Base de Datos en Scala

## Ubicación en el Main

La población de base de datos se ejecuta en la **FASE 17** del proceso, específicamente en esta línea del archivo `Main.scala`:

```scala
// FASE 17: Población BD
_ <- IO.println("\n>>> FASE 17: POBLACIÓN DE BASE DE DATOS")
_ <- pipeline(transactor)
```

**Ubicación exacta:** Después de completar todos los análisis exploratorios (fases 1-16), al final de la función `run: IO[Unit]`

---

## Flujo de la Función `pipeline()`

```scala
def pipeline(transactor: doobie.Transactor[IO]):IO[Unit] =
  Files[IO].readAll(filePath)
    .through(fs2.text.utf8.decode)
    .through(attemptDecodeUsingHeaders[Map[String, String]](separator = ';'))
    .zipWithIndex
    .evalMap {
      case (Right(row), index) =>
        PoblarBaseDatos.populateAll(row)
          .transact(transactor)
          .handleErrorWith { e =>
            PoblarBaseDatos.logError(index, e)
          } *>
          PoblarBaseDatos.logProgress(index, interval = 1000)
      case (Left(error), index) =>
        IO.println(s"Fila $index ignorada por formato inválido: ${error.getMessage}")
    }
    .compile.drain
```

### Desglose paso a paso:

| Paso | Código                                     | Descripción                                                  |
| ---- | ------------------------------------------ | ------------------------------------------------------------ |
| 1    | `Files[IO].readAll(filePath)`              | Lee el archivo CSV en modo streaming (bytes sin procesar)    |
| 2    | `.through(fs2.text.utf8.decode)`           | Decodifica los bytes a texto UTF-8                           |
| 3    | `.through(attemptDecodeUsingHeaders[...])` | Parsea el CSV usando headers como claves de mapa             |
| 4    | `separator = ';'`                          | Especifica que las columnas están separadas por punto y coma |
| 5    | `.zipWithIndex`                            | Añade un índice (0, 1, 2, ...) a cada fila para tracking     |
| 6    | `case (Right(row), index)`                 | Maneja filas que se parsearon correctamente                  |
| 7    | `PoblarBaseDatos.populateAll(row)`         | Llama la función que inserta en TODAS las tablas             |
| 8    | `.transact(transactor)`                    | Envuelve las operaciones en una transacción ACID             |
| 9    | `.handleErrorWith { e => ... }`            | Si hay error, registra y continúa                            |
| 10   | `PoblarBaseDatos.logProgress(...)`         | Imprime progreso cada 1000 filas                             |
| 11   | `case (Left(error), index)`                | Maneja errores de parsing del CSV                            |
| 12   | `.compile.drain`                           | Ejecuta el stream completo descartando resultados            |

---

## Función Principal: `PoblarBaseDatos.populateAll(row)`

Esta es la función que realiza TODAS las inserciones en la base de datos.

```scala
def populateAll(row: Map[String, String]): ConnectionIO[Unit]
```

**Parámetro:** `row` es un mapa donde las claves son los nombres de columnas CSV y los valores son los datos de la película.

**Retorno:** `ConnectionIO[Unit]` - Operación composable para Doobie que se ejecuta en una transacción.

---

## Tablas y Operaciones de Inserción

### **1. Tabla: Idiomas**

```scala
val insertIdioma = sql"INSERT IGNORE INTO Idiomas (iso_639_1, name)
  VALUES ($origLang, 'Unknown')".update.run
```

- **Columnas:** `iso_639_1` (código de idioma), `name`
- **Origen:** Campo `original_language` del CSV (tomando los primeros 2 caracteres)
- **INSERT IGNORE:** No inserta si ya existe (evita duplicados)
- **Dato de ejemplo:** `en` para inglés, `es` para español

---

### **2. Tabla: Peliculas (Principal)**

```scala
val insertPelicula = sql"""
  INSERT IGNORE INTO Peliculas (
    idPelicula, imdb_id, title, original_title, overview, tagline,
    adult, video, status, release_date, budget, revenue, runtime,
    popularity, vote_average, vote_count, homepage, poster_path,
    original_language
  ) VALUES (
    $pId,
    ${row.getOrElse("imdb_id", "").trim.take(20)},
    ${row.getOrElse("title", "").trim.take(150)},
    ...
  )
""".update.run
```

**Campos insertados:**

| Campo               | Origen CSV       | Validación                                        |
| ------------------- | ---------------- | ------------------------------------------------- |
| `idPelicula`        | `id`             | Convertido a Int, descarta si es 0                |
| `imdb_id`           | `imdb_id`        | String máximo 20 caracteres                       |
| `title`             | `title`          | String máximo 150 caracteres                      |
| `original_title`    | `original_title` | String máximo 150 caracteres                      |
| `overview`          | `overview`       | String máximo 2000 caracteres                     |
| `tagline`           | `tagline`        | String máximo 255 caracteres                      |
| `adult`             | `adult`          | Booleano (0 o 1)                                  |
| `video`             | `video`          | Booleano (0 o 1)                                  |
| `status`            | `status`         | String máximo 20 caracteres                       |
| `release_date`      | `release_date`   | Validación regex YYYY-MM-DD, default "1900-01-01" |
| `budget`            | `budget`         | Double con `safeDouble()`                         |
| `revenue`           | `revenue`        | Double con `safeDouble()`                         |
| `runtime`           | `runtime`        | Int con `safeInt()`                               |
| `popularity`        | `popularity`     | Double con `safeDouble()`                         |
| `vote_average`      | `vote_average`   | Double con `safeDouble()`                         |
| `vote_count`        | `vote_count`     | Int con `safeInt()`                               |
| `homepage`          | `homepage`       | URL string máximo 255 caracteres                  |
| `poster_path`       | `poster_path`    | Path string máximo 255 caracteres                 |
| `original_language` | Derivado         | Código de idioma (2 caracteres)                   |

---

### **3. Tabla: Generos y Relación Peliculas_Generos**

```scala
val insertGeneros = genres.traverse { g =>
  sql"INSERT IGNORE INTO Generos (idGenero, name)
    VALUES (${g.id}, ${g.name.take(50)})".update.run *>
    sql"INSERT IGNORE INTO Peliculas_Generos (idPelicula, idGenero)
      VALUES ($pId, ${g.id})".update.run
}
```

**Proceso:**

1. Parsea el JSON del campo `genres` del CSV
2. Por cada género:
   - Inserta en `Generos` si no existe
   - Inserta relación M:N en `Peliculas_Generos`

**Datos:** Lista de objetos `Genres` con estructura:

```json
[
  { "id": 28, "name": "Action" },
  { "id": 12, "name": "Adventure" }
]
```

---

### **4. Tabla: Keywords y Relación Peliculas_Keywords**

```scala
val insertKeywords = keywords.traverse { k =>
  sql"INSERT IGNORE INTO Keywords (idKeyword, name)
    VALUES (${k.id}, ${k.name.take(100)})".update.run *>
    sql"INSERT IGNORE INTO Peliculas_Keywords (idPelicula, idKeyword)
      VALUES ($pId, ${k.id})".update.run
}
```

**Similar a Géneros:**

- Parsea JSON del campo `keywords`
- Inserta palabra clave si no existe
- Crea relación M:N

**Datos:** Lista de objetos `Keywords`:

```json
[
  { "id": 1234, "name": "superhero" },
  { "id": 5678, "name": "action" }
]
```

---

### **5. Tabla: Paises y Relación Peliculas_Paises**

```scala
val insertCountries = countries.traverse { c =>
  sql"INSERT IGNORE INTO Paises (iso_3166_1, name)
    VALUES (${c.iso_3166_1.take(2)}, ${c.name.take(100)})".update.run *>
    sql"INSERT IGNORE INTO Peliculas_Paises (idPelicula, iso_3166_1)
      VALUES ($pId, ${c.iso_3166_1.take(2)})".update.run
}
```

**Proceso:**

- Parsea JSON del campo `production_countries`
- Inserta país con código ISO 3166-1 (2 caracteres)
- Crea relación M:N

**Datos:**

```json
[
  { "iso_3166_1": "US", "name": "United States of America" },
  { "iso_3166_1": "GB", "name": "United Kingdom" }
]
```

---

### **6. Tabla: Companias y Relación Peliculas_Companias**

```scala
val insertCompanies = companies.traverse { c =>
  sql"INSERT IGNORE INTO Companias (idCompania, name, origin_country)
    VALUES (${c.id}, ${c.name.take(100)}, ${c.origin_country.map(_.take(2))})".update.run *>
    sql"INSERT IGNORE INTO Peliculas_Companias (idPelicula, idCompania)
      VALUES ($pId, ${c.id})".update.run
}
```

**Proceso:**

- Parsea JSON del campo `production_companies`
- Inserta compañía con ID único
- Guarda país de origen (opcional)

**Datos:**

```json
[
  { "id": 1, "name": "Universal Pictures", "origin_country": "US" },
  { "id": 2, "name": "Warner Bros", "origin_country": "US" }
]
```

---

### **7. Tabla: Idiomas Hablados y Relación Peliculas_Idiomas**

```scala
val insertSpoken = spokenLangs.traverse { l =>
  sql"INSERT INTO Idiomas (iso_639_1, name)
    VALUES (${l.iso_639_1.take(2)}, ${l.name.take(50)})
    ON DUPLICATE KEY UPDATE name = VALUES(name)".update.run *>
    sql"INSERT IGNORE INTO Peliculas_Idiomas (idPelicula, iso_639_1)
      VALUES ($pId, ${l.iso_639_1.take(2)})".update.run
}
```

**Diferencia:** Usa `ON DUPLICATE KEY UPDATE` en lugar de `INSERT IGNORE` para actualizar si existe.

**Datos:**

```json
[
  { "iso_639_1": "en", "name": "English" },
  { "iso_639_1": "es", "name": "Español" }
]
```

---

### **8. Tabla: Colecciones y Relacion Peliculas_Colecciones**

```scala
val insertCollection = collectionOpt.map { c =>
  sql"INSERT IGNORE INTO Colecciones (idColeccion, name, poster_path, backdrop_path)
    VALUES (${c.id}, ${c.name.take(100)}, ${c.poster_path.map(_.take(255))},
            ${c.backdrop_path.map(_.take(255))})".update.run *>
    sql"INSERT IGNORE INTO Peliculas_Colecciones (idPelicula, idColeccion)
      VALUES ($pId, ${c.id})".update.run
}.getOrElse(FC.unit)
```

**Especial:** Es opcional (`collectionOpt`) - si no existe, no hace nada (`FC.unit`).

**Datos:**

```json
{
  "id": 121,
  "name": "The Lord of the Rings",
  "poster_path": "/path/to/poster.jpg",
  "backdrop_path": "/path/to/backdrop.jpg"
}
```

---

### **9. Tabla: Actores, Departamentos y Relación Asignaciones**

```scala
val insertCast = cast.traverse { a =>
  sql"INSERT IGNORE INTO Actores (idActor, name, gender, profile_path)
    VALUES (${a.cast_id}, ${a.name.take(100)}, ${a.gender.getOrElse(0)},
            ${a.profile_path.map(_.take(255))})".update.run *>
    sql"INSERT IGNORE INTO Asignaciones (idActor, idPelicula, `character`, cast_order, credit_id)
      VALUES (${a.cast_id}, $pId, ${a.character.map(_.take(150))},
              ${a.order.getOrElse(0)}, ${a.credit_id.map(_.take(50))})".update.run
}
```

**Tablas implicadas:**

- `Actores` - Información del actor
- `Asignaciones` - Relación actor-película con rol específico

**Campos:**

- `cast_id`: ID único del actor
- `name`: Nombre del actor
- `gender`: Género (0=no especificado, 1=femenino, 2=masculino)
- `character`: Personaje que interpreta
- `cast_order`: Orden en el elenco (1=primer actor, etc.)
- `credit_id`: ID de crédito único

**Datos:**

```json
[
  {
    "cast_id": 1,
    "name": "Tom Cruise",
    "gender": 2,
    "character": "Maverick",
    "order": 1,
    "credit_id": "abc123",
    "profile_path": "/path/to/profile.jpg"
  }
]
```

---

### **10. Tabla: Personal, Departamentos y Relación Trabajos**

```scala
val insertCrew = crew.traverse { c =>
  for {
    _ <- sql"INSERT IGNORE INTO Departamentos (name)
      VALUES (${c.department.take(50)})".update.run
    _ <- sql"INSERT IGNORE INTO Personal (idPersonal, name, gender, profile_path)
      VALUES (${c.id}, ${c.name.take(100)}, ${c.gender.getOrElse(0)},
              ${c.profile_path.map(_.take(255))})".update.run
    _ <- sql"""
      INSERT IGNORE INTO Trabajos (idPersonal, idPelicula, idDepartamento, job, credit_id)
      VALUES (
        ${c.id}, $pId,
        (SELECT idDepartamento FROM Departamentos WHERE name = ${c.department.take(50)} LIMIT 1),
        ${c.job.take(100)}, ${c.credit_id.map(_.take(50))}
      )
    """.update.run
  } yield ()
}
```

**Tablas implicadas:**

- `Departamentos` - Departamentos de producción
- `Personal` - Miembros del equipo
- `Trabajos` - Relación personal-película con job específico

**Proceso especial:**

1. Inserta el departamento si no existe
2. Inserta el personal
3. Usa SUBCONSULTA para obtener el ID del departamento e inserta la relación

**Datos:**

```json
[
  {
    "id": 5,
    "name": "Steven Spielberg",
    "department": "Directing",
    "job": "Director",
    "gender": 2,
    "credit_id": "def456",
    "profile_path": "/path/to/profile.jpg"
  }
]
```

---

### **11. Tabla: Usuarios y Calificaciones**

```scala
val insertRatings = ratings.traverse { r =>
  sql"INSERT IGNORE INTO Usuarios (userId)
    VALUES (${r.userId})".update.run *>
    sql"INSERT IGNORE INTO Calificaciones (userId, idPelicula, rating, timestamp)
      VALUES (${r.userId}, $pId, ${r.rating}, ${r.timestamp})".update.run
}
```

**Tablas implicadas:**

- `Usuarios` - Usuarios que han puntuado
- `Calificaciones` - Rating de cada usuario a cada película

**Campos:**

- `userId`: ID único del usuario
- `rating`: Puntuación (ej: 7.5)
- `timestamp`: Cuándo se hizo la calificación

**Datos:**

```json
[
  {
    "userId": 1001,
    "rating": 8.5,
    "timestamp": 1609459200
  }
]
```

---

## Funciones Auxiliares de Validación

### **safeInt(s: String): Int**

```scala
def safeInt(s: String): Int = s.trim.toDoubleOption.map(_.toInt).getOrElse(0)
```

- Recibe un string y lo convierte a Int de forma segura
- Si hay error o está vacío, retorna 0
- Útil para: `budget`, `runtime`, `vote_count`

---

### **safeDouble(s: String): Double**

```scala
def safeDouble(s: String): Double = s.trim.toDoubleOption.getOrElse(0.0)
```

- Convierte string a Double de forma segura
- Si hay error, retorna 0.0
- Útil para: `budget`, `revenue`, `popularity`, `vote_average`

---

##  Orden de Ejecución (Transacción)

```scala
for {
  _ <- insertIdioma                    // 1. Inserta idioma original
  _ <- insertPelicula                  // 2. Inserta película principal
  _ <- insertGeneros                   // 3. Inserta géneros y relaciones
  _ <- insertKeywords                  // 4. Inserta keywords y relaciones
  _ <- insertCountries                 // 5. Inserta países y relaciones
  _ <- insertCompanies                 // 6. Inserta compañías y relaciones
  _ <- insertSpoken                    // 7. Inserta idiomas hablados y relaciones
  _ <- insertCollection                // 8. Inserta colección (si existe)
  _ <- insertCast                      // 9. Inserta actores y asignaciones
  _ <- insertCrew                      // 10. Inserta departamentos, personal y trabajos
  _ <- insertRatings                   // 11. Inserta usuarios y ratings
} yield ()
```

**Importante:** Este orden es crucial porque:

- `Peliculas` depende de `Idiomas`
- Las relaciones M:N dependen de sus tablas padre
- `Trabajos` depende de `Departamentos`, `Personal` y `Peliculas`

---

## Manejo de Errores y Progreso

### **logError(index: Long, e: Throwable)**

```scala
def logError(index: Long, e: Throwable): IO[Unit] =
  IO.println(s" ERROR Fila $index: ${e.getMessage}")
```

Se ejecuta si hay excepción durante la inserción.

---

### **logProgress(index: Long, interval: Int)**

```scala
def logProgress(index: Long, interval: Int): IO[Unit] =
  if (index % interval == 0) IO.println(s" Procesadas $index filas") else IO.unit
```