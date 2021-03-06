package org.runestar.client.game.api

import org.runestar.client.game.raw.access.XPlayer

class Player(override val accessor: XPlayer) : Actor(accessor) {

    override val plane: Int get() = accessor.plane

    val name: Username? get() = accessor.username?.let { Username(it) }

    val actions: List<String> get() = accessor.actions.toList()

    val combatLevel get() = accessor.combatLevel

    val headIconPrayer get(): HeadIconPrayer? = HeadIconPrayer.of(accessor.headIconPrayer)

    val headIconPk get(): HeadIconPk? = HeadIconPk.of(accessor.headIconPk)

    val team get() = accessor.team

    val appearance: PlayerAppearance? get() = accessor.appearance?.let { PlayerAppearance(it) }

    override fun toString(): String {
        return "Player(name=$name)"
    }
}