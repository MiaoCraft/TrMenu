package trplugins.menu.api.receptacle.vanilla.window

import net.minecraft.server.v1_16_R3.*
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftItemStack
import org.bukkit.craftbukkit.v1_16_R3.util.CraftChatMessage
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.setProperty
import taboolib.library.reflex.Reflex.Companion.unsafeInstance
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.sendPacket
import taboolib.platform.util.isAir
import trplugins.menu.api.receptacle.provider.PlatformProvider
import trplugins.menu.api.receptacle.vanilla.window.StaticInventory.staticInventory

/**
 * @author Arasple
 * @date 2020/12/4 21:25
 */
class NMSImpl : NMS() {

    private val emptyItemStack: net.minecraft.server.v1_16_R3.ItemStack? = CraftItemStack.asNMSCopy((ItemStack(Material.AIR)))
    private val version = MinecraftVersion.majorLegacy

    private fun Player.isBedrockPlayer() = PlatformProvider.isBedrockPlayer(this)

    override fun sendWindowsClose(player: Player, windowId: Int) {
        if (player.isBedrockPlayer()) {
            StaticInventory.close(player)
        } else {
            player.sendPacket(PacketPlayOutCloseWindow(windowId))
        }
    }

    override fun sendWindowsItems(player: Player, windowId: Int, items: Array<ItemStack?>) {
        when {
            player.isBedrockPlayer() -> {
                val inventory = player.staticInventory!!
                items.forEachIndexed { index, item ->
                    if (index >= inventory.size) {
                        return
                    }
                    inventory.setItem(index, item)
                }
            }
            version >= 11701 -> {
                sendPacket(
                    player,
                    PacketPlayOutWindowItems::class.java.unsafeInstance(),
                    "containerId" to windowId,
                    "stateId" to 1,
                    "items" to items.map { i -> toNMSCopy(i) }.toList(),
                    "carriedItem" to emptyItemStack
                )
            }
            version >= 11700 -> {
                sendPacket(
                    player,
                    PacketPlayOutWindowItems::class.java.unsafeInstance(),
                    "containerId" to windowId,
                    "items" to items.map { i -> toNMSCopy(i) }.toList()
                )
            }
            version >= 11000 -> {
                sendPacket(
                    player,
                    PacketPlayOutWindowItems::class.java.unsafeInstance(),
                    "a" to windowId,
                    "b" to items.map { i -> toNMSCopy(i) }.toList()
                )
            }
            else -> {
                sendPacket(
                    player,
                    PacketPlayOutWindowItems::class.java.unsafeInstance(),
                    "a" to windowId,
                    "b" to items.map { i -> toNMSCopy(i) }.toTypedArray()
                )
            }
        }
    }

    override fun sendWindowsOpen(player: Player, windowId: Int, type: WindowLayout, title: String) {
        when {
            player.isBedrockPlayer() -> {
                StaticInventory.open(player, type, title)
            }
            version >= 11900 -> {
                sendPacket(
                    player,
                    PacketPlayOutOpenWindow::class.java.unsafeInstance(),
                    "containerId" to windowId,
                    "type" to Containers::class.java.getProperty(type.vanillaId, true),
                    "title" to CraftChatMessage.fromStringOrNull(title)
                )
            }
            MinecraftVersion.isUniversal -> {
                sendPacket(
                    player,
                    PacketPlayOutOpenWindow::class.java.unsafeInstance(),
                    "containerId" to windowId,
                    "type" to type.serialId,
                    "title" to CraftChatMessage.fromStringOrNull(title)
                )
            }
            version >= 11400 -> {
                sendPacket(
                    player,
                    PacketPlayOutOpenWindow(),
                    "a" to windowId,
                    "b" to type.serialId,
                    "c" to CraftChatMessage.fromStringOrNull(title)
                )
            }
            else -> {
                sendPacket(
                    player,
                    PacketPlayOutOpenWindow(),
                    "a" to windowId,
                    "b" to type.id,
                    "c" to ChatComponentText(title),
                    "d" to type.containerSize - 1 // Fixed ViaVersion can not view 6x9 menu bug.
                )
            }
        }
    }

    override fun sendWindowsSetSlot(player: Player, windowId: Int, slot: Int, itemStack: ItemStack?, stateId: Int) {
        when {
            player.isBedrockPlayer() -> {
                if (windowId == -1 && slot == -1) {
                    player.itemOnCursor.type = Material.AIR
                } else {
                    val inventory = player.staticInventory!!
                    if (slot >= 0 && slot < inventory.size) {
                        inventory.setItem(slot, itemStack)
                    }
                }
            }
            version >= 11701 -> {
                sendPacket(
                    player,
                    PacketPlayOutSetSlot::class.java.unsafeInstance(),
                    "containerId" to windowId,
                    "stateId" to -1,
                    "slot" to slot,
                    "itemStack" to toNMSCopy(itemStack)
                )
            }
            else -> {
                player.sendPacket(PacketPlayOutSetSlot(windowId, slot, toNMSCopy(itemStack)))
            }
        }
    }

    override fun sendWindowsUpdateData(player: Player, windowId: Int, property: Int, value: Int) {
        TODO("Not yet implemented")
    }

    private fun toNMSCopy(itemStack: ItemStack?): net.minecraft.server.v1_16_R3.ItemStack? {
        return if (itemStack.isAir()) emptyItemStack else CraftItemStack.asNMSCopy(itemStack)
    }

    private fun sendPacket(player: Player, packet: Any, vararg fields: Pair<String, Any?>) {
        fields.forEach { packet.setProperty(it.first, it.second) }
        player.sendPacket(packet)
    }

}