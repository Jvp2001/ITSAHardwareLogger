package org.itsadigitaltrust.hardwarelogger.consumers

trait PreloadedDataConsumer[D]:
  def receiveData(data: D): Unit
  