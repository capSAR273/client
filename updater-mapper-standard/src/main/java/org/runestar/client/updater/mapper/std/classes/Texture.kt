package org.runestar.client.updater.mapper.std.classes

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type.*
import org.runestar.client.updater.mapper.IdentityMapper
import org.runestar.client.updater.mapper.OrderMapper
import org.runestar.client.updater.mapper.annotations.DependsOn
import org.runestar.client.updater.mapper.extensions.Predicate
import org.runestar.client.updater.mapper.extensions.and
import org.runestar.client.updater.mapper.extensions.predicateOf
import org.runestar.client.updater.mapper.extensions.type
import org.runestar.client.updater.mapper.tree.Class2
import org.runestar.client.updater.mapper.tree.Instruction2
import org.runestar.client.updater.mapper.tree.Method2

@DependsOn(Node::class)
class Texture : IdentityMapper.Class() {

    override val predicate = predicateOf<Class2> { it.superType == type<Node>() }
            .and { it.instanceFields.count { it.type == IntArray::class.type } == 5 }

    class records : OrderMapper.InConstructor.Field(Texture::class, 0) {
        override val predicate = predicateOf<Instruction2> { it.opcode == PUTFIELD && it.fieldType == IntArray::class.type }
    }

    class pixels : OrderMapper.InConstructor.Field(Texture::class, -1) {
        override val predicate = predicateOf<Instruction2> { it.opcode == PUTFIELD && it.fieldType == IntArray::class.type }
    }

    class isLoaded : OrderMapper.InConstructor.Field(Texture::class, 0) {
        override val predicate = predicateOf<Instruction2> { it.opcode == PUTFIELD && it.fieldType == BOOLEAN_TYPE }
    }

    class reset : IdentityMapper.InstanceMethod() {
        override val predicate = predicateOf<Method2> { it.returnType == VOID_TYPE }
                .and { it.instructions.count { it.isField } == 1 }
    }
}