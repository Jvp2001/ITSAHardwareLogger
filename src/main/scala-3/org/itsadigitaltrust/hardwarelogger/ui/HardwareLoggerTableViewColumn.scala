package org.itsadigitaltrust.hardwarelogger.ui
import org.itsadigitaltrust.hardwarelogger.core.ui.*
object TableUtils:
  transparent inline def createTableColumnsFromTuple[S](inline tuple: Tuple)()(inline f: TableColumn[S, ?] => TableColumn[S, ?]) : Any =
    
    tuple match
      case EmptyTuple => ()
      case head *: tail =>


