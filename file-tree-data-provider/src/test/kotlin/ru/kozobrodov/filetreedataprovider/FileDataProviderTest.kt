package ru.kozobrodov.filetreedataprovider

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.NotDirectoryException
import kotlin.properties.Delegates

class FileDataProviderTest {
    private var dataProvider: FileDataProvider by Delegates.notNull()

    @Before
    fun init() {
        val baseDir = File(javaClass.getResource("/TestData").toURI())
        dataProvider = FileDataProvider(baseDir.absolutePath)
    }

    @Test
    fun `list root directory`() = runBlocking {
        val expectedSubpaths = setOf(
                "Inner directory",
                "jconsole.jar",
                "passport-wallet.pdf",
                "rar-archive.rar",
                "test-image.jpg",
                "test-zip.zip",
                "zip-with-inner-zip.zip"
        )

        val result = dataProvider.list(listOf())
        assertEquals(7, result.size)

        val resultSubpaths = result.map { it.path }.toSet()
        assertEquals(expectedSubpaths, resultSubpaths)
    }

    @Test
    fun `list subdirectory`() = runBlocking {
        val expectedSubpath = "Inner directory${File.separator}test-txt"
        val result = dataProvider.list(listOf("Inner directory"))
        assertEquals(1, result.size)
        assertEquals(expectedSubpath, result[0].path)
    }

    @Test
    fun `list ZIP`() = runBlocking {
        val expectedSubpath = "test-zip.zip${File.separator}Inner directory"
        val result = dataProvider.list(listOf("test-zip.zip"))
        assertEquals(1, result.size)
        assertEquals(expectedSubpath, result[0].path)
    }

    @Test(expected = NotDirectoryException::class)
    fun `attempt to list non-expandable file`() {
        runBlocking {
            dataProvider.list(listOf("test-image.jpg"))
        }
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `attempt to list unsupported type of archive`() {
        runBlocking {
            dataProvider.list(listOf("rar-archive.rar"))
        }
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `attempt to list inner ZIP archive`() {
        runBlocking {
            dataProvider.list(listOf("zip-with-inner-zip.zip", "test-zip.zip"))
        }
    }

    @Test(expected = FileNotFoundException::class)
    fun `attempt to list non-existing file`() {
        runBlocking {
            dataProvider.list(listOf("this file doesn't exist"))
        }
    }
}
