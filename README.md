
<img src="/.idea/icon.png" alt="Kalium Logo" style="max-width:100%;">

# Kalium
[![JVM & JS Tests](https://github.com/wireapp/kalium/actions/workflows/gradle-jvm-tests.yml/badge.svg)](https://github.com/wireapp/kalium/actions/workflows/gradle-jvm-tests.yml)
[![codecov](https://codecov.io/gh/wireapp/kalium/branch/develop/graph/badge.svg?token=UWQ1P7DY7I)](https://codecov.io/gh/wireapp/kalium)

## How to build

### GitHub Packages authentication

In order to download some open source libraries published on GitHub Pacakges, a GitHub Personal Access Token is needed with `packages:read` scope.

1. You can generate one quickly [on this page](https://github.com/settings/tokens/new?description=ReadPackages&scopes=read:packages). Or, for more details, see [Creating a personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token).

2. In `local.properties` add:
```
github.package_registry.user=<github_username>
github.package_registry.token=<github_token>
```

Alternatively the credentials are also read from your environment if `GITHUB_USER` and `GITHUB_TOKEN` exists.

> *Note*: See [GitHub packages docs](https://docs.github.com/en/packages/learn-github-packages/introduction-to-github-packages#authenticating-to-github-packages) for more details and.

### Dependencies

- JDK 11 (ex: openjdk-11-jdk on Ubuntu)
- [libsodium](https://github.com/jedisct1/libsodium)
- [cryptobox-c](https://github.com/wireapp/cryptobox-c)
- [cryptobox4j](https://github.com/wireapp/cryptobox4j)

### Supported Platforms

- Android (see the [Android Reloaded](https://github.com/wireapp/wire-android-reloaded) module)
- JVM (see the [cli](https://github.com/wireapp/kalium/tree/develop/cli) module)
- iOS (partially)
- JavaScript (just a tiny bit)

The `cli` can be executed on the terminal of any machine that 
satisfies the dependencies mentioned above, and is capable of actions like:
- Logging in
- Create a group conversation
- Add user to group conversation
- Receive and send text messages in real time
- Remove another client from your account remotely
- Refill MSL key packages

#### Building dependencies on macOS 12

Just run `make`, which will download and compile dependencies listed above from source, 
the output will be `$PROJECT_ROOT$/native/libs`

#### Running on your machine

When running any tasks that require the native libraries (`libsodium`, `cryptobox-c` 
and `cryptobox4j`), you need to pass their location as VM options like so:

```
-Djava.library.path=./path/to/native/libraries/mentioned/before
```

For example, if you want to run the task `jvmTest` and the libraries are in `./native/libs`:

```
./gradlew jvmTest -Djava.library.path=./native/libs
```

#### Running the CLI

##### JVM

Run the following with the native libs in the
classpath (-Djava.library.path=/usr/local/lib/:./native/libs):

```
./gradlew :cli:assemble
java -Djava.library.path=/usr/local/lib/:./native/libs -jar cli/build/libs/cli.jar login 
```

or if you want the jar file deleted after your run:

```
./gradlew :cli:run --args="login" -Djava.library.path=/usr/local/lib/:./native/libs
```

##### Native (Mac)

For running on arm64 mac
```
./gradlew :cli:macosArm64Binaries
./cli/build/bin/macosArm64/debugExecutable/cli.kexe login
```

For running on intel mac
```
./gradlew :cli:macosX64Binaries
./cli/build/bin/macosX64/debugExecutable/cli.kexe login
```

#### Detekt rules

We use and try to maintain our codestyle uniformed, so apart from having our checks in place in our
CI. You can have live feedback using the IDE, here is how:

1. IntelliJ -> Settings -> Plugins -> Marketplace -> Search and install "Detekt"
2. Settings -> Tools -> Detekt -> set: (replace $PROJECT_ROOT accordingly to your machine)

    - Configuration Files: $PROJECT_ROOT/detekt/detekt.yml
    - Baseline File: $PROJECT_ROOT/detekt/baseline.yml (optional)
    - Plugin Jars: $PROJECT_ROOT/detekt-rules/build/libs/detekt-rules.jar (this will add our custom
      rules to provide live feedback)

or

You can run locally in your terminal:

```
./gradlew clean detekt
```

#### Logo

The logo is adapted from [OpenMoji](https://openmoji.org/) – the open-source emoji and icon project. License: [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/)
