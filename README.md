# SoundProcesses - JS - Test

## statement

A sandbox project to eventually find out if we can make it possible to run a
[SoundProcesses](https://git.iem.at/sciss/SoundProcesses) workspace in the browser, 
by building everything necessary
for [Scala.js](https://www.scala-js.org/).

This project is (C)opyright 2020 by Hanns Holger Rutz. All rights reserved.
Like SoundProcesses, on which it depends, it is released under the 
[GNU Affero General Public License](https://github.com/Sciss/SoundProcessesJSTest/raw/main/LICENSE) v3+
and comes with absolutely no warranties. To contact the author, send an e-mail to `contact at sciss.de`.

## running

Compile with `sbt fastOptJS`, then open [index-fast.html](index-fast.html), or
compile with `sbt fullOptJS`, then open [index.html](index.html).

## notes

From Sébastien:

- `Float32Array` might perform better than `Array[Float]`
- for Firefox performance, forcing ECMAScript 5.1 helps (instead of default ECMAScript 2015)

## status

See also [this Gist](https://gist.github.com/Sciss/22996370ea2a277a409775705d740993)

