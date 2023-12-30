import io.github.sgpublic.backsql.core.BackFilenameEncoder
import kotlin.test.Test

class ArchieveNameTest {
    @Test
    fun test() {
        println("new filename: ${BackFilenameEncoder.createFilename()}")
    }
}