error id: E1DFD188DE2A4206249788C2C64C5DF0
jar:file:///C:/Users/Lenin/AppData/Local/Coursier/cache/v1/https/repo1.maven.org/maven2/org/tpolecat/doobie-core_3/1.0.0-RC4/doobie-core_3-1.0.0-RC4-sources.jar!/doobie/free/Aliases.scala
### dotty.tools.dotc.ast.Trees$UnAssignedTypeException: type of Ident(cats) is not assigned

occurred in the presentation compiler.



action parameters:
uri: jar:file:///C:/Users/Lenin/AppData/Local/Coursier/cache/v1/https/repo1.maven.org/maven2/org/tpolecat/doobie-core_3/1.0.0-RC4/doobie-core_3-1.0.0-RC4-sources.jar!/doobie/free/Aliases.scala
text:
```scala
// Copyright (c) 2013-2020 Rob Norris and Contributors
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package doobie.free

import cats.{Monoid, Semigroup}
import doobie.WeakAsync

trait Types {
  /** @group Type Aliases - Free API */ type BlobIO[A]              = blob.BlobIO[A]
  /** @group Type Aliases - Free API */ type CallableStatementIO[A] = callablestatement.CallableStatementIO[A]
  /** @group Type Aliases - Free API */ type ClobIO[A]              = clob.ClobIO[A]
  /** @group Type Aliases - Free API */ type ConnectionIO[A]        = connection.ConnectionIO[A]
  /** @group Type Aliases - Free API */ type DatabaseMetaDataIO[A]  = databasemetadata.DatabaseMetaDataIO[A]
  /** @group Type Aliases - Free API */ type DriverIO[A]            = driver.DriverIO[A]
  /** @group Type Aliases - Free API */ type NClobIO[A]             = nclob.NClobIO[A]
  /** @group Type Aliases - Free API */ type PreparedStatementIO[A] = preparedstatement.PreparedStatementIO[A]
  /** @group Type Aliases - Free API */ type RefIO[A]               = ref.RefIO[A]
  /** @group Type Aliases - Free API */ type ResultSetIO[A]         = resultset.ResultSetIO[A]
  /** @group Type Aliases - Free API */ type SQLDataIO[A]           = sqldata.SQLDataIO[A]
  /** @group Type Aliases - Free API */ type SQLInputIO[A]          = sqlinput.SQLInputIO[A]
  /** @group Type Aliases - Free API */ type SQLOutputIO[A]         = sqloutput.SQLOutputIO[A]
  /** @group Type Aliases - Free API */ type StatementIO[A]         = statement.StatementIO[A]
}

trait Modules {
  /** @group Module Aliases - Free API */ lazy val FB   = blob
  /** @group Module Aliases - Free API */ lazy val FCS  = callablestatement
  /** @group Module Aliases - Free API */ lazy val FCL  = clob
  /** @group Module Aliases - Free API */ lazy val FC   = connection
  /** @group Module Aliases - Free API */ lazy val FDMD = databasemetadata
  /** @group Module Aliases - Free API */ lazy val FD   = driver
  /** @group Module Aliases - Free API */ lazy val FNCL = nclob
  /** @group Module Aliases - Free API */ lazy val FPS  = preparedstatement
  /** @group Module Aliases - Free API */ lazy val FREF = ref
  /** @group Module Aliases - Free API */ lazy val FRS  = resultset
  /** @group Module Aliases - Free API */ lazy val FSD  = sqldata
  /** @group Module Aliases - Free API */ lazy val FSI  = sqlinput
  /** @group Module Aliases - Free API */ lazy val FSO  = sqloutput
  /** @group Module Aliases - Free API */ lazy val FS   = statement
}

trait Instances  {

  /** @group Typeclass Instances */  implicit lazy val WeakAsyncBlobIO: WeakAsync[BlobIO] =
    blob.WeakAsyncBlobIO

  /** @group Typeclass Instances */  implicit lazy val WeakAsyncCallableStatementIO: WeakAsync[CallableStatementIO] =
    callablestatement.WeakAsyncCallableStatementIO

  /** @group Typeclass Instances */  implicit lazy val WeakAsyncClobIO: WeakAsync[ClobIO] =
    clob.WeakAsyncClobIO

  /** @group Typeclass Instances */  implicit lazy val WeakAsyncConnectionIO: WeakAsync[ConnectionIO] =
    connection.WeakAsyncConnectionIO

  /** @group Typeclass Instances */  implicit def MonoidConnectionIO[A: Monoid]: Monoid[ConnectionIO[A]] =
    connection.MonoidConnectionIO[A]

  /** @group Typeclass Instances */  implicit def SemigroupConnectionIO[A: Semigroup]: Semigroup[ConnectionIO[A]] =
    connection.SemigroupConnectionIO[A]

  /** @group Typeclass Instances */  implicit lazy val WeakAsyncDatabaseMetaDataIO: WeakAsync[DatabaseMetaDataIO] =
    databasemetadata.WeakAsyncDatabaseMetaDataIO

  /** @group Typeclass Instances */  implicit lazy val WeakAsyncDriverIO: WeakAsync[DriverIO] =
    driver.WeakAsyncDriverIO

  /** @group Typeclass Instances */  implicit lazy val WeakAsyncNClobIO: WeakAsync[NClobIO] =
    nclob.WeakAsyncNClobIO

  /** @group Typeclass Instances */  implicit lazy val WeakAsyncPreparedStatementIO: WeakAsync[PreparedStatementIO] =
    preparedstatement.WeakAsyncPreparedStatementIO

  /** @group Typeclass Instances */  implicit lazy val WeakAsyncRefIO: WeakAsync[RefIO] =
    ref.WeakAsyncRefIO

  /** @group Typeclass Instances */  implicit lazy val WeakAsyncResultSetIO: WeakAsync[ResultSetIO] =
    resultset.WeakAsyncResultSetIO

  /** @group Typeclass Instances */  implicit lazy val WeakAsyncSQLDataIO: WeakAsync[SQLDataIO] =
    sqldata.WeakAsyncSQLDataIO

  /** @group Typeclass Instances */  implicit lazy val WeakAsyncSQLInputIO: WeakAsync[SQLInputIO] =
    sqlinput.WeakAsyncSQLInputIO

  /** @group Typeclass Instances */  implicit lazy val WeakAsyncSQLOutputIO: WeakAsync[SQLOutputIO] =
    sqloutput.WeakAsyncSQLOutputIO

  /** @group Typeclass Instances */  implicit lazy val WeakAsyncStatementIO: WeakAsync[StatementIO] =
    statement.WeakAsyncStatementIO

}

```


