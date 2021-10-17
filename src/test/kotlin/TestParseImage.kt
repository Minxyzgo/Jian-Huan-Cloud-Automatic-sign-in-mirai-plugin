import com.luciad.imageio.webp.WebPReadParam
import org.example.my.plugin.imageComparison
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.stream.FileImageInputStream

fun main() {
    val root = File(System.getProperty("user.dir"))
    val image = root.resolve("tncode.webp")
    val reader = ImageIO.getImageReadersByMIMEType("image/webp").next()

    // Configure decoding parameters
    val readParam = WebPReadParam()
    readParam.isBypassFiltering = true

    // Configure the input on the ImageReader
    reader.input = FileImageInputStream(image)
    println("distance: ${imageComparison(root, reader, readParam)}")
}
