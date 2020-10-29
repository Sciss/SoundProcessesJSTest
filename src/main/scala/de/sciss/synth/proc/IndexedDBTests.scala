package de.sciss.synth.proc

import java.io.OutputStream
import java.nio.ByteBuffer

import org.scalajs.dom
import org.scalajs.dom.{Blob, BlobPropertyBag}
import org.scalajs.dom.raw.{IDBDatabase, IDBObjectStore, IDBOpenDBRequest, IDBRequest, IDBTransaction}

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array

object IndexedDBTests {
  final val BLOCK_SIZE = 8192

  class IDBOutputStream(db: IDBDatabase, storeName: String) {
    private[this] var blobOffset  = 0
    private[this] var data        = new Uint8Array(BLOCK_SIZE)
    private[this] val blobParts   = js.Array[js.Any](data)
    private[this] val blobProps   = BlobPropertyBag("audio/x-aiff")
    private[this] val storeNames  = js.Array(storeName)
    private[this] var blobIndex   = 0

    def write(bb: ByteBuffer): Unit = {
      val oldOffset = blobOffset
      data(oldOffset) = b.toByte
      val newOffset = oldOffset + 1

      if (newOffset == BLOCK_SIZE) {
        val blob  = new Blob(blobParts, blobProps)
        val tx    = db.transaction(storeNames, mode = IDBTransaction.READ_WRITE)
        val store = tx.objectStore(storeName)
        val req   = store.put(blob, key = blobIndex)
        blobIndex += 1

        data          = new Uint8Array(BLOCK_SIZE)
        blobParts(0)  = data
        blobOffset    = 0

      } else {
        blobOffset = newOffset
      }
    }

    def flush(): Unit = {
      if (blobOffset > 0) {

      }
    }

    def close(): Unit = {
      flush()
    }
  }

  def run(): Unit = {
    val idb = dom.window.indexedDB
    val dbVersion = 1
    val req: IDBOpenDBRequest = idb.open("filesystem", dbVersion)
    req.onsuccess = { _ =>
      println("Success creating/accessing IndexedDB database")
      val db = req.result.asInstanceOf[IDBDatabase]
      println("db: " + db)
    }
    req.onupgradeneeded = { e =>
      val db = req.result.asInstanceOf[IDBDatabase]
      println(s"Upgrading to version ${db.version}")
      val store: IDBObjectStore = db.createObjectStore("test")
    }
    req.onerror = { e =>
      println("IndexedDB database could not be created:")
      println(req.error)
    }
  }
}
