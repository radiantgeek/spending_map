package lib.html

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.iteratee.{Iteratee, Enumeratee, Enumerator}
import play.api.mvc.{Codec, SimpleResult}
import play.api.http.{ContentTypeOf, Writeable}
import play.api.templates.{HtmlFormat, Html}
import play.templates.{Format, Appendable}
import play.mvc.Results.Chunks
import play.mvc.Results.Chunks.Out

/**
 * A custom Appendable that lets us have .scala.stream templates instead of .scala.html. These templates can mix Html
 * markup with Enumerators that contain Html markup. We add this class as a custom template type in build.sbt.
 *
 * @param enumerator
 */
case class HtmlStream(enumerator: Enumerator[Html]) extends Appendable[HtmlStream] {
  def +=(other: HtmlStream): HtmlStream = andThen(other)

  def andThen(other: HtmlStream): HtmlStream = HtmlStream(enumerator.andThen(other.enumerator))
}

/**
 * Companion object for HtmlStream that contains convenient factory and composition methods.
 */
object HtmlStream {

  implicit def convert(text: String): Html = Html(text)

  // Implicit conversion so HtmlStream can be passed directly to Ok.feed and Ok.chunked
  implicit def toEnumerator(stream: HtmlStream): Enumerator[Html] = {
    // Skip empty chunks, as these mean EOF in chunked encoding
    stream.enumerator.through(Enumeratee.filter(!_.body.isEmpty))
  }

  /**
   * Create an HtmlStream from Html
   *
   * @param html
   * @return
   */
  def apply(html: Html): HtmlStream = {
    HtmlStream(Enumerator(html))
  }

  /**
   * Create an HtmlStream from a Future that will eventually contain Html
   *
   * @param eventuallyHtml
   * @return
   */
  def apply[T](eventuallyHtml: Future[T])(implicit converter:T => Html): HtmlStream = {
    flatten(eventuallyHtml.map{ f => apply(converter(f))})
  }


  /**
   * Create an HtmlStream from the body of the SimpleResult.
   *
   * @param result
   * @return
   */
  def fromResult(result: SimpleResult): HtmlStream = {
    HtmlStream(result.body.map(bytes => Html(new String(bytes, "UTF-8"))))
  }

  /**
   * Create an HtmlStream from a the body of a Future[SimpleResult].
   *
   * @param result
   * @return
   */
  def fromResult(result: Future[SimpleResult]): HtmlStream = {
    flatten(result.map(fromResult))
  }

  /**
   * Interleave multiple HtmlStreams together. Interleaving is done based on whichever HtmlStream next has input ready,
   * if multiple have input ready, the order is undefined.
   *
   * @param streams
   * @return
   */
  def interleave(streams: HtmlStream*): HtmlStream = {
    HtmlStream(Enumerator.interleave(streams.map(_.enumerator)))
  }

  /**
   * Create an HtmlStream from a Future that will eventually contain an HtmlStream.
   *
   * @param eventuallyStream
   * @return
   */
  def flatten(eventuallyStream: Future[HtmlStream]): HtmlStream = {
    HtmlStream(Enumerator.flatten(eventuallyStream.map(_.enumerator)))
  }

  /**
   * Java API. Provides a convenience method for interleaving streams from a Java controller that doesn't rely on
   * scala's Seq
   *
   * @param streams
   * @return
   */
  def interleave(streams: java.util.List[HtmlStream]): HtmlStream = {
    import collection.JavaConverters._
    HtmlStream(Enumerator.interleave(streams.asScala.map(_.enumerator)))
  }
  def interleave(streams: List[HtmlStream]): HtmlStream = {
    HtmlStream(Enumerator.interleave(streams.map(_.enumerator)))
  }

  /**
   * Java API. Creates a Chunks object that can be returned from a Java controller to stream out the HtmlStream.
   *
   * @param stream
   * @return
   */
  def toChunks(stream: HtmlStream): Chunks[Html] = {
    val utf8 = Codec.javaSupported("utf-8")

    new Chunks[Html](Writeable.writeableOf_Content(utf8, ContentTypeOf.contentTypeOf_Html(utf8))) {
      def onReady(out: Out[Html]) {
        stream.enumerator.run(Iteratee.foreach { html =>
          if (!html.toString.isEmpty) {
            out.write(html)
          }
        }).onComplete(_ => out.close())
      }
    }
  }
}

/**
 * A custom Format that lets us have .scala.stream templates instead of .scala.html. These templates can mix Html
 * markup with Enumerators that contain Html markup.
 */
object HtmlStreamFormat extends Format[HtmlStream] {

  def raw(text: String): HtmlStream = {
    HtmlStream(Html(text))
  }

  def escape(text: String): HtmlStream = {
    raw(HtmlFormat.escape(text).body)
  }
}

