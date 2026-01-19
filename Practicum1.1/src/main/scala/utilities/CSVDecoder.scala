package utilities

import fs2.data.csv.CellDecoder

object CSVDecoder {

  // Decodificador seguro para enteros
  implicit val trimIntDecoder: CellDecoder[Int] =
    CellDecoder.stringDecoder.map(_.trim).map { s =>
      s.toIntOption.getOrElse(0)
    }

  // Decodificador seguro para doubles
  implicit val trimDoubleDecoder: CellDecoder[Double] =
    CellDecoder.stringDecoder.map(_.trim).map { s =>
      s.toDoubleOption.getOrElse(0.0)
    }

  // Decodificador seguro para booleanos
  implicit val trimBoolDecoder: CellDecoder[Boolean] =
    CellDecoder.stringDecoder.map(_.trim.toLowerCase).map {
      case "true" | "1" | "t" | "true.0" => true
      case _ => false
    }

  // Decodificador para strings (trim automático)
  implicit val trimStringDecoder: CellDecoder[String] =
    CellDecoder.stringDecoder.map(_.trim)
}