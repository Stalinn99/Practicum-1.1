package models

case class BelongToCollection(
                               id: Int,
                               name: String,
                               poster_path: Option[String],
                               backdrop_path: Option[String]
                             )