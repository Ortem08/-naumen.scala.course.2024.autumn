import cats._
import cats.implicits._

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

/*
  Задание №3
  Всё просто, нужно посчитать количество строк.
  Реализуйте функцию countWords, которая принимает список строк.
  Обязательно использовать функцию mapReduce.
 */
object Task3 extends App {
  def mapReduce[A, B: Monoid](values: Vector[A])(func: A => B): Future[B] = {
    val numCores = Runtime.getRuntime.availableProcessors
    val groupSize = (1.0 * values.size / numCores).ceil.toInt
    values
      .grouped(groupSize)
      .toVector
      .traverse(group => Future(group.foldMap(func)))
      .map(_.combineAll)
  }

  case class Count(word: String, count: Int)
  case class WordsCount(count: Seq[Count])
  object WordsCount {
    implicit val monoid: Monoid[WordsCount] = new Monoid[WordsCount] {

      override def empty: WordsCount = new WordsCount(Seq.empty)

      override def combine(x: WordsCount, y: WordsCount): WordsCount = new WordsCount(
        (x.count ++ y.count)
          .groupBy(w => w.word)
          .mapValues(group => group.map(_.count).sum)
          .map(p => Count(p._1, p._2))
          .toSeq
      )
    }
  }

  def countWords(lines: Vector[String]): WordsCount = Await.result(mapReduce(lines){
    line => new WordsCount(line.split("\\s+").map(word => Count(word, 1)))
  }, 3.seconds)
}
