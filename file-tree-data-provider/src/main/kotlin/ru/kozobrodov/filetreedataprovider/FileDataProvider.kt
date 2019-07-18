package ru.kozobrodov.filetreedataprovider

import java.io.FileNotFoundException
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
    private val typeToHandler = mapOf(
            "application/zip" to FileDataProvider::listZipArchive,
            "application/java-archive" to FileDataProvider::listZipArchive,
            "application/x-java-archive" to FileDataProvider::listZipArchive,
            "application/x-rar" to FileDataProvider::listRarArchive,
            "application/x-rar-compressed" to FileDataProvider::listRarArchive
    )

    /**
     * Lists directory specified by `pathSegments`.
     *
     * @param pathSegments path to directory represented
     *                     as list of it's segments
     * @return list of [FileData]
     */
    fun list(pathSegments: List<String>): List<FileData> {
        val origin = if (pathSegments.isNotEmpty()) {
            pathSegments
                .map { Paths.get(it) }
                .reduce { p1, p2 -> p1.resolve(p2) }
        } else {
            Paths.get("")
        }
        return list(origin, Paths.get(basePath), pathSegments.iterator())
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
            if (typeToHandler.containsKey(subPath.getType())) {
                return typeToHandler[subPath.getType()]
                        ?.invoke(this, origin, subPath, pathSegmentsIterator)
                        ?: emptyList()
            }
            path = subPath
        }
        if (Files.notExists(path))
            throw FileNotFoundException("File doesn't exist")
        return Files.list(path).use {
            it.map {
                path -> FileData(
                    "$origin/${path.fileName}",
                    path.getType(),
                    path.isExpandable()
            )
            }.toList()
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
}

data class FileData(val path: String, val type: String, val isExpandable: Boolean)

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
    return Files.probeContentType(this) ?: "<unknown_type>"
}
