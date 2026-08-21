# Telegram FAQ Assistant Bot

Bot de Telegram desarrollado en Java 21 y Spring Boot para la gestión y respuesta automatizada de Preguntas Frecuentes (FAQs). Permite la administración en tiempo real de la base de conocimientos sin necesidad de reiniciar la aplicación.

## Características

- Búsqueda por palabras clave: Normalización de texto y coincidencia automática con respuestas preconfiguradas.
- Actualización en caliente: Modificación de preguntas y respuestas vía comandos en Telegram sin reiniciar el servicio.
- Importación/Exportación masiva: Soporte para importar/exportar bases de conocimiento completas mediante archivos `.json` y `.csv`.
- Control de acceso (RBAC): Restricción de comandos administrativos únicamente a los IDs de Telegram autorizados.

## Requisitos Previos

* Java JDK 21 o superior.
* Apache Maven 3.8+.
* Cuenta en Telegram y un bot creado a través de [@BotFather](https://t.me/BotFather).

## Variables de Entorno

El proyecto requiere las siguientes variables para su ejecución:

`TELEGRAM_BOT_TOKEN` Token HTTP API provisto por BotFather.
`TELEGRAM_BOT_USERNAME` Username del bot (sin el prefijo `@`).
`TELEGRAM_ADMIN_IDS` Lista de IDs numéricos de administradores (separados por coma).

## Instalación y Ejecución (En local)

1. Clonar el repositorio:
```
bash
   git clone <URL_DEL_REPOSITORIO>
   cd <CARPETA_PROYECTO>
```

2. Configurar las variables de entorno en la terminal:

```
bash

export TELEGRAM_BOT_TOKEN="tu_token"
export TELEGRAM_BOT_USERNAME="tu_bot_username"
export TELEGRAM_ADMIN_IDS="tu_telegram_user_id"
```

3. Compilar y empaquetar:

```
bash

mvn clean package -DskipTests
```

4. Ejecutar la app:

```
bash

mvn spring-boot:run

```

## Guía de Interacción y Comandos
- Usuarios Generales. Cualquier mensaje de texto — El bot buscará coincidencias en la base de FAQs y devolverá la respuesta
correspondiente de forma automática. (Los comandos directos están deshabilitados para no administradores).

### Comandos de Administración (Solo usuarios en TELEGRAM_ADMIN_IDS)
/start — Inicia el bot para administradores.

/help — Muestra el menú de comandos disponibles.

/listfaqs — Lista todas las FAQs registradas junto con sus palabras clave.

/addfaq <kw1, kw2, ...> | <Respuesta> — Agrega una nueva FAQ.

Ejemplo: /addfaq horarios, atencion, abrir | Nuestro horario es de 9:00 a 18:00 hs.

/delfaq <keyword> — Elimina la FAQ que coincida con la palabra clave dada.

Ejemplo: /delfaq horarios

/updatefaqs — Fuerza la recarga en memoria del archivo de FAQs desde el disco.

/exportfaqs — El bot envía por el chat el archivo faqs.json con la base actual.

/importfaqs — Permite sobrescribir la base enviando un archivo .json o .csv adjunto con el texto /importfaqs en el comentario (caption).

## Formato de los archivos para importar

### JSON
```
[
  {
    "keywords": ["palabras", "clave"],
    "answer": "Respuesta predeterminada"
  },
  {
    "keywords": ["ubicacion", "donde estan", "direccion"],
    "answer": "Nos encontramos en Av. Corrientes 1234."
  }
]
```

### CSV (separados por punto y coma)
```
keywords;answer
horarios, abrir, atencion;Atendemos de lunes a viernes de 09:00 a 18:00 hs.
ubicacion, donde estan, direccion;Nos encontramos en Av. Corrientes 1234.
```
