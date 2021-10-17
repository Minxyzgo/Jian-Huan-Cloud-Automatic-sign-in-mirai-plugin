package org.example.my.plugin

import com.luciad.imageio.webp.WebPReadParam
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.rootDir
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.text.SimpleDateFormat
import java.time.Duration
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.FileImageInputStream


object NetWork {
    private val client = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    val dataFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    private fun login(qq: Long, pass: String): String {
        val body = FormBody.Builder()
            .add("QQ", qq.toString())
            .add("pass", pass)
            .build()
        val request = Request.Builder()
            .url("https://sfe.simpfun.cn/login-redirect.php")
            .addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
            .addHeader("accept-encoding", "gzip, deflate, br")
            .addHeader("accept-language", "zh-CN,zh;q=0.9")
            .addHeader("cache-control", "max-age=0")
            .addHeader("sec-ch-ua-mobile", "?0")
            .addHeader("sec-fetch-dest", "document")
            .addHeader("sec-fetch-mode", "navigate")
            .addHeader("sec-fetch-site", "same-origin")
            .addHeader("sec-fetch-user", "?1")
            .addHeader("content-length", "31")
            .addHeader("content-type", "application/x-www-form-urlencoded")
            .addHeader("origin", "https://sfe.simpfun.cn")
            .addHeader("upgrade-insecure-requests", "1")
            .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36")
            .addHeader("referer", "https://sfe.simpfun.cn/login.html")
            .addHeader(":path", "/login-redirect.php")
            .post(body)
            .build()
        val call = client.newCall(request)
        val response = call.execute()
        return response.headers["set-cookie"]!!.split(";").first()
    }

    private fun getNew(qq: Long, pass: String): ResponseStack {
        val cookie = login(qq, pass)
        val mainRequest = Request.Builder()
            .normalRequest("https://sfe.simpfun.cn/point.php?action=main", cookie, "https://sfe.simpfun.cn/point.php?action=main")
            .get()
            .build()
        val signInRequest = Request.Builder()
            .normalRequest("https://sfe.simpfun.cn/point.php?&action=sign", cookie, "https://sfe.simpfun.cn/point.php?action=main")
            .get()
            .build()
        val signInMessageRequest = Request.Builder()
            .url("https://sfe.simpfun.cn/sign_code/tncode.php?t=0.9859233780021472")
            //.addHeader(":path", "/sign_code/tncode.php?t=0.9859233780021472")
            .addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
            .addHeader("accept-encoding", "gzip, deflate, br")
            .addHeader("accept-language", "zh-CN,zh;q=0.9")
            .addHeader("cookie", cookie)
            .addHeader("sec-ch-ua", "\"Chromium\";v=\"94\", \"Google Chrome\";v=\"94\", \";Not A Brand\";v=\"99\"")
            .addHeader("sec-ch-ua-mobile", "?0")
            .addHeader("sec-ch-ua-platform", "\"Windows\"")
            .addHeader("sec-fetch-dest", "document")
            .addHeader("sec-fetch-mode", "navigate")
            .addHeader("sec-fetch-site", "none")
            .addHeader("sec-fetch-user", "?1")
            .addHeader("upgrade-insecure-requests", "1")
            .addHeader("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36")
            .get()
            .build()
        lateinit var signInMessageResponse: Response
        lateinit var bytes: ByteArray
        lateinit var tempFile: File
        lateinit var reader: ImageReader
        lateinit var readParam: WebPReadParam
        lateinit var authenticationCookie: String
        var distance: Int = -1

        val imageRequest = {
            signInMessageResponse = client.newCall(signInMessageRequest).execute()
            bytes = signInMessageResponse.body!!.byteStream().readBytes()
            tempFile = MiraiConsole.rootDir.resolve("last_image.webp")
            tempFile.writeBytes(bytes)
            reader = ImageIO.getImageReadersByMIMEType("image/webp").next()

            // Configure decoding parameters
            readParam = WebPReadParam()
            readParam.isBypassFiltering = true

            // Configure the input on the ImageReader
            reader.input = FileImageInputStream(tempFile)

            distance = imageComparison(MiraiConsole.rootDir, reader, readParam)
            authenticationCookie = signInMessageResponse.headers["set-cookie"]!!.split(";").first()
            signInMessageResponse.close()
        }

        imageRequest()

        val authenticationRequest = {
            println("distance: $distance")
            Request.Builder()
                .url("https://sfe.simpfun.cn/sign_code/check.php?tn_r=$distance")
                .addHeader(
                    "accept",
                    "ext/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
                )
                .addHeader("accept-encoding", "gzip, deflate, br")
                .addHeader("accept-language", "zh-CN,zh;q=0.9")
                .addHeader("cache-contro", "max-age=0")
                .addHeader("cookie", "$cookie; $authenticationCookie")
                .addHeader("sec-ch-ua", "\"Chromium\";v=\"94\", \"Google Chrome\";v=\"94\", \";Not A Brand\";v=\"99\"")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("sec-ch-ua-platform", "\"Windows\"")
                .addHeader("sec-fetch-dest", "document")
                .addHeader("sec-fetch-mode", "navigate")
                .addHeader("sec-fetch-site", "none")
                .addHeader("sec-fetch-user", "?1")
                .addHeader("upgrade-insecure-requests", "1")
                .addHeader(
                    "user-agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36"
                )
                .addHeader("referer", "https://sfe.simpfun.cn/point.php?&action=sign")
                .get()
                .build()
        }
        var result = client.newCall(authenticationRequest()).execute().use { it.body!!.string() }
        var retry = 0
        while(result == "error" && retry < 5) {
            retry++
            MiraiConsole.mainLogger.info("Picture parsing failed, trying for the $retry time...")
            Thread.sleep(100)
            imageRequest()
            result = client.newCall(authenticationRequest()).execute().use { it.body!!.string() }
        }
        if(result == "error") throw RuntimeException("Unknown Exception: Image parse error after five attempts")
        return ResponseStack(
            client.newCall(mainRequest).execute(),
            client.newCall(signInRequest).execute(),
            result
        )
    }

    fun newRequest(qq: Long, pass: String): Map<String, Any> {
        val (mainResponse, signInResponse, signInMessage) = getNew(qq, pass)
        val signInBody = signInResponse.body!!.charStream()
        val signInAll = signInBody.readLines().filter { it.isNotBlank() }.map { it.trim() }
        val mainBody = mainResponse.body!!.charStream()
        val mainAll = mainBody.readLines().filter { it.isNotBlank() }.map { it.trim() }
        val result = mutableMapOf<String, Any>()
        result["lastSignTime"] = signInAll[signInAll.indexOf("<td>上次签到时间</td>") + 1]
            .removePrefix("<td>")
            .removeSuffix("</td>")
            .let {
                dataFormat.parse(it)
            }
        result["signInMessage"] = signInMessage
        result["canSignIn"] = signInAll[signInAll.indexOf("<td>当前是否可进行签到<small>(距离上次满三小时)</small></td>") + 1]
            .removePrefix("<td>")
            .removeSuffix("</td>")
            .let {
                it == "是"
            }
        result["integral"] = mainAll[mainAll.indexOf("<td>我的积分</td>") + 1]
            .removePrefix("<td>")
            .removeSuffix("</td>")
            .toInt()
        result["diamond"] = mainAll[mainAll.indexOf("<td>我的钻石</td>") + 1]
            .removePrefix("<td>")
            .removeSuffix("</td>")
            .toInt()
        signInResponse.close()
        mainResponse.close()
        return result
    }

    data class ResponseStack(
        val mainResponse: Response,
        val signInResponse: Response,
        val signInMessage: String
    )
}