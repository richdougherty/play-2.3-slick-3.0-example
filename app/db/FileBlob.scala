package db

import java.io._
import java.sql.Blob
import org.apache.commons.io.IOUtils
import org.apache.commons.io.input.BoundedInputStream
import play.api.libs.Files.TemporaryFile

/**
 * A Java Blob that wraps a File. If the user of the Blob calls the
 * `free` method then the Blob will be deleted.
 */
trait FileBlob extends Blob {
  def file: File
  override def free(): Unit = file.delete
  override def getBinaryStream(): InputStream = {
    new FileInputStream(file)
  }
  override def getBinaryStream(pos: Long, length: Long) = {
    val stream = new BoundedInputStream(getBinaryStream(), pos + length)
    stream.skip(pos)
    stream
  }
  override def getBytes(pos: Long, length: Int): Array[Byte] = {
    IOUtils.toByteArray(getBinaryStream(pos, length))
  }
  override def length: Long = {
    file.length
  }
  // Search method not implemented
  override def position(pattern: Blob, start: Long): Long = ???
  override def position(pattern: Array[Byte], start: Long): Long = ???
  // Write methods not implemented
  override def setBinaryStream(pos: Long): OutputStream = ???
  override def setBytes(pos: Long, bytes: Array[Byte]): Int = ???
  override def setBytes(pos: Long, bytes: Array[Byte], offset: Int, len: Int): Int = ???
  override def truncate(len: Long): Unit = ???
}

/**
 * We want the Blob to hold a reference to the TemporaryFile, because
 * if the TemporaryFile is garbage colleced then the TemporaryFile's
 * `finalize` method will be called, which will delete the File.
 */
class TemporaryFileBlob(tempFile: TemporaryFile) extends FileBlob {
  override def file = tempFile.file
}