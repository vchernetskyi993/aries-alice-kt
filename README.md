# Aries Kotlin Demo

Learning project. Implementation of [alice demo agent](https://github.com/hyperledger/aries-cloudagent-python/blob/main/demo/runners/alice.py) 
from ACA-Py repo in Kotlin. Task from a course [LFS173x: Becoming a Hyperledger Aries Developer](https://training.linuxfoundation.org/training/becoming-a-hyperledger-aries-developer-lfs173/) 
/ Chapter 4: Developing Aries Controllers / Building Your Own Controller.

Main difference from original is an explicit split of controller and framework 
in two separate processes (i.e. they are started separately, see usage below).

## Usage

1. Build `./gradlew installDist`.

2. Start framework `docker compose up`.

3. Start controller `./build/install/alice-kt/bin/alice-kt`.

4. Input faber invitation once prompted.

5. Use the same way as the original `alice.py`!
