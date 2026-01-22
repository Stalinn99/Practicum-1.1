package models

case class Cast(
                 cast_id: Int,
                 name: String,
                 gender: Option[Int],
                 profile_path: Option[String],
                 character: Option[String],
                 order: Option[Int],
                 credit_id: Option[String]
               )
