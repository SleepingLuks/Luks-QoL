Luks QoL — сборка

Нужно: JDK 25 и Gradle 9.4+ (или gradlew из шаблона Fabric).

1) Если в папке нет gradlew / gradlew.bat / папки gradle:
   - скопируйте их из шаблона с fabricmc.net/develop (Minecraft 26.3),
   - либо, если установлен Gradle 9.4+, выполните:  gradle wrapper
2) Сборка:
   Windows:    gradlew.bat build
   Mac/Linux:  ./gradlew build
