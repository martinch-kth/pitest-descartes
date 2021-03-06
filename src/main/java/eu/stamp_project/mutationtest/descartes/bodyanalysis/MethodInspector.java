package eu.stamp_project.mutationtest.descartes.bodyanalysis;

import eu.stamp_project.mutationtest.descartes.MutationPointFinder;
import org.pitest.reloc.asm.Label;
import org.pitest.reloc.asm.MethodVisitor;
import org.pitest.reloc.asm.Opcodes;
import org.pitest.reloc.asm.commons.Method;

public class MethodInspector extends MethodVisitor {

    private Method method;
    private MutationPointFinder finder;
    private LineCounter lineCounter;

    public MethodInspector(Method method, MutationPointFinder finder) {
        super(Opcodes.ASM5);

        this.method = method;
        this.finder = finder;
        lineCounter = new LineCounter();

    }

    @Override
    public void visitEnd() {
        finder.registerMutations(method, lineCounter.getFirstLine(), lineCounter.getLastLine());
    }

    @Override
    public void	visitLineNumber(int line, Label start) {
        lineCounter.registerLine(line);
    }

}
