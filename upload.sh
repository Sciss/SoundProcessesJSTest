#!/bin/bash
rsync -rltDuv target/scala-2.13/soundprocesses-js-test-opt.js* www.sciss.de@ssh.strato.de:temp/soundprocesses.js/target/scala-2.13/
