package org.runestar.client.updater.mapper.std.classes

import org.runestar.client.updater.mapper.IdentityMapper
import org.runestar.client.updater.mapper.extensions.and
import org.runestar.client.updater.mapper.extensions.predicateOf
import org.runestar.client.updater.mapper.extensions.type
import org.runestar.client.updater.mapper.tree.Class2
import org.objectweb.asm.Opcodes.BIPUSH
import org.objectweb.asm.Opcodes.SIPUSH

class Varps : IdentityMapper.Class() {
    override val predicate = predicateOf<Class2> { it.superType == Any::class.type }
            .and { it.interfaces.isEmpty() }
            .and { it.instanceFields.isEmpty() }
            .and { it.instanceMethods.isEmpty() }
            .and { it.staticFields.count { it.type == IntArray::class.type } >= 3 }
            .and { it.classInitializer?.instructions?.any { it.opcode == BIPUSH && it.intOperand == 32 } ?: false }
            .and { it.classInitializer?.instructions?.any { it.opcode == SIPUSH && it.intOperand == 2000 } ?: false }
}