presentation compiler configuration:
Scala version: 3.3.3
Classpath:
<WORKSPACE>\.bloop\root\bloop-bsp-clients-classes\classes-Metals-Ju_LUYBETpiwzerk9kNOVQ== [exists ], <HOME>\AppData\Local\bloop\cache\semanticdb\com.sourcegraph.semanticdb-javac.0.11.2\semanticdb-javac-0.11.2.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\scala-lang\scala3-library_3\3.3.3\scala3-library_3-3.3.3.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\cats-effect_3\3.5.3\cats-effect_3-3.5.3.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\co\fs2\fs2-core_3\3.9.3\fs2-core_3-3.9.3.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\co\fs2\fs2-io_3\3.9.3\fs2-io_3-3.9.3.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\gnieh\fs2-data-csv_3\1.9.1\fs2-data-csv_3-1.9.1.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\gnieh\fs2-data-csv-generic_3\1.9.1\fs2-data-csv-generic_3-1.9.1.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\tpolecat\doobie-core_3\1.0.0-RC4\doobie-core_3-1.0.0-RC4.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\tpolecat\doobie-hikari_3\1.0.0-RC4\doobie-hikari_3-1.0.0-RC4.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\io\circe\circe-core_3\0.14.6\circe-core_3-0.14.6.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\io\circe\circe-generic_3\0.14.6\circe-generic_3-0.14.6.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\io\circe\circe-parser_3\0.14.6\circe-parser_3-0.14.6.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\scala-lang\scala-library\2.13.12\scala-library-2.13.12.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\cats-effect-kernel_3\3.5.3\cats-effect-kernel_3-3.5.3.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\cats-effect-std_3\3.5.3\cats-effect-std_3-3.5.3.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\scodec\scodec-bits_3\1.1.38\scodec-bits_3-1.1.38.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\cats-core_3\2.10.0\cats-core_3-2.10.0.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\com\comcast\ip4s-core_3\3.4.0\ip4s-core_3-3.4.0.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\gnieh\fs2-data-text_3\1.9.1\fs2-data-text_3-1.9.1.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\scala-lang\modules\scala-collection-compat_3\2.11.0\scala-collection-compat_3-2.11.0.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\portable-scala\portable-scala-reflect_2.13\1.1.2\portable-scala-reflect_2.13-1.1.2.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\shapeless3-deriving_3\3.3.0\shapeless3-deriving_3-3.3.0.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\tpolecat\doobie-free_3\1.0.0-RC4\doobie-free_3-1.0.0-RC4.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\tpolecat\typename_3\1.1.0\typename_3-1.1.0.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\com\zaxxer\HikariCP\5.0.1\HikariCP-5.0.1.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\io\circe\circe-numbers_3\0.14.6\circe-numbers_3-0.14.6.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\io\circe\circe-jawn_3\0.14.6\circe-jawn_3-0.14.6.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\cats-kernel_3\2.10.0\cats-kernel_3-2.10.0.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\literally_3\1.1.0\literally_3-1.1.0.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\cats-free_3\2.9.0\cats-free_3-2.9.0.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\com\google\protobuf\protobuf-java\3.21.9\protobuf-java-3.21.9.jar [exists ], <HOME>\AppData\Local\Coursier\cache\v1\https\repo1.maven.org\maven2\org\typelevel\jawn-parser_3\1.4.0\jawn-parser_3-1.4.0.jar [exists ]
Options:
-Xsemanticdb -sourceroot <WORKSPACE> -release 21




#### Error stacktrace:

```
dotty.tools.dotc.ast.Trees$Tree.tpe(Trees.scala:72)
	dotty.tools.dotc.semanticdb.ExtractSemanticDB$Extractor.traverse$$anonfun$15(ExtractSemanticDB.scala:254)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:333)
	dotty.tools.dotc.semanticdb.ExtractSemanticDB$Extractor.traverse(ExtractSemanticDB.scala:261)
	dotty.tools.dotc.semanticdb.ExtractSemanticDB$Extractor.traverse$$anonfun$1(ExtractSemanticDB.scala:145)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:333)
	dotty.tools.dotc.semanticdb.ExtractSemanticDB$Extractor.traverse(ExtractSemanticDB.scala:145)
	scala.meta.internal.pc.SemanticdbTextDocumentProvider.textDocument(SemanticdbTextDocumentProvider.scala:39)
	scala.meta.internal.pc.ScalaPresentationCompiler.semanticdbTextDocument$$anonfun$1(ScalaPresentationCompiler.scala:227)
```
#### Short summary: 

dotty.tools.dotc.ast.Trees$UnAssignedTypeException: type of Ident(cats) is not assigned