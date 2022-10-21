# Aries Kotlin Demo

Learning project. Implementation of [alice demo agent](https://github.com/hyperledger/aries-cloudagent-python/blob/main/demo/runners/alice.py) 
from ACA-Py repo in Kotlin. Task from a course [LFS173x: Becoming a Hyperledger Aries Developer](https://training.linuxfoundation.org/training/becoming-a-hyperledger-aries-developer-lfs173/) 
/ Chapter 4: Developing Aries Controllers / Building Your Own Controller.

Main difference from original is an explicit split of controller and framework 
in two separate processes (i.e. they are started separately, see usage below).

## Usage

1. Start agent `docker compose up`.

2. Build controller `./gradlew installDist`.

3. Start controller `./build/install/alice-kt/bin/alice-kt`.

4. Use the same way as the original `alice.py`!
