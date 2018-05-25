# Cava: ConsenSys Core Libraries for Java (& Kotlin)

[![Build Status](https://circleci.com/gh/ConsenSys/cava.svg?style=shield&circle-token=440c81af8cae3c059b516a8e375471258d7e0229)](https://circleci.com/gh/ConsenSys/cava) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/consensys/cava/blob/master/LICENSE)

In the spirit of [Google Guava](https://github.com/google/guava/), Cava is a set of libraries and other tools to aid development of blockchain and other decentralized software in Java and other JVM languages.

It includes a low-level bytes library, serialization and deserialization codecs (e.g. [RLP](https://github.com/ethereum/wiki/wiki/RLP)), various cryptography functions and primatives, and lots of other helpful utilities.

Cava is developed for JDK 1.8 or higher, and depends on various other FOSS libraries, including Guava.

## Build Instructions

To build, clone this repo and run with `./gradlew` like so:

```
git clone --recursive https://github.com/ConsenSys/cava
cd cava
./gradlew
```

After a successful build, libraries will be available in `build/libs`.

## Links

- [GitHub project](https://github.com/consensys/cava)
- [Issue tracker: Report a defect or feature request](https://github.com/google/cava/issues/new)
- [StackOverflow: Ask "how-to" and "why-didn't-it-work" questions](https://stackoverflow.com/questions/ask?tags=cava+java)
- [cava-discuss: For open-ended questions and discussion](http://groups.google.com/group/cava-discuss)
