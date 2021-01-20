package de.sciss.proc

import de.sciss.lucre.edit.UndoManager
import de.sciss.lucre.expr
import de.sciss.lucre.store.InMemoryDB
import de.sciss.lucre.swing.View
import de.sciss.proc.Implicits.{FolderOps, ObjOps}
import de.sciss.proc.Test.{S, T}
import de.sciss.serial.DataInput
import org.scalajs.dom.raw.XMLHttpRequest

import scala.concurrent.{Future, Promise}
import scala.scalajs.js.typedarray.{ArrayBuffer, Int8Array}
import scala.util.control.NonFatal

object LoadWorkspace {
  def apply(): Future[(Universe[T], View[T])] = {
    val oReq  = new XMLHttpRequest
    val url   = "example-workspace.mllt.bin"
    oReq.open(method = "GET", url = url, async = true)
    oReq.responseType = "arraybuffer"
    val res = Promise[(Universe[T], View[T])]()

    oReq.onload = { _ =>
      oReq.response match {
        case ab: ArrayBuffer =>
          try {
//            val bytes = new Uint8Array(ab)
            val bytes = new Int8Array(ab)
            // N.B. currently no way in SJS to get `Array[Byte]` without copying.
            // a slightly better way might be to wrap in `ByteBuffer`
            val BIN_COOKIE = 0x6d6c6c742d777300L  // "mllt-ws\u0000"
            val dIn = DataInput(bytes.toArray) // [Byte])
            val cookie = dIn.readLong()
            if (cookie != BIN_COOKIE) sys.error(s"Unexpected cookie 0x${cookie.toHexString} is not ${BIN_COOKIE.toHexString}")
            val mVer = dIn.readUTF()
            println(s"Workspace was exported by Mellite $mVer")
            val blob = new Array[Byte](dIn.size - dIn.position)
            System.arraycopy(dIn.buffer, dIn.position, blob, 0, blob.length)
            val db = InMemoryDB.fromByteArray(blob)
            // println(db.numEntries)
            implicit val cursor: Durable = Durable(db)
            val ws = Workspace.Ephemeral[T, S](cursor)
            implicit val undo: UndoManager[T] = UndoManager()
            val resOpt = cursor.step { implicit tx =>
              val fRoot = ws.root
              fRoot.iterator.foreach { child =>
                println(s"CHILD: ${child.name}")
              }
              fRoot.$[Widget]("start").map { w =>
                implicit val u: Universe[T] = Universe.dummy[T]
                val wH = tx.newHandle(w)
                implicit val ctx: expr.Context[T] = ExprContext(selfH = Some(wH))
                val gW    = w.graph().value
                val _view = gW.expand[T]
                _view.initControl()
                (u, _view)
              }
            }
            val resVal = resOpt.getOrElse(sys.error("No start element found"))
            res.success(resVal)

          } catch {
            case NonFatal(ex) => res.failure(ex)
          }

        case other =>
          res.failure(new Exception(s"Expected an ArrayBuffer but got $other"))
      }
    }
    oReq.onerror = { _ =>
      res.failure(new Exception(s"XMLHttpRequest failed for '$url'"))
    }

    oReq.send(null)

    res.future
  }
}
