package com.gitee.planners.api.job.target

import com.gitee.planners.api.PlayerTemplateAPI.plannersTemplate
import com.gitee.planners.api.common.entity.ProxyBukkitEntity
import com.gitee.planners.api.common.metadata.EntityMetadataManager
import com.gitee.planners.api.common.metadata.Metadata
import com.gitee.planners.api.common.metadata.MetadataContainer
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import taboolib.common.util.Vector
import taboolib.platform.util.toBukkitLocation
import java.util.*

sealed interface ProxyTarget<T> {

    val instance: T

    // ============ 嵌套接口 ============

    interface Named {
        fun getName(): String
    }

    interface Location<T> : ProxyTarget<T> {

        fun getWorld(): String

        fun getBukkitWorld(): World?

        fun getBukkitLocation(): org.bukkit.Location

        fun getX(): Double

        fun getY(): Double

        fun getZ(): Double

        fun add(x: Double, y: Double, z: Double)

        fun getNearbyLivingEntities(vector: Vector): List<LivingEntity>
    }

    interface Entity<T> : Location<T>, Named {

        fun getUniqueId(): UUID

        fun getEntityType(): EntityType

        fun getBukkitEyeLocation(): org.bukkit.Location

        fun isValid(): Boolean
    }

    interface CommandSender<T> : ProxyTarget<T> {

        fun sendMessage(message: String)

        fun dispatchCommand(command: String): Boolean
    }

    interface Containerization {

        fun getMetadata(id: String): Metadata?

        fun setMetadata(id: String, data: Metadata)
    }

    // ============ 嵌套实现类 ============

    open class BukkitLocation(override val instance: org.bukkit.Location) : Location<org.bukkit.Location> {

        override fun getWorld(): String {
            return instance.world!!.name
        }

        override fun getBukkitWorld(): World? {
            return instance.world
        }

        override fun getX(): Double {
            return instance.x
        }

        override fun getY(): Double {
            return instance.y
        }

        override fun getZ(): Double {
            return instance.z
        }

        override fun getBukkitLocation(): org.bukkit.Location {
            return instance
        }

        override fun getNearbyLivingEntities(vector: Vector): List<LivingEntity> {
            return getBukkitWorld()!!
                .getNearbyEntities(this.instance, vector.x, vector.y, vector.z)
                .filterIsInstance<LivingEntity>()
        }

        override fun add(x: Double, y: Double, z: Double) {
            this.instance.add(x, y, z)
        }

        override fun toString(): String {
            return "ProxyTarget.BukkitLocation(instance=$instance)"
        }
    }

    class TabooLocation(val location: taboolib.common.util.Location) : Location<taboolib.common.util.Location> {

        override val instance = location

        override fun getWorld(): String {
            return location.world!!
        }

        override fun getBukkitWorld(): World? {
            return Bukkit.getWorld(getWorld()) ?: error("Couldn't get world from ${getWorld()}'")
        }

        override fun getX(): Double {
            return location.x
        }

        override fun getY(): Double {
            return location.y
        }

        override fun getBukkitLocation(): org.bukkit.Location {
            return location.toBukkitLocation()
        }

        override fun getZ(): Double {
            return location.z
        }

        override fun add(x: Double, y: Double, z: Double) {
            location.add(x, y, z)
        }

        override fun getNearbyLivingEntities(vector: Vector): List<LivingEntity> {
            return getBukkitWorld()!!
                .getNearbyEntities(instance.toBukkitLocation(), vector.x, vector.y, vector.z)
                .filterIsInstance<LivingEntity>()
        }

        override fun toString(): String {
            return "ProxyTarget.TabooLocation(location=$location)"
        }
    }

    class TargetBlock(val block: Block) : BukkitLocation(block.location) {

        override fun toString(): String {
            return "ProxyTarget.Block(block=$block)"
        }
    }

