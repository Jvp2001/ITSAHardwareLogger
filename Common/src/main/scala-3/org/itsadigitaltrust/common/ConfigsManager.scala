package org.itsadigitaltrust.common

import ox.OxError
import ox.either.ok

import java.io.{File, IOException}
import java.nio.file.Files
import scala.Conversion.into
import scala.reflect.ClassTag
import scala.util.boundary.Label

opaque type ConfigFilePath = String

object ConfigFilePath:
  def apply(path: String): ConfigFilePath = path
  def toFile(path: ConfigFilePath): File = new File(path)

  given Conversion[ConfigFilePath, File] = ConfigFilePath.toFile
object ConfigsManager:
  type ConfigsError = Throwable

  type Result[R] = Either[ConfigsError, R]
  type OxContext[E <: ConfigsError] = ox.Ox //OxError[E, [X] =>> Either[E, X]]
  private type ContextLabel[T] = Label[Either[ConfigsError, T]]

  private final val jarPath: String = getClass.getProtectionDomain.getCodeSource.getLocation.getFile

  private final val parent = new File(jarPath).getParentFile

  final val configsFolder = new File(parent, "Configs")

  import java.io.{FileNotFoundException, InputStream}
  import java.nio.charset.StandardCharsets
  import java.nio.file.{Files, Path, Paths, StandardCopyOption}
  import scala.util.{Failure, Success, Try, Using}


    private val externalDirPath: Path =
      val codeSourceLocation = getClass.getProtectionDomain.getCodeSource.getLocation.toURI
      new File(Paths.get(codeSourceLocation).getParent.toString, "Configs").toPath

    /**
     * Copies a configuration file from the project resources to the external folder.
     *
     * @param resourcePath Path to resource relative to the resources root (no leading slash)
     * @param fileName     Target filename in the external folder
     * @param overwrite    Replace the exsisting file.
     */
    def copyFromResources(resourcePath: String, fileName: String, overwrite: Boolean = false): Try[Path] =
      val inputStream: InputStream = getClass.getClassLoader.getResourceAsStream(resourcePath)

      if inputStream == null then
        Failure(new FileNotFoundException(s"Resource file not found: $resourcePath"))
      else
        Try:
          ensureExternalDirectoryExists()
          val targetPath = externalDirPath.resolve(fileName)

          Using.resource(inputStream): is =>
            if overwrite then
              Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING)
            else
              Files.copy(is, targetPath)
          targetPath

    /**
     * Reads the content of a configuration file located in the external folder.
     */
    def readConfig(fileName: String): Try[String] =
      Try:
        val targetPath = externalDirPath.resolve(fileName)
        if !Files.exists(targetPath) then
          throw new FileNotFoundException(s"Config file does not exist at: $targetPath")

        val bytes = Files.readAllBytes(targetPath)
        new String(bytes, StandardCharsets.UTF_8)

    /**
     * Writes or updates text content into a file within the external folder.
     */
    def writeConfig(fileName: String, content: String): Try[Path] =
      Try:
        ensureExternalDirectoryExists()
        val targetPath = externalDirPath.resolve(fileName)

        Files.write(targetPath, content.getBytes(StandardCharsets.UTF_8))
        targetPath

    private def ensureExternalDirectoryExists(): Unit =
      if !Files.exists(externalDirPath) then
        Files.createDirectories(externalDirPath)


end ConfigsManager

extension (sc: StringContext)
  def config(args: Any*): ConfigFilePath =
    val path = sc.s(args*)
    ConfigFilePath(s"${ConfigsManager.configsFolder}/$path")