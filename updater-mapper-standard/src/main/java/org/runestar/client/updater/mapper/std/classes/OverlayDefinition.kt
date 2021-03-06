package org.runestar.client.updater.mapper.std.classes

import org.kxtra.lang.list.startsWith
import org.runestar.client.updater.mapper.IdentityMapper
import org.runestar.client.updater.mapper.annotations.DependsOn
import org.runestar.client.updater.mapper.annotations.MethodParameters
import org.runestar.client.updater.mapper.extensions.Predicate
import org.runestar.client.updater.mapper.extensions.and
import org.runestar.client.updater.mapper.extensions.predicateOf
import org.runestar.client.updater.mapper.tree.Class2
import org.runestar.client.updater.mapper.tree.Method2
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.Type.*

@DependsOn(DualNode::class)
class OverlayDefinition : IdentityMapper.Class() {
    override val predicate = predicateOf<Class2> { it.superType == type<DualNode>() }
            .and { it.interfaces.isEmpty() }
            .and { it.instanceFields.count { it.type == INT_TYPE } == 9 }
            .and { it.instanceFields.count { it.type == BOOLEAN_TYPE } == 1 }

    @DependsOn(Buffer::class)
    class read : IdentityMapper.InstanceMethod() {
        override val predicate = predicateOf<Method2> { it.returnType == Type.VOID_TYPE }
                .and { it.arguments.startsWith(type<Buffer>()) }
                .and { it.instructions.none { it.opcode == Opcodes.BIPUSH && it.intOperand == 8 } }
    }

    @DependsOn(Buffer::class)
    class readNext : IdentityMapper.InstanceMethod() {
        override val predicate = predicateOf<Method2> { it.returnType == Type.VOID_TYPE }
                .and { it.arguments.startsWith(type<Buffer>()) }
                .and { it.instructions.any { it.opcode == Opcodes.BIPUSH && it.intOperand == 8 } }
    }

    @MethodParameters
    class init : IdentityMapper.InstanceMethod() {
        override val predicate = predicateOf<Method2> { it.returnType == VOID_TYPE }
                .and { it.arguments.size in 0..1 }
                .and { it.instructions.count { it.isMethod } == 2 }
    }
}