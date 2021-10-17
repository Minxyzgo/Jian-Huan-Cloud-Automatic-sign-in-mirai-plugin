package org.example.my.plugin

import okhttp3.Request
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.ImageReadParam
import javax.imageio.ImageReader
import kotlin.math.abs


fun Request.Builder.normalRequest(
    url: String,
    cookie: String,
    referer: String
): Request.Builder{
    return this
        .url(url)
        .addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
        .addHeader("accept-encoding", "gzip, deflate, br")
        .addHeader("accept-language", "zh-CN,zh;q=0.9")
        .addHeader("cache-control", "max-age=0")
        .addHeader("cookie", cookie)
        .addHeader("sec-ch-ua-mobile", "?0")
        .addHeader("sec-fetch-dest", "document")
        .addHeader("sec-fetch-mode", "navigate")
        .addHeader("sec-fetch-site", "same-origin")
        .addHeader("sec-fetch-user", "?1")
        .addHeader("upgrade-insecure-requests", "1")
        .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36")
        .addHeader("referer", referer)
}

fun imageComparison(root: File, imageReader: ImageReader, readParam: ImageReadParam): Int {
    val image = imageReader.read(0, readParam)
    val h = image.height
    val w = image.width
    val split1 = image.getSubimage(0, 0, w, h / 3)
    val split2 = image.getSubimage(0, h / 3 * 2, w, h / 3)
    val split1_f = root.resolve("split1_f.webp")
    val split2_f = root.resolve("split2_f.webp")
    val distance = getDistance(split1, split2)
    ImageIO.write(split1, "png", split1_f)
    ImageIO.write(split2, "png", split2_f)
    return distance
}

fun getDistance(image1: BufferedImage, image2: BufferedImage, threshold: Int = 60): Int {
    val w = image1.width
    val h = image1.height
    if(w != image2.width || h != image2.height) throw IllegalArgumentException("The two images are of different sizes!!")
    for(x in 0 until w) {
        for(y in 0 until h) {
            val c1 = Color(image1.getRGB(x, y))
            val c2 = Color(image2.getRGB(x, y))

            if(abs(c1.red - c2.red) > threshold
                || abs(c1.green - c2.green) > threshold
                || abs(c1.blue - c2.blue) > threshold)
            {
                image1.setRGB(x, y, Color.red.rgb)
                image2.setRGB(x, y, Color.red.rgb)
                return x - 2
            }
        }
    }

    return -1
}