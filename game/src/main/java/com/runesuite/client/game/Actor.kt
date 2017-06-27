package com.runesuite.client.game

import com.runesuite.client.base.Client
import com.runesuite.client.base.access.XActor

abstract class Actor(override val accessor: XActor) : Entity(accessor), ActorTargeting {

    private val plane = Client.accessor.plane

    override val npcTargetIndex: Int?
        get() = accessor.targetIndex.let { if (it in 0..32767) it else null }

    override val playerTargetIndex: Int?
        get() = accessor.targetIndex.let { if (it > 32768) it - 32768 else null }

    override val position get() = Position(accessor.x, accessor.y, accessor.height, plane)

    val location get() = SceneTile(accessor.pathX[0], accessor.pathY[0], plane)

    override val orientation get() = Angle(accessor.orientation)

    val overheadText: String? get() = accessor.overheadMessage

    override val model: Model?
        get() = accessor.getModel()?.run { Model(position.copy(height = 0), orientation,
                    indicesX, indicesY, indicesZ,
                    verticesX, verticesY, verticesZ) }
}