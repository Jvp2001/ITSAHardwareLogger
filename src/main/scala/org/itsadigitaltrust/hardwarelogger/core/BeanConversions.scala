package org.itsadigitaltrust.hardwarelogger.core

import javafx.beans.property.*

object conversions:
  
  given Conversion[String, StringProperty] with
    override def apply(x: String): StringProperty = new SimpleStringProperty(x)
  
  
  given Conversion[Int, IntegerProperty] with
    override def apply(x: Int): IntegerProperty = new SimpleIntegerProperty(x)
  
  
  given Conversion[Long, LongProperty] with
    override def apply(x: Long): LongProperty = new SimpleLongProperty(x)
  
  given Conversion[Double, DoubleProperty] with
    override def apply(x: Double): DoubleProperty = SimpleDoubleProperty(x)
  
  given Conversion[Float, FloatProperty] with
    override def apply(x: Float): FloatProperty = SimpleFloatProperty(x)
  
  given Conversion[Boolean, BooleanProperty] with
    override def apply(x: Boolean): BooleanProperty = SimpleBooleanProperty(x)
    

