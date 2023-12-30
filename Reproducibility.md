# Reproducibility

Starting with version 1.24.0 of this mod, this file will list what version of
Java was used to compile the jars. In theory, using the same version of Java
should result in an identical jar due to reproducible builds.

## NeoForge 1.24.0

    $ javac --version
    javac 17.0.9

    $ sha256sum build/libs/TurnBasedMinecraft-NeoForge-1.24.0-all.jar
    584935b6e928ad141a55e4d1a21944cebff5152396782085d145bbe34c29286c  build/libs/TurnBasedMinecraft-NeoForge-1.24.0-all.jar

## Forge 1.24.0

    $ javac --version
    javac 17.0.9

    $ sha256sum build/libs/TurnBasedMinecraft-Forge-1.24.0.jar
    e17c370cdf347b053c7f55091afed77564dcd8f419615bd6ca87babe10329c07  build/libs/TurnBasedMinecraft-Forge-1.24.0.jar
