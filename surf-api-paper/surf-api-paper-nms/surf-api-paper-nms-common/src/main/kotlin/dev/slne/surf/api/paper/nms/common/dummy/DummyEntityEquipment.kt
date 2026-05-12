package dev.slne.surf.api.paper.nms.common.dummy

import org.bukkit.entity.Entity
import org.bukkit.inventory.EntityEquipment
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

abstract class DummyEntityEquipment: EntityEquipment {
    override fun setItem(slot: EquipmentSlot, item: ItemStack?) {
        throw NotImplementedError()
    }

    override fun setItem(
        slot: EquipmentSlot,
        item: ItemStack?,
        silent: Boolean
    ) {
        setItem(slot, item)
    }

    override fun getItem(slot: EquipmentSlot): ItemStack {
        throw NotImplementedError()
    }

    override fun getItemInMainHand(): ItemStack {
        return getItem(EquipmentSlot.HAND)
    }

    override fun setItemInMainHand(item: ItemStack?) {
        setItem(EquipmentSlot.HAND, item)
    }

    override fun setItemInMainHand(item: ItemStack?, silent: Boolean) {
        setItemInMainHand(item)
    }

    override fun getItemInOffHand(): ItemStack {
        return getItem(EquipmentSlot.OFF_HAND)
    }

    override fun setItemInOffHand(item: ItemStack?) {
        setItem(EquipmentSlot.OFF_HAND, item)
    }

    override fun setItemInOffHand(item: ItemStack?, silent: Boolean) {
        setItemInOffHand(item)
    }

    override fun getItemInHand(): ItemStack {
        return itemInMainHand
    }

    override fun setItemInHand(stack: ItemStack?) {
        setItemInMainHand(stack)
    }

    override fun getHelmet(): ItemStack {
        return getItem(EquipmentSlot.HEAD)
    }

    override fun setHelmet(helmet: ItemStack?) {
        setItem(EquipmentSlot.HEAD, helmet)
    }

    override fun setHelmet(helmet: ItemStack?, silent: Boolean) {
        setHelmet(helmet)
    }

    override fun getChestplate(): ItemStack {
        return getItem(EquipmentSlot.CHEST)
    }

    override fun setChestplate(chestplate: ItemStack?) {
        setItem(EquipmentSlot.CHEST, chestplate)
    }

    override fun setChestplate(chestplate: ItemStack?, silent: Boolean) {
        setChestplate(chestplate)
    }

    override fun getLeggings(): ItemStack {
        return getItem(EquipmentSlot.LEGS)
    }

    override fun setLeggings(leggings: ItemStack?) {
        setItem(EquipmentSlot.LEGS, leggings)
    }

    override fun setLeggings(leggings: ItemStack?, silent: Boolean) {
        setLeggings(leggings)
    }

    override fun getBoots(): ItemStack {
        return getItem(EquipmentSlot.FEET)
    }

    override fun setBoots(boots: ItemStack?) {
        setItem(EquipmentSlot.FEET, boots)
    }

    override fun setBoots(boots: ItemStack?, silent: Boolean) {
        setBoots(boots)
    }

    override fun getArmorContents(): Array<out ItemStack?> {
        return arrayOf(boots, leggings, chestplate, helmet)
    }

    override fun setArmorContents(items: Array<out ItemStack>) {
        setBoots(items.getOrNull(0))
        setLeggings(items.getOrNull(1))
        setChestplate(items.getOrNull(2))
        setHelmet(items.getOrNull(3))
    }

    override fun clear() {
        throw NotImplementedError()
    }

    override fun getItemInHandDropChance(): Float {
        throw NotImplementedError()
    }

    override fun setItemInHandDropChance(chance: Float) {
        throw NotImplementedError()
    }

    override fun getItemInMainHandDropChance(): Float {
        throw NotImplementedError()
    }

    override fun setItemInMainHandDropChance(chance: Float) {
        throw NotImplementedError()
    }

    override fun getItemInOffHandDropChance(): Float {
        throw NotImplementedError()
    }

    override fun setItemInOffHandDropChance(chance: Float) {
        throw NotImplementedError()
    }

    override fun getHelmetDropChance(): Float {
        throw NotImplementedError()
    }

    override fun setHelmetDropChance(chance: Float) {
        throw NotImplementedError()
    }

    override fun getChestplateDropChance(): Float {
        throw NotImplementedError()
    }

    override fun setChestplateDropChance(chance: Float) {
        throw NotImplementedError()
    }

    override fun getLeggingsDropChance(): Float {
        throw NotImplementedError()
    }

    override fun setLeggingsDropChance(chance: Float) {
        throw NotImplementedError()
    }

    override fun getBootsDropChance(): Float {
        throw NotImplementedError()
    }

    override fun setBootsDropChance(chance: Float) {
        throw NotImplementedError()
    }

    override fun getHolder(): Entity {
        throw NotImplementedError()
    }

    override fun getDropChance(slot: EquipmentSlot): Float {
        throw NotImplementedError()
    }

    override fun setDropChance(slot: EquipmentSlot, chance: Float) {
        throw NotImplementedError()
    }
}