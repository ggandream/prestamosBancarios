Hola, buenas noches. Estas serían las asignaciones. @luis hice un pequeño cambio en donde me enforcaría más en el backend para llevar las versiones en git, y creo que en docker solo sería de pasar el archivo resultante.

Andrea:
1. Crear un docker compose (docker-compose.yml) para la base de datos con PostgreSQL y la aplicación. Dejar las instrucciones para los demás del equipo la podamos levantar.
2. Mapear las entidades en JPA, definiendo las relaciones y restricciones de integridad

Luis (devops):
1. Crear un Dockerfile con multi stage (build con Maven/Gradle → imagen runtime ligera con JRE).
2. Crear el pipeline CI/CD (build, test e imagen).
3. Generación del reporte de cobertura con JaCoCo.

Renato (backend):
1. Implementar la jerarquía de clases definida en el documento y reglas de scoring.
2. Escribir los tests unitarios.
3. Controladores REST API