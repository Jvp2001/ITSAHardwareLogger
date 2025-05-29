package org.itsadigitaltrust.common.collections

import org.apache.commons.text.similarity.FuzzyScore
import org.itsadigitaltrust.common.Operators.|>

import java.util.Locale

trait FuzzyMap[V]:
  private var map: Map[String, V] = Map()


  def update(key: String, value: V): Unit =
    put(key, value)
  def put(key: String, value: V): Unit =
    map = map.updated(key, value)

  def get(key: String): Option[V] =
    map.get(key)

  def get(key: Option[String]): Option[V] =
    key match
      case Some(value) => map.get(value)
      case None => None

  def getMatchingKeys(input: String, tolerance: Int = 2): Seq[String] =
    map.keys.filter(key => calculateFuzzyScore(key, input) >= input.length - tolerance).toSeq

  def getBestMatchingKey(input: String): Option[String] =
    getMatchingKeys(input, input.length).headOption

  def ++ (other: Map[String, V]): this.type =
    map = map ++ other
    this

  def getFirstValueForMatchingKey(input: String, tolerance: Int = 2): Option[V] =
    getMatchingKeys(input, tolerance).headOption |> get

  def getValueForBestMatchingKey(input: String): Option[V] =
    input |> getBestMatchingKey |> get // Same as get(getBestMatchingKey(input))
  /**
   *
   * @return The number of matching characters
   */
  def calculateFuzzyScore(term: String, query: String): Int

end FuzzyMap

final class ApacheFuzzyMap[V](locale: Locale = Locale.UK) extends FuzzyMap[V]:
  private val fuzzyScore = new FuzzyScore(locale)

  override def calculateFuzzyScore(term: String, query: String): Int =
    fuzzyScore.fuzzyScore(term, query)



