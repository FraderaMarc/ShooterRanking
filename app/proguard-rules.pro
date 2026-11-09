
# ShooterRanking - Optimizacion R8 para Google Play
#
# Se utilizan las reglas predeterminadas optimizadas
# de Android y las reglas de las dependencias.
#
# Firebase, Jetpack Compose, AdMob y UMP incluyen
# reglas de consumidor para sus integraciones.
#
# Los documentos Firestore de ShooterRanking
# se convierten mediante nombres de campos explicitos.
#
# No utilizar reglas -keep globales que impidan
# optimizar y ofuscar el codigo de la aplicacion.

# Compactar los paquetes de las clases que R8
# pueda renombrar de forma segura.
-repackageclasses
