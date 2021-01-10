# SoundProcesses - JS - Test

[![Build Status](https://github.com/Sciss/SoundProcessesJSTest/workflows/Scala%20CI/badge.svg?branch=main)](https://github.com/Sciss/SoundProcessesJSTest/actions?query=workflow%3A%22Scala+CI%22)

## statement

A test project to develop the mechanisms necessary to run a
[SoundProcesses](https://git.iem.at/sciss/SoundProcesses) workspace in the browser, 
by building everything necessary for [Scala.js](https://www.scala-js.org/).

This project is (C)opyright 2020–2021 by Hanns Holger Rutz. All rights reserved.
Like SoundProcesses, on which it depends, it is released under the 
[GNU Affero General Public License](https://github.com/Sciss/SoundProcessesJSTest/raw/main/LICENSE) v3+
and comes with absolutely no warranties. To contact the author, send an e-mail to `contact at sciss.de`.

## scsynth.wasm

I'm now working on real-time sound synthesis support (`Proc` abstraction), and the WebAssembly build
of the SuperCollider server is included as binary in the `lib` directory. SuperCollider is covered by GNU GPL v2+,
see [here](https://github.com/Sciss/supercollider/tree/wasm) for source code.
See the [ScalaCollider JS Test](https://github.com/Sciss/ScalaColliderJSTest) for a smaller test project
focusing only on ScalaCollider.

## running

The project builds with [sbt](https://www.scala-sbt.org/).

Compile with `sbt fastOptJS` or `sbt fullOptJS`, then open [index.html](index.html).
To use scsynth.wasm, you must run a web server, such as

    python -m SimpleHTTPServer

(then open its default page, like [127.0.0.1:8000](http://127.0.0.1:8000))

There may or may not be a test site up
at [www.sciss.de/temp/soundprocesses.js](https://www.sciss.de/temp/soundprocesses.js/). For scsynth.wasm,
you need to use either Chrome/Chromium, or Firefox 79 or newer (Firefox 85 has been confirmed to work).

## status

The components or pieces that need to come together:

- [X] basic compilation of Lucre, SoundProcesses, FScape
- [X] basic runner functionality, including Widget (LucreSwing)
- [X] real-time sound production

   - [X] experimental through FScape UGen for WebAudio
   - [X] through dedicated DSP server on WASM in Scala Native, Rust, or __SuperCollider__
       
- [ ] artifact management

   - [X] revised multi-filesystem artifact representation
   - [X] local virtual file system
   - [ ] downloading and caching of artifacts
   - [ ] probably reader support for mp3 or other compressed format
       
- [ ] removal of reflection based serialization 

   - [X] SynthGraph
   - [ ] Ex/Control/Widget
   - [ ] FScape
   - [ ] Patterns
  
- [ ] export of workspaces 

   - [ ] new database back-end that works in SJS, or JSON/binary based export/import
       
- [ ] performance evaluation. Usage of web worker?

## notes

From Sébastien:

- `Float32Array` might perform better than `Array[Float]`
- for Firefox performance, forcing ECMAScript 5.1 helps (instead of default ECMAScript 2015)

'Virtual File System' options:

- solved now in [AsyncFile](https://github.com/Sciss/AsyncFile)
- IndexedDB at [MDN](https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API), at [W3C](https://www.w3.org/TR/IndexedDB/)
- FileSystem API at [MDN](https://developer.mozilla.org/en-US/docs/Web/API/FileSystem), at [CanIUse](https://caniuse.com/?search=FileSystem%20API)
- [BrowserFS](https://jvilk.com/browserfs/2.0.0-beta/index.html)
- [scalajs-indexeddb](https://github.com/math85360/scalajs-indexeddb/blob/master/src/main/scala/com/iz2use/indexeddb/IndexedDB.scala)

For limitations of Scala.js, see also [this Gist](https://gist.github.com/Sciss/22996370ea2a277a409775705d740993)
