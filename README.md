# EyeYouReadyForIt

A discord bot that shows pairs of eyes. Your job is to find out whose those eyes belong to.  
The default ones provided in `run/entries.json` belong to artists and actors.

To run:
- Clone this repository.
- Run `./gradlew build` and copy `app-all.jar` into a directory of your choice.
- Create a PostgreSQL database anywhere, make a new table called `eyes_entries` with the following schema:
    | Name               | Datatype     |
    |--------------------|--------------|
    | [Primary Key] name | VARCHAR(255) |
    | image_url          | VARCHAR(255) |
    | hint               | VARCHAR(255) |
    | aliases            | VARCHAR[]    |

- Either set the following Environment Variables or System Properties when running the bot:
    | Environment Variable | System Property  | Value                            |
    |----------------------|------------------|----------------------------------|
    | EYRFI_TOKEN          | eyrfi.token      | Bot token                        |
    | EYRFI_DB_URL         | eyrfi.dbURL      | Connection String for PostgreSQL |
    | EYRFI_DB_USER        | eyrfi.dbUser     | Username for PostgreSQL          |
    | EYFRI_DB_PASSWORD    | eyrfi.dbPassword | Password for PostgreSQL          |

- Run the bot like any other Java program.