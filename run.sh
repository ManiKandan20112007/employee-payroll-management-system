#!/bin/sh
mkdir -p out && javac -encoding UTF-8 -d out src/epms/*.java && java -cp out epms.EpmsApp
