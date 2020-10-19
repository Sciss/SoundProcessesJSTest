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

## status

The following references in SP are invalid:

- `java.util.concurrent.ScheduledExecutorService`
- `java.util.concurrent.ScheduledFuture`
- `java.util.concurrent.Executors`
- `java.lang.Runtime.addShutdownHook` (from `scala.sys.addShutdownHook`)

