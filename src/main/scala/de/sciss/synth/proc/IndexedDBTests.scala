package de.sciss.synth.proc

import de.sciss.audiofile.{AudioFile, AudioFileSpec, IndexedDBFile}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.util.{Failure, Success}

@JSExportTopLevel("IndexedDBTests")
object IndexedDBTests {
  @JSExport
  def run(): Unit = {
    sys.props.put(AudioFile.KEY_DIRECT_MEMORY, true.toString)

    val N = 100000
    val BUF = new Array[Float](N)

    def writeFile(): Future[Double] = {
      val spec = AudioFileSpec(numChannels = 1, sampleRate = 44100)
      var rms  = 0.0
      for {
        ch <- IndexedDBFile.openWrite("test.aif")
        af <- { /*println("ch created");*/ AudioFile.openWriteAsync(ch, spec) }
        _  <- {
          // println("af created")
          val b   = af.buffer(N)
          val bL  = b(0)
          for (i <- 0 until N) {
            val v = math.sin(0.01 * i).toFloat
            rms += v*v
            bL(i) = v
          }
          af.write(b)
        }
        _ <- af.flush()
      } yield {
        af.close()
        math.sqrt(rms / N)
      }
    }

    def readFile(): Future[Double] = {
      val spec = AudioFileSpec(numChannels = 1, sampleRate = 44100)
      var rms  = 0.0
      for {
        ch <- IndexedDBFile.openRead("test.aif")
        af <- { /*println("ch created");*/ AudioFile.openReadAsync(ch) }
        _  <- {
          println(s"af opened. spec is ${af.spec}")
          val n   = af.numFrames.toInt
          val b   = af.buffer(n)
          af.read(b).map { _ =>
            val bL = b(0)
            for (i <- 0 until N) {
              val v = bL(i)
              rms += v*v
            }
          }
        }
      } yield {
        af.close()
        math.sqrt(rms / N)
      }
    }

    val t1w = System.currentTimeMillis()
    writeFile().onComplete {
      case Success(rmsWrite) =>
        val t2w = System.currentTimeMillis()
        println(f"Writing completed. RMS is $rmsWrite%1.3f Took ${(t2w - t1w).toDouble / 1000}%1.3f s.")

        val t1r = System.currentTimeMillis()
        readFile().onComplete {
          case Success(rmsRead) =>
            val t2r = System.currentTimeMillis()
            println(f"Reading completed. RMS is $rmsRead%1.3f. Took ${(t2r - t1r).toDouble / 1000}%1.3f s.")

          case Failure(ex) =>
            println("Failed:")
            ex.printStackTrace()
        }

      case Failure(ex) =>
        println("Failed:")
        ex.printStackTrace()
    }
  }
}
