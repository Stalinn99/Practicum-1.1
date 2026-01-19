package models

case class Cast(
                 cast_id: Int,
                 character: String,
                 credit_id: String,
                 gender: Int,
                 id: Int,
                 name: String,
                 order: Int,
                 profile_path: Option[String]
               )
