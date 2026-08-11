# Building FastTTS from Source

## Prerequisites

- **JDK 17+** — [Download](https://adoptium.net/)
- **Maven 3.9+** — [Download](https://maven.apache.org/download.cgi)

## Quick Build

```bash
# Build JAR
mvn clean package
```

## Build Commands

| Command | Purpose |
|---------|---------|
| `mvn clean compile` | Compile Java only |
| `mvn clean package` | Build FatJAR with dependencies |
| `mvn test` | Run unit tests |
| `mvn clean install` | Build and install to local Maven repository |

## Maven Build

The Maven `pom.xml` will automatically:
- Compile Java sources
- Bundle all dependencies into a single JAR
- Include native resources if present
- Generate source and Javadoc JARs

## Troubleshooting

**"Java version mismatch"** — Ensure JDK 17+ is installed and JAVA_HOME is set.

**"Cannot resolve dependencies"** — Check your internet connection and Maven repository configuration.

**"Build fails with compilation errors"** — Ensure all dependencies are available in Maven Central or JitPack.