    class BukkitEntity(override val instance: org.bukkit.entity.Entity) :
        Entity<org.bukkit.entity.Entity>,
        CommandSender<org.bukkit.entity.Entity>,
        Containerization {

        override fun getUniqueId(): UUID {
            return instance.uniqueId
        }

        override fun getEntityType(): EntityType {
            return instance.type
        }

        override fun getName(): String {
            return instance.name
        }

        override fun getBukkitEyeLocation(): org.bukkit.Location {
            return (instance as? LivingEntity)?.eyeLocation ?: getBukkitLocation()
        }

        override fun isValid(): Boolean {
            return instance.isValid
        }

        override fun getWorld(): String {
            return instance.world.name
        }

        override fun getBukkitWorld(): World? {
            return instance.world
        }

        override fun getBukkitLocation(): org.bukkit.Location {
            return instance.location
        }

        override fun getX(): Double {
            return instance.location.x
        }

        override fun getY(): Double {
            return instance.location.y + instance.height / 2
        }

        override fun getZ(): Double {
            return instance.location.z
        }

        override fun add(x: Double, y: Double, z: Double) {
        }

        override fun sendMessage(message: String) {
            instance.sendMessage(message)
        }

        override fun dispatchCommand(command: String): Boolean {
            return Bukkit.dispatchCommand(instance, command)
        }

        override fun getNearbyLivingEntities(vector: Vector): List<LivingEntity> {
            return getBukkitWorld()!!
                .getNearbyEntities(instance.location, vector.x, vector.y, vector.z)
                .filterIsInstance<LivingEntity>()
        }

        private fun getMetadataContainer(): MetadataContainer {
            return if (instance is Player) {
                instance.plannersTemplate
            } else {
                EntityMetadataManager[ProxyBukkitEntity(instance)]
            }
        }

        override fun getMetadata(id: String): Metadata? {
            return getMetadataContainer()[id]
        }

        override fun setMetadata(id: String, data: Metadata) {
            getMetadataContainer()[id] = data
        }

        override fun toString(): String {
            return "ProxyTarget.BukkitEntity(instance=$instance)"
        }

        override fun hashCode(): Int {
            return instance.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ProxyTarget<*>

            return instance == other.instance
        }
    }

    class Console(val console: ConsoleCommandSender) : CommandSender<ConsoleCommandSender>, Containerization {

        override val instance = console

        override fun sendMessage(message: String) {
            console.sendMessage(message)
        }

        override fun dispatchCommand(command: String): Boolean {
            return Bukkit.dispatchCommand(console, command)
        }

        override fun getMetadata(id: String): Metadata? {
            return container[id]
        }

        override fun setMetadata(id: String, data: Metadata) {
            container[id] = data
        }

        override fun toString(): String {
            return "ProxyTarget.Console(console=$console)"
        }

        companion object {
            // 控制台元数据 在关服后数据丢失
            private val container = object : MetadataContainer(emptyMap()) {}
        }
    }

    // ============ 伴生对象工厂方法 ============

    companion object {

        inline fun <reified T : ProxyTarget<*>> ProxyTarget<*>.cast(): T? {
            return this as? T
        }

        inline fun <reified T : ProxyTarget<*>> ProxyTarget<*>.castUnsafely(): T {
            return cast<T>()!!
        }

        fun of(entity: org.bukkit.entity.Entity): BukkitEntity {
            return BukkitEntity(entity)
        }

        fun of(player: Player): BukkitEntity {
            return BukkitEntity(player)
        }

        fun of(location: org.bukkit.Location): BukkitLocation {
            return BukkitLocation(location)
        }

        fun of(location: taboolib.common.util.Location): TabooLocation {
            return TabooLocation(location)
        }

        fun of(block: Block): TargetBlock {
            return TargetBlock(block)
        }

        fun of(sender: ConsoleCommandSender): Console {
            return Console(sender)
        }

        fun of(sender: org.bukkit.command.CommandSender): ProxyTarget<*> {
            return when (sender) {
                is ConsoleCommandSender -> Console(sender)
                is org.bukkit.entity.Entity -> BukkitEntity(sender)
                else -> throw IllegalStateException("Target ${sender::class.java.name} is not supported")
            }
        }

        fun of(any: Any): ProxyTarget<*> {
            return when (any) {
                is org.bukkit.command.CommandSender -> of(any)
                is org.bukkit.Location -> BukkitLocation(any)
                is Block -> TargetBlock(any)
                is taboolib.common.util.Location -> TabooLocation(any)
                else -> throw IllegalStateException("Target ${any::class.java.name} is not supported")
            }
        }
    }
}

// 扩展函数语法糖
fun org.bukkit.entity.Entity.asTarget(): ProxyTarget.BukkitEntity = ProxyTarget.of(this)
fun Player.asTarget(): ProxyTarget.BukkitEntity = ProxyTarget.of(this)
fun org.bukkit.Location.asTarget(): ProxyTarget.BukkitLocation = ProxyTarget.of(this)
fun taboolib.common.util.Location.asTarget(): ProxyTarget.TabooLocation = ProxyTarget.of(this)
fun Block.asTarget(): ProxyTarget.TargetBlock = ProxyTarget.of(this)
fun ConsoleCommandSender.asTarget(): ProxyTarget.Console = ProxyTarget.of(this)
fun org.bukkit.command.CommandSender.asTarget(): ProxyTarget<*> = ProxyTarget.of(this)
