package org.runestar.client.game.api

import org.runestar.client.game.raw.access.XGroundItem

class GroundItem(
        override val accessor: XGroundItem,
        val location: SceneTile
) : Entity(accessor) {

    override val orientation get() = Angle.ZERO

    override val position get() = location.center

    val id get() = accessor.id

    val quantity get() = accessor.quantity

    val item get() = Item(id, quantity)

    override fun toString(): String {
        return "GroundItem(item=$item, location=$location)"
    }

    companion object {

        const val MAX_QUANTITY = 65535
    }
}