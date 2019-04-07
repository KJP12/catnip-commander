package net.kjp12.commands.utils;

public interface DurianConsumer<I> {
    void accept(I i) throws Exception;
}
