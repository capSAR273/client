package org.runestar.client.plugins.fpsthrottle

import org.runestar.client.game.raw.Client
import org.runestar.client.game.raw.access.XRasterProvider
import org.runestar.client.plugins.spi.PluginSettings
import org.runestar.client.utils.DisposablePlugin

class FpsThrottle : DisposablePlugin<FpsThrottle.Settings>() {

    override val defaultSettings = Settings()

    override val name = "FPS Throttle"

    override fun start() {
        if (ctx.settings.sleepTimeMs <= 0) return
        val sleepTimeMs = ctx.settings.sleepTimeMs
        if (ctx.settings.onlyWhenUnfocused) {
            add(XRasterProvider.drawFull0.exit.subscribe {
                if (!Client.accessor.canvas.isFocusOwner) {
                    Thread.sleep(sleepTimeMs)
                }
            })
        } else {
            add(XRasterProvider.drawFull0.exit.subscribe {
                Thread.sleep(sleepTimeMs)
            })
        }
    }

    data class Settings(
            val onlyWhenUnfocused: Boolean = true,
            val sleepTimeMs: Long = 50L
    ) : PluginSettings()
}