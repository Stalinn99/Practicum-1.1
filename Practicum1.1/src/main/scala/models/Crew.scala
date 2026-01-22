package models

case class Crew(
                 id: Int,
                 name: String,
                 gender: Option[Int],
                 profile_path: Option[String],
                 department: String,
                 job: String,
                 credit_id: Option[String]
               )