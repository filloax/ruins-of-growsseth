package com.ruslan.growsseth.client.resource

import com.ruslan.growsseth.RuinsOfGrowsseth
import com.ruslan.growsseth.resource.MusicCommon
import com.ruslan.growsseth.utils.DecryptUtil
import com.ruslan.growsseth.utils.resLoc
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resources.FileToIdConverter
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import java.io.IOException
import java.io.InputStream
import javax.crypto.SecretKey

// TODO: separate into common, issue is resource listeners

/**
 * Used to load encrypted music files that would normally be monetized, at the
 * artist's wishes, to avoid having the high quality files plainly in the jar. We are aware that
 * this is not usual for a Minecraft mod, but it should not affect performance in a relevant way.
 * (If it does, please report!)
 * Yes, it's possible to reverse this with some effort, but please support the official (free)
 * release on spotify by Il Coro di Mammonk rather than doing that!
 * ---
 * Might reimplement with fabric sound library later,for now this works and is even loader-independent!
 */
object EncryptedMusicResources {
    @JvmField
    val LISTER = FileToIdConverter("soundsx", ".oggx")

    private const val KEY_PATH = "sounds.key"
    private var key: SecretKey? = null

    @JvmStatic
    fun checkEncryptedSoundStream(resourceLocation: ResourceLocation, inputStream: InputStream): InputStream {
        return if (resourceLocation.path.startsWith("soundsx")) {
            if (!MusicCommon.hasMusicKey) {
                throw IOException("Couldn't load encrypted music as no key loaded in mod build!")
            }
            // inputStream // debug with plain files
            DecryptUtil.decryptInputStream(key ?: throw IllegalStateException("Didn't load decryption key yet!"), inputStream)
        } else {
            inputStream
        }
    }

    class KeyListener : SimpleSynchronousResourceReloadListener {
        override fun getFabricId() = resLoc("sounds_key_listener")

        override fun onResourceManagerReload(resourceManager: ResourceManager) {
            if (MusicCommon.hasMusicKey) {
                key = DecryptUtil.readKey(resourceManager.open(resLoc(KEY_PATH)), MusicCommon.musicPw)
                RuinsOfGrowsseth.LOGGER.info("Read music key!")
            } else {
                RuinsOfGrowsseth.LOGGER.warn("Cannot read key, not setup during build correctly!")
            }
        }
    }
}