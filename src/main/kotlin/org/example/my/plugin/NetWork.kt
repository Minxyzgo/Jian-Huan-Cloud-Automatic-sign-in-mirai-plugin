package org.example.my.plugin

import okhttp3.*
import java.text.*
import java.time.*

object NetWork {
    val client = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    val dataFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    fun login(qq: Long, pass: String): String {
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
    fun getNew(qq: Long, pass: String): ResponseStack {
        val cookie = login(qq, pass)
        val mainRequest = Request.Builder()
            .url("https://sfe.simpfun.cn/point.php?action=main")
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
            .addHeader("referer", "https://sfe.simpfun.cn/point.php?action=main")
            .get()
            .build()
        val signInRequest = Request.Builder()
            .url("https://sfe.simpfun.cn/point.php?&action=sign")
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
            .addHeader("referer", "https://sfe.simpfun.cn/point.php?action=main")
            .get()
            .build()
        return ResponseStack(
            client.newCall(mainRequest).execute(),
            client.newCall(signInRequest).execute()
        )
    }

    fun newRequest(qq: Long, pass: String): Map<String, Any> {
        val (mainResponse, signInResponse) = getNew(qq, pass)
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
        result["signInMessage"] = signInAll[signInAll.indexOf("<td>本次签到内容</td>") + 1]
            .removePrefix("<td>")
            .removeSuffix("</td>")
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
        val signInResponse: Response
    )
}