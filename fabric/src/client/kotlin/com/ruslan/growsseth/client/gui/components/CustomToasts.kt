package com.ruslan.growsseth.client.gui.components

import net.minecraft.client.gui.components.toasts.Toast
import net.minecraft.client.gui.components.toasts.ToastComponent
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

fun ToastComponent.addCustomToast(title: Component, message: Component? = null, item: ItemStack? = null) {
    if (item != null) {
        addToast(CustomTextItemToast.multiline(minecraft.font, title, item, message))
    } else {
        addToast(CustomTextToast.multiline(minecraft.font, title, message))
    }
}

fun ToastComponent.updateCustomToast(title: Component, message: Component? = null, item: ItemStack? = null) {
    val clazz: Class<out Toast> = if (item != null) CustomTextItemToast::class.java else CustomTextToast::class.java
    val toast = getToast(clazz, Toast.NO_TOKEN)
    if (toast == null) {
        addCustomToast(title, message, item)
    } else {
        if (item != null)
            (toast as CustomTextItemToast).reset(title, item, message, minecraft.font)
        else
            (toast as CustomTextToast).reset(title, message, minecraft.font)
    }
}