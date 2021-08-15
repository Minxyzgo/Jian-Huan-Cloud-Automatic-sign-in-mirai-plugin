@file:Suppress("unused")

package org.example.my.plugin

import kotlinx.coroutines.*
import net.mamoe.mirai.*
import net.mamoe.mirai.console.plugin.jvm.*
import net.mamoe.mirai.console.util.*
import net.mamoe.mirai.contact.*
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.*
import java.time.*
import java.util.*

object MyPluginMain : KotlinPlugin(
    @OptIn(ConsoleExperimentalApi::class)
    JvmPluginDescription.loadFromResource()
) {
    val delayMillis = Duration.ofMinutes(3 * 60 + 1).toMillis()
    val signInGroup = 826900096L
    val masterId = 1225327866L
    var job: Job? = null
    val passwd = "123456"
    val bot: Bot
        get() = Bot.instances.first()
    var lastMap: Map<String, Any>? = null
    var enable = false
    var totalIntegral = 0
    var totalDiamond = 0

    override fun onEnable() {
        this.globalEventChannel().subscribeAlways<GroupMessageEvent> {
            if(message.size > 2 && message[1] is At && sender.id.let { it == 3542343807L || it == masterId }) {
                val at0 = message[1] as At
                val text = message[2].content.trim()
                println("get")
                if(at0.target == bot.id && text.startsWith("获得")) {
                    val message = text.removePrefix("获得").removeSuffix("\uD83D\uDC8E")
                    val all = message.split("\uD83C\uDF55")
                    totalIntegral += all.first().toInt()
                    totalDiamond += all[1].toInt()
                    return@subscribeAlways
                }
            }
            if(sender.id != masterId) return@subscribeAlways
            this.message.forEach {
                if(it is PlainText) {
                    when(it.content.trim()) {
                        ".启用" -> {
                            if(enable) group.sendMessage("已启用")
                            else firstLogin(group)
                        }

                        ".禁用" -> {
                            if(enable) {
                                job!!.cancel()
                                lastMap = null
                                enable = false
                                totalDiamond = 0
                                totalIntegral = 0
                                group.sendMessage("👌")
                            } else {
                                group.sendMessage("未启用")
                            }
                        }

                        ".当前语句" -> {
                            if(enable) {
                                group.sendMessage(lastMap?.get("signInMessage")?.toString() ?: "没有找到语句")
                            } else {
                                group.sendMessage("未启用")
                            }
                        }

                        ".下次时间" -> {
                            if(enable) {
                                val nowTime = Date().time
                                val lastSignTime = lastMap!!["lastSignTime"] as Date
                                val next = Duration.ofHours(3).toMillis() - (nowTime - lastSignTime.time)
                                group.sendMessage("还剩${Duration.ofMillis(next).toMinutes()}分钟")
                            } else {
                                group.sendMessage("未启用")
                            }
                        }

                        ".刷新" -> {
                            if(enable) {
                                lastMap = NetWork.newRequest(bot.id, passwd)
                                group.sendMessage("👌")
                            } else {
                                group.sendMessage("未启用")
                            }
                        }

                        ".当前积分" -> {
                            if(enable) {
                                val integral = lastMap!!["integral"]!! as Int
                                val diamond = lastMap!!["diamond"]!! as Int
                                group.sendMessage("$integral\uD83C\uDF55$diamond\uD83D\uDC8E")
                            } else {
                                group.sendMessage("未启用")
                            }
                        }

                        ".累计获得" -> {
                            if(enable) {
                                group.sendMessage("$totalIntegral\uD83C\uDF55$totalDiamond\uD83D\uDC8E")
                            } else {
                                group.sendMessage("未启用")
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun firstLogin(group: Group) {
        val map = NetWork.newRequest(bot.id, passwd)
        val nowTime = Date().time
        val lastSignTime = map["lastSignTime"] as Date
        val next = Duration.ofHours(3).toMillis() - (nowTime - lastSignTime.time)
        group.sendMessage("""
            启用自动签到成功!
            上一次签到时间: ${NetWork.dataFormat.format(map["lastSignTime"])}
            距离下一次可签到时间还剩: ${Duration.ofMillis(next).toMinutes()}分
            本次签到语句: ${map["signInMessage"]}
        """.trimIndent())
        lastMap = map
        enable = true
        val _signInGroup = bot.getGroup(signInGroup)!!

        if(next <= 0) {
            job = launch {
                _signInGroup.sendMessage(map["signInMessage"]!!.toString())

                while(true) {
                    delay(delayMillis)
                    lastMap = NetWork.newRequest(bot.id, passwd)
                    _signInGroup.sendMessage(lastMap!!["signInMessage"]!!.toString())
                }
            }
        } else {
            println("next: $next")
            job = launch {
                delay(next)
                lastMap = NetWork.newRequest(bot.id, passwd)
                _signInGroup.sendMessage(lastMap!!["signInMessage"]!!.toString())
                while(true) {
                    delay(delayMillis)
                    lastMap = NetWork.newRequest(bot.id, passwd)
                    _signInGroup.sendMessage(lastMap!!["signInMessage"]!!.toString())
                }
            }
        }
    }
}