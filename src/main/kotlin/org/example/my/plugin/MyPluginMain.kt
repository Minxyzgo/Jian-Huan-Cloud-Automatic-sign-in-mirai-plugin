@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.example.my.plugin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.console.rootDir
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.PlainText
import java.time.Duration
import java.util.*

object MyPluginMain : KotlinPlugin(
    @OptIn(ConsoleExperimentalApi::class)
    JvmPluginDescription.loadFromResource()
) {
    var signInGroup = 826900096L
    var masterId = 123456L
    var passwd = "1234567890"

    val delayMillis = Duration.ofMinutes(3 * 60 + 1).toMillis()
    var job: Job? = null
    val bot: Bot
        get() = Bot.instances.first()
    var lastMap: Map<String, Any>? = null
    var enable = false
    var totalIntegral = 0
    var totalDiamond = 0

    override fun onEnable() {

        readConfig()

        this.globalEventChannel().subscribeAlways<GroupMessageEvent> {
            //以下已失效
//            if(message.size > 2 && message[1] is At && sender.id.let { it == 3542343807L || it == masterId }) {
//                val at0 = message[1] as At
//                val text = message[2].content.trim()
//                if(at0.target == bot.id && text.startsWith("获得")) {
//                    val message = text.removePrefix("获得").removeSuffix("\uD83D\uDC8E")
//                    val all = message.split("\uD83C\uDF55")
//                    val integral = all.first().toInt()
//                    val diamond = all[1].toInt()
//                    totalIntegral += integral
//                    totalDiamond += diamond
//                    println("de-get diamond: $totalDiamond integral: $integral")
//                    return@subscribeAlways
//                }
//            }
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
            距离下一次可签到时间还剩: ${Duration.ofMillis(next).toMinutes().coerceAtLeast(0)}分
            本次签到语句: ${map["signInMessage"]}
        """.trimIndent())
        //暂时关闭
        lastMap = map
        enable = true
        val _signInGroup = bot.getGroup(signInGroup)!!

        val refresh = suspend {
            launch(Dispatchers.IO) {
                delay(100)
                val result = NetWork.newRequest(bot.id, passwd)
                val diamond = result["diamond"] as Int
                val integral = result["integral"] as Int
                val getDiamond = diamond - (lastMap!!["diamond"] as Int)
                val getIntegral = integral - (lastMap!!["integral"] as Int)
                lastMap = result
                if(integral < 0 || diamond < 0) {
                    logger.warning("Unknown refresh error")
                } else {
                    bot.getFriend(masterId)?.sendMessage("签到成功！得到$getIntegral\uD83C\uDF55$getDiamond\uD83D\uDC8E")
                    totalDiamond += getDiamond
                    totalIntegral += getIntegral
                }
            }
        }

        if(next <= 0) {
            job = launch {
                _signInGroup.sendMessage(map["signInMessage"]!!.toString())
                refresh()
                while(true) {
                    delay(delayMillis)
                    lastMap = NetWork.newRequest(bot.id, passwd)
                    _signInGroup.sendMessage(lastMap!!["signInMessage"]!!.toString())
                    refresh()
                }
            }
        } else {
            println("next: $next")
            job = launch {
                delay(next)
                lastMap = NetWork.newRequest(bot.id, passwd)
                _signInGroup.sendMessage(lastMap!!["signInMessage"]!!.toString())
                refresh()
                while(true) {
                    delay(delayMillis)
                    lastMap = NetWork.newRequest(bot.id, passwd)
                    _signInGroup.sendMessage(lastMap!!["signInMessage"]!!.toString())
                    refresh()
                }
            }
        }
    }

    fun readConfig() {
        val configFile = MiraiConsole.rootDir.resolve("signInConfig.properties")
        if(!configFile.exists()) {
            logger.warning("Read sign-in config failed: didn't exists")
            return
        }
        val properties = Properties().also { it.load(configFile.inputStream()) }
        signInGroup = properties.getOrDefault("signInGroup", signInGroup).toString().toLong()
        masterId = properties.getOrDefault("masterId", masterId).toString().toLong()
        passwd = properties.getOrDefault("signInGroup", passwd).toString()
    }
}