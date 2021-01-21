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

Compile with `sbt -J-Xmx2G fastOptJS` or `sbt -J-Xmx2G fullOptJS`. The former compiles faster, whereas the
latter takes longer and is meant to minimise the JavaScript file size. Currently `fastOptJS` creates a 30 MB
file, whereas `fullOptJS` comes down to 5 MB, which makes a big difference when downloading from a website.

After compilation, the [index.html](index.html) can be used to run the application.
To use scsynth.wasm, you must run a web server, such as

    python -m SimpleHTTPServer

(then open its default page, like [127.0.0.1:8000](http://127.0.0.1:8000))

There may or may not be a test site up
at [www.sciss.de/temp/soundprocesses.js](https://www.sciss.de/temp/soundprocesses.js/). For scsynth.wasm,
you need to use either Chrome/Chromium, or Firefox 79 or newer (Firefox 85 has been confirmed to work).

## workspace

The current version, instead of building a SoundProcesses structure from scratch (source file
`DirectWorkspace`), loads an existing workspace (see `LoadWorkspace`). This is a workspace exported
from Mellite as a 'binary blob', the file `workspace.mllt.bin`. The original desktop
workspace for Mellite is `workspace.mllt`. If you want to test other workspaces, you need to use
Mellite 3.4.0-SNAPSHOT or newer, and in a workspace's root folder choose the menu item
_File_ > _Export Binary Workspace_, then overwrite `workspace.mllt.bin`. The root folder must
contain a `Widget` element named `start`, which will be rendered in the browser.

Currently, `Control`, `Widget`, `Proc`, and `FScape` are supported in the browser, 
but `Pattern` and `Stream` are not yet supported.
`FScape` objects can make use of  a virtual file system in the local
browser storage (IndexedDB), and also capture and play sound in real-time through 
the graph elements <code>PhysicalIn</code> and <code>PhysicalOut</code>.

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
   - [X] Ex/Control
   - [X] Widget
   - [X] FScape
   - [ ] Patterns
  
- [ ] export of workspaces 

   - [X] new database back-end that works in SJS, or JSON/__binary__ based export/import
       
- [ ] performance evaluation. Usage of web worker?

## notes

From Sébastien:

'Virtual File System' options:

- solved now in [AsyncFile](https://github.com/Sciss/AsyncFile)
- IndexedDB at [MDN](https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API), at [W3C](https://www.w3.org/TR/IndexedDB/)
- FileSystem API at [MDN](https://developer.mozilla.org/en-US/docs/Web/API/FileSystem), at [CanIUse](https://caniuse.com/?search=FileSystem%20API)
- [BrowserFS](https://jvilk.com/browserfs/2.0.0-beta/index.html)
- [scalajs-indexeddb](https://github.com/math85360/scalajs-indexeddb/blob/master/src/main/scala/com/iz2use/indexeddb/IndexedDB.scala)

For limitations of Scala.js, see also [this Gist](https://gist.github.com/Sciss/22996370ea2a277a409775705d740993)
