package net.kjp12.commands.utils;

@Deprecated
public interface DurianConsumer<I> {
    void accept(I i) throws Exception;
}
