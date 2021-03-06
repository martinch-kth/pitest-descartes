package eu.stamp_project.mutationtest.descartes.stopmethods;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.pitest.sequence.*;

import static eu.stamp_project.mutationtest.descartes.stopmethods.StopMethodMatcher.*;
import static org.objectweb.asm.Opcodes.*;
import static org.pitest.bytecode.analysis.InstructionMatchers.opCode;
import static org.pitest.sequence.QueryStart.match;

public interface StopMethodMatchers {

    static StopMethodMatcher isEnumGenerated() {
        return ((classTree, methodTree) -> {
            ClassNode classNode = classTree.rawNode();
            //Inside enum declaration
            if(!classNode.superName.equals("java/lang/Enum"))
                return false;
            //Static
            MethodNode methodNode = methodTree.rawNode();
            if((methodNode.access & ACC_STATIC) == 0) //Both methods are static, if it is not an static method, then false
                return false;
            String returnTypeDescription = "L" + classNode.name + ";";
            // Class valueOf(String) or  Class[] values()
            //TODO: See if it is convenient to leave an overloaded version with MethodNode
            return matchesNameDesc(methodTree,"valueOf", "(Ljava/lang/String;)" + returnTypeDescription)
                    || matchesNameDesc(methodTree, "values", "()[" + returnTypeDescription);
        });
    }

    static  StopMethodMatcher isToString() {
        return forNameDesc("toString", "()Ljava/lang/String;");
    }

    static StopMethodMatcher isHashCode() {
        return forNameDesc("hashCode", "()I");
    }

    static StopMethodMatcher isDeprecated() {
        return (classTree, methodTree) ->
                matchesAccess(classTree, ACC_DEPRECATED) || matchesAccess(methodTree, ACC_DEPRECATED);
    }

    static StopMethodMatcher isEmptyVoid() {
        return forBody(QueryStart.match(opCode(RETURN)));
    }

    static StopMethodMatcher isSynthetic() {
        return (classTree, methodTree) -> methodTree.isSynthetic();
    }

    static Match<AbstractInsnNode> opCodeBetween(int lower, int upper) {
        return new Match<AbstractInsnNode>() {
            @Override
            public boolean test(Context<AbstractInsnNode> c, AbstractInsnNode abstractInsnNode) {
                int opcode = abstractInsnNode.getOpcode();
                return opcode >= lower && opcode <= upper;
            }
        };
    }

    static StopMethodMatcher isSimpleGetter() {
        return forBody(
                (match(opCode(GETSTATIC))
                        .or(match(opCode(ALOAD)).then(opCode(GETFIELD))))
                    .then(opCodeBetween(IRETURN, RETURN))
        );
    }

    static StopMethodMatcher isSimpleSetter() {
        return forBody(
                ((match(opCode(ALOAD)).then(opCodeBetween(ILOAD, ALOAD)).then(opCode(PUTFIELD)))
                        .or(match(opCode(ILOAD)).then(opCode(PUTSTATIC)))
                ).then(opCode(RETURN))
        );

    }

    static StopMethodMatcher returnsAConstant() {
        return forBody(match(opCodeBetween(1, 20)).then(opCodeBetween(IRETURN, RETURN)));
    }

    static StopMethodMatcher isDelegate() {

        SequenceQuery<AbstractInsnNode> paramMatch = match(opCodeBetween(21, 45));
        Match<AbstractInsnNode> returnOpcode = opCodeBetween(IRETURN, RETURN);

        return forBody(

                (
                        match(opCode(ALOAD).or(opCode(GETSTATIC)))
                        .or(match(opCode(ALOAD)).then(opCode(GETFIELD))) //Target on the stack
                        .zeroOrMore(paramMatch) // Param loop
                        .then(new Match<AbstractInsnNode>() {
                            @Override
                            public boolean test(Context<AbstractInsnNode> c, AbstractInsnNode abstractInsnNode) {
                                int opcode = abstractInsnNode.getOpcode();
                                return opcode == INVOKEVIRTUAL || opcode == INVOKESPECIAL || opcode == INVOKEINTERFACE;
                            }
                        })
                        .then(returnOpcode)
                )
                .or(paramMatch.zeroOrMore(paramMatch).then(opCode(INVOKESTATIC)).then(returnOpcode))
                .or(match(opCode(INVOKESTATIC)).then(returnOpcode)) //Not expressive enough to match zeroOrMore form the beginning
        );
    }

    static StopMethodMatcher isStaticInitializer() {
        return forNameDesc("<clinit>", "()V");
    }

    static StopMethodMatcher returnsAnEmptyArray() {
        return forBody(
                match(opCode(ICONST_0))
                        .then(opCode(NEWARRAY).or(opCode(ANEWARRAY)))
                        .then(opCode(ARETURN))
        );
    }
    
    static StopMethodMatcher returnsNull() {
        return forBody(
          match(opCode(ACONST_NULL))
          .then(opCode(ARETURN)));
    }





}
