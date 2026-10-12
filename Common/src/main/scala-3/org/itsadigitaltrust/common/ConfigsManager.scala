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

  given Conversion[ConfigFilePath, String] = _.toString
  given Conversion[String, ConfigFilePath] = _.toString
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
      Paths.get(Paths.get(codeSourceLocation).getParent.toString, "Configs").toAbsolutePath


    inline def resolvePathString(path:ConfigFilePath): String =
      externalDirPath.resolve(path.toString).toAbsolutePath.toString
    /**
     * Copies a configuration file from the project resources to the external folder.
     *
     * @param resourcePath Classpath resource name or URL returned by an asset string context
     * @param fileName     Target filename in the external folder
     * @param overwrite    Replace the exsisting file.
     */
    def copyFromResources(resourcePath: into[ConfigFilePath], fileName: into[ConfigFilePath], overwrite: Boolean = false): Try[Path] =
      Try:
        Using.resource(openResource(resourcePath.toString)): is =>
          ensureExternalDirectoryExists()
          val targetPath = externalDirPath.resolve(fileName.toPath)

          if !overwrite && Files.exists(targetPath) then
            targetPath
          else
            try
              if overwrite then
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING)
              else
                Files.copy(is, targetPath)
              targetPath
            catch
              case _: java.nio.file.FileAlreadyExistsException if !overwrite => targetPath

    /**
     * Reads the content of a configuration file located in the external folder.
     */
    def readConfig(fileName: into[ConfigFilePath]): Try[String] =
      Try:
        val targetPath = externalDirPath.resolve(fileName.toPath)
        if !Files.exists(targetPath) then
          throw new FileNotFoundException(s"Config file does not exist at: $targetPath")

        val bytes = Files.readAllBytes(targetPath)
        new String(bytes, StandardCharsets.UTF_8)


    /**
     * Writes or updates text content into a file within the external folder.
     */
    def writeConfig(fileName: into[ConfigFilePath], content: String): Try[Path] =
      Try:
        ensureExternalDirectoryExists()
        val targetPath = externalDirPath.resolve(fileName.toPath)

        Files.write(targetPath, content.getBytes(StandardCharsets.UTF_8))
        targetPath

    def writeConfigIfMissing(fileName: into[ConfigFilePath], content: String): Try[Path] =
      Try:
        ensureExternalDirectoryExists()
        val targetPath = externalDirPath.resolve(fileName.toPath)
        try
          Files.write(
            targetPath,
            content.getBytes(StandardCharsets.UTF_8),
            java.nio.file.StandardOpenOption.CREATE_NEW,
            java.nio.file.StandardOpenOption.WRITE
          )
        catch
          case _: java.nio.file.FileAlreadyExistsException => ()
        targetPath

    private def ensureExternalDirectoryExists(): Unit =
      if !Files.exists(externalDirPath) then
        Files.createDirectories(externalDirPath)

    private def openResource(resourcePath: String): InputStream =
      val hasUrlScheme = resourcePath.matches("^[A-Za-z][A-Za-z0-9+.-]*:.*")
      if hasUrlScheme then
        java.net.URI.create(resourcePath).toURL.openStream()
      else
        Option(getClass.getClassLoader.getResourceAsStream(resourcePath))
          .getOrElse(throw new FileNotFoundException(s"Resource file not found: $resourcePath"))


end ConfigsManager

extension (sc: StringContext)
  def config(args: Any*): ConfigFilePath =
    val path = sc.s(args*)
    ConfigsManager.resolvePathString(path)
