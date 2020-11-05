package de.sciss.synth.proc

import com.raquo.laminar.api.L.{documentEvents, unsafeWindowOwner}
import de.sciss.asyncfile.IndexedDBFile
import de.sciss.audiofile.{AudioFile, AudioFileSpec, SampleFormat}

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.{Future, Promise}
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.util.{Failure, Success}

@JSExportTopLevel("IndexedDBTests")
object IndexedDBTests {
  @JSExport
  def run(): Unit = {
    sys.props.put(AudioFile.KEY_DIRECT_MEMORY, true.toString)

    val prTestData = Promise[Vector[Double]]()
    launchGUI(prTestData.future)

    val N = 8192 * 2 + 1000

    def writeFile(): Future[Double] = {
      val spec = AudioFileSpec(numChannels = 1, sampleRate = 44100, sampleFormat = SampleFormat.Int8)
      var rms  = 0.0
      for {
        ch <- IndexedDBFile.openWrite("test.aif")
        af <- AudioFile.openWriteAsync(ch, spec)
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
      var rms  = 0.0
      for {
        ch <- IndexedDBFile.openRead("test.aif")
        af <- AudioFile.openReadAsync(ch)
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
            prTestData.success(bL.iterator.map(_.toDouble).toVector)
            ()
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

  def launchGUI(futData: Future[Vector[Double]]): Unit = {
    documentEvents.onDomContentLoaded.foreach { _ =>
      futData.foreach { data =>
        runGUI(data)
      }
    } (unsafeWindowOwner)
  }

  def runGUI(data: Vector[Double]): Unit = {
    import plotly.Plotly._
    import plotly.Scatter
    import plotly.layout.Layout

    val x = (0 to data.length).flatMap(v => v :: v :: Nil).drop(1).dropRight(1)
    val y = data.flatMap(v => v :: v :: Nil)

    val plot = Seq(
      Scatter(x, y).withName("Read"),
    )

    val lay = Layout().withTitle("Read").withWidth(600)
    plot.plot("plot", lay)
  }
}
