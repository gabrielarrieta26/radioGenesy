# Radio genesy

Aplicación local de radio con dos vistas sincronizadas: **oyente** (`/`) y **estudio** (`/admin`).

## Requisitos

- Java 17
- MySQL con una base de datos llamada `radio`
- Maven Wrapper incluido (`mvnw` / `mvnw.cmd`)

## Configuración

Editar `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/radio
spring.datasource.username=root
spring.datasource.password=
radio.musica.carpeta=C:/proyectos/radio/musica
radio.musica.programa-id=1
radio.musica.intervalo-ms=300000
```

## Ejecutar

```powershell
.\mvnw.cmd spring-boot:run
```

- Oyente: http://localhost:8090/radio/
- Estudio: http://localhost:8090/radio/admin

## Funcionalidades

- Reproducción compartida entre estudio y oyente con posición, play/pausa y avance automático.
- Solo una vista suena a la vez: el oyente si está abierta; si no, el estudio.
- Sincronización automática al recargar cualquier vista.
- Anuncio hablado de la hora, manual o automático cada hora.
- Sincronización de audios desde una carpeta local, con portadas opcionales.
- Historial de últimas 5 pistas en el oyente y 10 en el estudio.

## Convención de archivos de música

Dentro de `radio.musica.carpeta`:

- Audio: `Artista - Título.mp3` (también `.wav`, `.ogg`, `.m4a`, `.aac`, `.flac`, `.opus`).
- Portada opcional: imagen con el mismo nombre base (`.jpg`, `.jpeg`, `.png`, `.webp`, `.svg`).

## Pruebas

```powershell
.\mvnw.cmd test
```
