error id: 852FFCBFD6F308116290A6358EC650DD
file:///C:/Users/Lenin/Desktop/Practicum-1.1/Practicum1.1/src/main/scala/data/LecturaCSV.scala
### java.lang.AssertionError: assertion failed: tree: ArrowAssoc[A](null:
  (movieDecoder : fs2.data.csv.CsvRowDecoder[models.Movie, String])), pt: <notype>

occurred in the presentation compiler.



action parameters:
offset: 527
uri: file:///C:/Users/Lenin/Desktop/Practicum-1.1/Practicum1.1/src/main/scala/data/LecturaCSV.scala
text:
```scala
package data

import cats.effect.IO
import fs2.io.file.{Files, Path}
import fs2.data.csv.lenient.attemptDecodeUsingHeaders
import fs2.data.csv.generic.semiauto.*
import models.Movie
import utilities.CSVDecoder.*

object LecturaCSV {

  /**
   * Lee un archivo CSV y retorna un Stream de Movies
   * Maneja errores de forma segura
   */
  def readMoviesFromCsv(filePath: Path, separator: Char = ';'): IO[List[Either[String, Movie]]] = {
    import fs2.data.csv.CsvRowDecoder

    implicit val movieDecoder: Csv@@RowDecoder[Movie, String] = deriveCsvRowDecoder[Movie]

    Files[IO]
      .readAll(filePath)
      .through(fs2.text.utf8.decode)
      .through(attemptDecodeUsingHeaders[Movie](separator = separator))
      .map {
        case Right(movie) => Right(movie)
        case Left(error) => Left(error.getMessage)
      }
      .compile
      .toList
  }

  /**
   * Cuenta filas válidas por un campo específico (ej: ID)
   * Usa attemptDecodeUsingHeaders
   */
  def countValidRows(
                      filePath: Path,
                      fieldName: String,
                      validator: String => Boolean,
                      separator: Char = ';'
                    ): IO[Long] = {
    Files[IO]
      .readAll(filePath)
      .through(fs2.text.utf8.decode)
      .through(attemptDecodeUsingHeaders[Map[String, String]](separator = separator))
      .collect { case Right(row) => row }
      .map(row => row.getOrElse(fieldName, ""))
      .filter(validator)
      .compile
      .count
  }

  /**
   * Lee todas las filas y las convierte a Map[String, String]
   * Maneja filas corruptas
   */
  def readCsvAsMap(filePath: Path, separator: Char = ';'): IO[List[Map[String, String]]] = {
    Files[IO]
      .readAll(filePath)
      .through(fs2.text.utf8.decode)
      .through(attemptDecodeUsingHeaders[Map[String, String]](separator = separator))
      .collect { case Right(row) => row }
      .compile
      .toList
  }
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
scala.runtime.Scala3RunTime$.assertFailed(Scala3RunTime.scala:8)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:3598)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3590)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Implicits.tryConversion$1(Implicits.scala:1137)
	dotty.tools.dotc.typer.Implicits.typedImplicit(Implicits.scala:1168)
	dotty.tools.dotc.typer.Implicits.typedImplicit$(Implicits.scala:819)
	dotty.tools.dotc.typer.Typer.typedImplicit(Typer.scala:117)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.tryImplicit(Implicits.scala:1243)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.rank$1(Implicits.scala:1342)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1512)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1540)
	dotty.tools.dotc.typer.Implicits$ImplicitSearch.bestImplicit(Implicits.scala:1573)
	dotty.tools.dotc.typer.Implicits.inferImplicit(Implicits.scala:1061)
	dotty.tools.dotc.typer.Implicits.inferImplicit$(Implicits.scala:819)
	dotty.tools.dotc.typer.Typer.inferImplicit(Typer.scala:117)
	dotty.tools.dotc.typer.Implicits.inferView(Implicits.scala:857)
	dotty.tools.dotc.typer.Implicits.inferView$(Implicits.scala:819)
	dotty.tools.dotc.typer.Typer.inferView(Typer.scala:117)
	dotty.tools.dotc.typer.Implicits.viewExists(Implicits.scala:832)
	dotty.tools.dotc.typer.Implicits.viewExists$(Implicits.scala:819)
	dotty.tools.dotc.typer.Typer.viewExists(Typer.scala:117)
	dotty.tools.dotc.typer.Implicits.ignoredConvertibleImplicits$1$$anonfun$3(Implicits.scala:961)
	scala.collection.Iterator$$anon$6.hasNext(Iterator.scala:479)
	scala.collection.Iterator.isEmpty(Iterator.scala:466)
	scala.collection.Iterator.isEmpty$(Iterator.scala:466)
	scala.collection.AbstractIterator.isEmpty(Iterator.scala:1300)
	scala.collection.View$Filter.isEmpty(View.scala:146)
	scala.collection.IterableOnceOps.nonEmpty(IterableOnce.scala:853)
	scala.collection.IterableOnceOps.nonEmpty$(IterableOnce.scala:853)
	scala.collection.AbstractIterable.nonEmpty(Iterable.scala:933)
	dotty.tools.dotc.reporting.MissingImplicitArgument.noChainConversionsNote$1(messages.scala:2929)
	dotty.tools.dotc.reporting.MissingImplicitArgument.msgPostscript$$anonfun$4(messages.scala:2944)
	scala.Option.orElse(Option.scala:477)
	dotty.tools.dotc.reporting.MissingImplicitArgument.msgPostscript(messages.scala:2944)
	dotty.tools.dotc.reporting.Message.message$$anonfun$1(Message.scala:344)
	dotty.tools.dotc.reporting.Message.inMessageContext(Message.scala:340)
	dotty.tools.dotc.reporting.Message.message(Message.scala:344)
	dotty.tools.dotc.reporting.Message.isNonSensical(Message.scala:321)
	dotty.tools.dotc.reporting.HideNonSensicalMessages.isHidden(HideNonSensicalMessages.scala:16)
	dotty.tools.dotc.reporting.HideNonSensicalMessages.isHidden$(HideNonSensicalMessages.scala:10)
	dotty.tools.dotc.interactive.InteractiveDriver$$anon$5.isHidden(InteractiveDriver.scala:156)
	dotty.tools.dotc.reporting.Reporter.issueUnconfigured(Reporter.scala:156)
	dotty.tools.dotc.reporting.Reporter.go$1(Reporter.scala:181)
	dotty.tools.dotc.reporting.Reporter.issueIfNotSuppressed(Reporter.scala:200)
	dotty.tools.dotc.reporting.Reporter.report(Reporter.scala:203)
	dotty.tools.dotc.reporting.StoreReporter.report(StoreReporter.scala:50)
	dotty.tools.dotc.report$.error(report.scala:68)
	dotty.tools.dotc.typer.Typer.issueErrors$1$$anonfun$1(Typer.scala:3811)
	scala.runtime.function.JProcedure3.apply(JProcedure3.java:15)
	scala.runtime.function.JProcedure3.apply(JProcedure3.java:10)
	scala.collection.LazyZip3.foreach(LazyZipOps.scala:248)
	dotty.tools.dotc.typer.Typer.issueErrors$1(Typer.scala:3813)
	dotty.tools.dotc.typer.Typer.addImplicitArgs$1(Typer.scala:3835)
	dotty.tools.dotc.typer.Typer.adaptNoArgsImplicitMethod$1(Typer.scala:3852)
	dotty.tools.dotc.typer.Typer.adaptNoArgs$1(Typer.scala:4047)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4277)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3590)
	dotty.tools.dotc.typer.Typer.readapt$1(Typer.scala:3601)
	dotty.tools.dotc.typer.Typer.adapt1(Typer.scala:4264)
	dotty.tools.dotc.typer.Typer.adapt(Typer.scala:3590)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3303)
	dotty.tools.dotc.typer.Typer.typeSelectOnTerm$1(Typer.scala:755)
	dotty.tools.dotc.typer.Typer.typedSelect(Typer.scala:793)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3019)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3114)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3303)
	dotty.tools.dotc.typer.Typer.typedBlock(Typer.scala:1168)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3058)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3115)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3303)
	dotty.tools.dotc.typer.Typer.$anonfun$57(Typer.scala:2486)
	dotty.tools.dotc.inlines.PrepareInlineable$.dropInlineIfError(PrepareInlineable.scala:243)
	dotty.tools.dotc.typer.Typer.typedDefDef(Typer.scala:2486)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3026)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3114)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.traverse$1(Typer.scala:3213)
	dotty.tools.dotc.typer.Typer.typedStats(Typer.scala:3259)
	dotty.tools.dotc.typer.Typer.typedClassDef(Typer.scala:2669)
	dotty.tools.dotc.typer.Typer.typedTypeOrClassDef$1(Typer.scala:3038)
	dotty.tools.dotc.typer.Typer.typedNamed$1(Typer.scala:3042)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3114)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.traverse$1(Typer.scala:3213)
	dotty.tools.dotc.typer.Typer.typedStats(Typer.scala:3259)
	dotty.tools.dotc.typer.Typer.typedPackageDef(Typer.scala:2812)
	dotty.tools.dotc.typer.Typer.typedUnnamed$1(Typer.scala:3083)
	dotty.tools.dotc.typer.Typer.typedUnadapted(Typer.scala:3115)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3187)
	dotty.tools.dotc.typer.Typer.typed(Typer.scala:3191)
	dotty.tools.dotc.typer.Typer.typedExpr(Typer.scala:3303)
	dotty.tools.dotc.typer.TyperPhase.typeCheck$$anonfun$1(TyperPhase.scala:44)
	dotty.tools.dotc.typer.TyperPhase.typeCheck$$anonfun$adapted$1(TyperPhase.scala:50)
	scala.Function0.apply$mcV$sp(Function0.scala:42)
	dotty.tools.dotc.core.Phases$Phase.monitor(Phases.scala:440)
	dotty.tools.dotc.typer.TyperPhase.typeCheck(TyperPhase.scala:50)
	dotty.tools.dotc.typer.TyperPhase.runOn$$anonfun$3(TyperPhase.scala:84)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.immutable.List.foreach(List.scala:333)
	dotty.tools.dotc.typer.TyperPhase.runOn(TyperPhase.scala:84)
	dotty.tools.dotc.Run.runPhases$1$$anonfun$1(Run.scala:246)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:15)
	scala.runtime.function.JProcedure1.apply(JProcedure1.java:10)
	scala.collection.ArrayOps$.foreach$extension(ArrayOps.scala:1323)
	dotty.tools.dotc.Run.runPhases$1(Run.scala:262)
	dotty.tools.dotc.Run.compileUnits$$anonfun$1(Run.scala:270)
	dotty.tools.dotc.Run.compileUnits$$anonfun$adapted$1(Run.scala:279)
	dotty.tools.dotc.util.Stats$.maybeMonitored(Stats.scala:71)
	dotty.tools.dotc.Run.compileUnits(Run.scala:279)
	dotty.tools.dotc.Run.compileSources(Run.scala:194)
	dotty.tools.dotc.interactive.InteractiveDriver.run(InteractiveDriver.scala:165)
	scala.meta.internal.pc.MetalsDriver.run(MetalsDriver.scala:45)
	scala.meta.internal.pc.WithCompilationUnit.<init>(WithCompilationUnit.scala:28)
	scala.meta.internal.pc.WithSymbolSearchCollector.<init>(PcCollector.scala:362)
	scala.meta.internal.pc.PcDocumentHighlightProvider.<init>(PcDocumentHighlightProvider.scala:16)
	scala.meta.internal.pc.ScalaPresentationCompiler.documentHighlight$$anonfun$1(ScalaPresentationCompiler.scala:178)
```
#### Short summary: 

java.lang.AssertionError: assertion failed: tree: ArrowAssoc[A](null:
  (movieDecoder : fs2.data.csv.CsvRowDecoder[models.Movie, String])), pt: <notype>