# Reproducibility

Starting with version 1.24.0 of this mod, this file will list what version of
Java was used to compile the jars. In theory, using the same version of Java
should result in an identical jar due to reproducible builds.

## NeoForge 1.25.0

    $ java --version
    openjdk 17.0.9 2023-10-17
    OpenJDK Runtime Environment (build 17.0.9+8)
    OpenJDK 64-Bit Server VM (build 17.0.9+8, mixed mode)

    $ javac --version
    javac 17.0.9

    $ sha256sum build/libs/TurnBasedMinecraft-NeoForge-1.25.0-all.jar
    0e5eacc8aefd3b1a1c8e6c9657108172934fae2e727547ca7c12f9ff79ce4e8e  build/libs/TurnBasedMinecraft-NeoForge-1.25.0-all.jar

## Forge 1.25.0

    $ java --version
    openjdk 17.0.9 2023-10-17
    OpenJDK Runtime Environment (build 17.0.9+8)
    OpenJDK 64-Bit Server VM (build 17.0.9+8, mixed mode)

    $ javac --version
    javac 17.0.9

    $ sha256sum build/libs/TurnBasedMinecraft-Forge-1.25.0-all.jar
    51ef854552b180df68969f4cec6fdc8716ef519b947948b9e5f4ce9953d00162  build/libs/TurnBasedMinecraft-Forge-1.25.0-all.jar

## NeoForge 1.24.0

    $ java --version
    openjdk 17.0.9 2023-10-17
    OpenJDK Runtime Environment (build 17.0.9+8)
    OpenJDK 64-Bit Server VM (build 17.0.9+8, mixed mode)

    $ javac --version
    javac 17.0.9

    $ sha256sum build/libs/TurnBasedMinecraft-NeoForge-1.24.0-all.jar
    584935b6e928ad141a55e4d1a21944cebff5152396782085d145bbe34c29286c  build/libs/TurnBasedMinecraft-NeoForge-1.24.0-all.jar

## Forge 1.24.0

    $ java --version
    openjdk 17.0.9 2023-10-17
    OpenJDK Runtime Environment (build 17.0.9+8)
    OpenJDK 64-Bit Server VM (build 17.0.9+8, mixed mode)

    $ javac --version
    javac 17.0.9

    $ sha256sum build/libs/TurnBasedMinecraft-Forge-1.24.0.jar
    e17c370cdf347b053c7f55091afed77564dcd8f419615bd6ca87babe10329c07  build/libs/TurnBasedMinecraft-Forge-1.24.0.jar
