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

There may or may not be a test site up at https://www.sciss.de/temp/soundprocesses.js/ .

## notes

From Sébastien:

- `Float32Array` might perform better than `Array[Float]`
- for Firefox performance, forcing ECMAScript 5.1 helps (instead of default ECMAScript 2015)

'Virtual File System' options:

- IndexedDB at [MDN](https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API), at [W3C](https://www.w3.org/TR/IndexedDB/)
- FileSystem API at [MDN](https://developer.mozilla.org/en-US/docs/Web/API/FileSystem), at [CanIUse](https://caniuse.com/?search=FileSystem%20API)
- [BrowserFS](https://jvilk.com/browserfs/2.0.0-beta/index.html)
- [scalajs-indexeddb](https://github.com/math85360/scalajs-indexeddb/blob/master/src/main/scala/com/iz2use/indexeddb/IndexedDB.scala)

## status

The components or pieces that need to come together:

- [X] basic compilation of Lucre, SoundProcesses, FScape
- [X] basic runner functionality, including Widget (LucreSwing)
- [ ] real-time sound production

   - [X] experimental through FScape UGen for WebAudio
   - [ ] through dedicated DSP server on WASM in Scala Native, Rust, or SuperCollider
       
- [ ] artifact management

   - [X] revised multi-filesystem artifact representation
   - [X] local virtual file system
   - [ ] downloading and caching of artifacts
   - [ ] probably reader support for mp3 or other compressed format
       
- [ ] removal of reflection based serialization 

   - [ ] FScape
   - [ ] Ex/Control/Widget
   - [ ] Patterns
   - [ ] SynthGraph
       
- [ ] export of workspaces 

   - [ ] new database back-end that works in SJS
   - [ ] or JSON based export/import
       
- [ ] performance evaluation. Usage of web worker?

For limitations of Scala.js, see also [this Gist](https://gist.github.com/Sciss/22996370ea2a277a409775705d740993)
