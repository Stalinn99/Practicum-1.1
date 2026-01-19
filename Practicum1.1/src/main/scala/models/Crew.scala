package models

case class Crew(
                 credit_id: String,
                 department: String,
                 gender: Int,
                 id: Int,
                 job: String,
                 name: String,
                 profile_path: Option[String]
               )