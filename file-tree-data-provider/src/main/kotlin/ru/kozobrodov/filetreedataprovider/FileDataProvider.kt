package ru.kozobrodov.filetreedataprovider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.tika.Tika
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.*
import kotlin.streams.toList

/**
 * Provider of data about files, works on top of directory
 * specified by `basePath` constructor parameter.
 */
class FileDataProvider(private val basePath: String) {
    /**
     * Mapping from file MIME type to appropriate handler-method.
     */
    private val typeToHandler = mapOf<String, SpecialTypeHandler>(
            "application/zip" to this::listZipArchive,
            "application/x-zip-compressed" to this::listZipArchive,
            "application/java-archive" to this::listZipArchive,
            "application/x-java-archive" to this::listZipArchive,
            "application/x-rar" to this::listRarArchive,
            "application/x-rar-compressed" to this::listRarArchive
    )

    /**
     * Lists directory specified by `pathSegments`.
     *
     * @param pathSegments path to directory represented
     *                     as list of it's segments
     * @return list of [FileData]
     */
    suspend fun list(pathSegments: List<String>): List<FileData> = withContext(Dispatchers.IO) {
        val origin = if (pathSegments.isNotEmpty()) {
            pathSegments
                .map { Paths.get(it) }
                .reduce { p1, p2 -> p1.resolve(p2) }
        } else {
            Paths.get("")
        }
        list(origin, Paths.get(basePath), pathSegments.iterator())
    }

    /**
     * Lists directory specified by `currentBase` and
     * `pathSegmentsIterator` parameters.
     *
     * For expandable files (like zip archives) this
     * method uses handlers from [typeToHandler] map.
     *
     * @param origin path of originally requested directory
     * @param currentBase path of already visited directory
     *                    (in current [file system][FileSystem])
     * @param pathSegmentsIterator [iterator][Iterator] of not
     *                              visited segments
     * @return list of [FileData]
     */
    private fun list(
            origin: Path,
            currentBase: Path,
            pathSegmentsIterator: Iterator<String>
    ): List<FileData> {
        var path = currentBase
        while (pathSegmentsIterator.hasNext()) {
            val subPath = path.resolve(pathSegmentsIterator.next())
            if (Files.notExists(subPath))
                throw FileNotFoundException("File doesn't exist")
            if (!subPath.isChildOf(currentBase)) // Check that we aren't going back in tree
                throw IllegalArgumentException("Special names are not allowed in path")
            if (typeToHandler.containsKey(subPath.getType())) {
                return typeToHandler[subPath.getType()]
                        ?.invoke(origin, subPath, pathSegmentsIterator)
                        ?: emptyList()
            }
            path = subPath
        }
        return Files.list(path).use {
            it.map { p -> p.toFileData(origin) }.toList()
        }
    }

    /**
     * Handler method which lists directory inside ZIP archive
     *
     * This handler changes [file system][FileSystem] for not
     * visited path segments and calls [list] method again
     * (i.e., works with indirect recursion).
     *
     * @param origin path of originally requested directory
     * @param zipPath path to ZIP file
     * @param pathSegmentsIterator [iterator][Iterator] of not
     *                              visited segments
     */
    private fun listZipArchive(
            origin: Path,
            zipPath: Path,
            pathSegmentsIterator: Iterator<String>
    ): List<FileData> {
        try {
            FileSystems.newFileSystem(zipPath, this.javaClass.classLoader).use {
                return list(origin, it.getPath("/"), pathSegmentsIterator)
            }
        } catch (e: ProviderNotFoundException) {
            // It's possible that there is no appropriate provider
            // (e.g., in case of nested zip archives)
            throw UnsupportedOperationException("Provider for zip filesystems was not found")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun listRarArchive(
            origin: Path,
            zipPath: Path,
            pathSegmentsIterator: Iterator<String>
    ): List<FileData> {
        // todo: implement
        throw UnsupportedOperationException("This type of archives is not supported yet")
    }

    /**
     * Extension method which allows to check if path
     * is expandable in terms of this `FileDataProvider`,
     * e.g. it's a directory or this provider has appropriate
     * handler to open file of this type.
     *
     * @return `true` is file is expandable, `false` otherwise
     */
    private fun Path.isExpandable(): Boolean =
            Files.isDirectory(this) or typeToHandler.containsKey(this.getType())

    /**
     * Returns `true` if this path is a child of specific path
     */
    private fun Path.isChildOf(other: Path): Boolean =
            this.normalize().startsWith(other)

    /**
     * Same as [Path.resolve], but ignores different file
     * systems and returns string instead of [Path]
     */
    private fun Path.resolveIgnoreFS(other: Path): String =
            this.resolve(other.toString()).toString()

    /**
     * Creates new [FileData] from this path and it's origin path
     */
    private fun Path.toFileData(origin: Path): FileData =
            FileData(
                    origin.resolveIgnoreFS(this.fileName),
                    this.fileName?.toString() ?: "",
                    this.getType(),
                    this.isExpandable()
            )
}

data class FileData(val path: String, val name: String, val type: String, val isExpandable: Boolean)
typealias SpecialTypeHandler = (Path, Path, Iterator<String>) -> List<FileData>

private val tika = Tika()
/**
 * Extension method which allows to get file MIME type,
 * if possible.
 *
 * @return string with MIME type or:
 *          - "directory" for directories
 *          - "<unknown_type>" if MIME type cannot be recognized
 */
private fun Path.getType(): String {
    if (Files.isDirectory(this))
        return "directory" // To be less platform-specific

    var type = Files.probeContentType(this)

    // Try Apache Tika
    if (type == null) {
        try {
            type = tika.detect(this.toString()) // doesn't access file

            if (type == null) {
                // try to guess by accessing file contents
                type = tika.detect(this)
            }
        } catch (ignore: IOException) {}
    }

    return type ?: "<unknown_type>"
}
