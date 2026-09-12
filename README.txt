Shooter Ranking - ajuste visual final
======================================

CAMBIOS ÚNICOS:
1. Temporadas y Equipos:
   - Barra superior de 64dp, equivalente al TopAppBar usado en Estadísticas.
   - Botón atrás 48dp con icono visual de 24dp.
   - Título 22sp, normal, una línea y sin mayúsculas forzadas.
   - No se toca navegación, RecyclerView, botones, creación, edición ni borrado.

2. Home:
   - La imagen queda anclada arriba, justo bajo el banner superior.
   - El bloque de selección Entrenador/Jugador conserva callbacks, textos y tamaños.
   - No se toca ninguna navegación ni funcionalidad.

INSTALACIÓN:
A) Copia la carpeta "app" de este ZIP encima de la carpeta "app" de ShooterRanking
   y acepta reemplazar los 2 archivos incluidos.
B) Copia APLICAR_TITULOS.bat y APLICAR_TITULOS.ps1 a la raíz de ShooterRanking.
C) Haz doble clic en APLICAR_TITULOS.bat.
D) Sync Project with Gradle Files y compila.

El script solo elimina .uppercase() de los títulos de Temporadas y Equipos.
