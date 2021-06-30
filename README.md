# EyeYouReadyForIt

A discord bot that shows pairs of eyes. Your job is to find out whose those eyes belong to.  
The default ones provided in `run/entries.json` belong to artists and actors.

To run:
- Clone this repository.
- Run `./gradlew build` to create a built jar.
- Move the jar in `build/libs` to a different directory.
- Copy over `run/entries.json` into said directory. You can add/remove entries as well.
- `cd` into said directory and run `java -jar EyeYouReadyForIt.jar <token>`
