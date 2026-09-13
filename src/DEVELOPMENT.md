# Development

## Running

```
./gradlew run
```

If you want to override the language, you can use `-Duser.country` and `-Duser.language`, for example:

```
./gradlew run -Duser.country=BR -Duser.language=pt
```

## Prerequisites

A full **JDK 25+** (including compiler `javac` and `jpackage`, not just headless JRE) is required to build:

- **Fedora / RHEL:** `sudo dnf install -y java-25-openjdk-devel java-25-openjdk flatpak-builder`
- **Debian / Ubuntu:** `sudo apt install -y openjdk-25-jdk flatpak-builder rpm`
- **Arch Linux:** `sudo pacman -S jdk25-openjdk flatpak-builder rpm-tools`

## Packaging

MuzikPlayer can be packaged as Flatpak, standalone `.deb`/`.rpm`, or portable `.tar.gz` bundles.

### Flatpak
Make sure you have the following installed:

- `flatpak-builder`
- Flatpak runtimes:
  ```
  flatpak install flathub org.freedesktop.Platform//26.08 org.freedesktop.Sdk//26.08
  ```


To build and install:

```
./flatpak/build.sh
```

To run:

```
flatpak run io.github.coderaulia.MuzikPlayer
```
