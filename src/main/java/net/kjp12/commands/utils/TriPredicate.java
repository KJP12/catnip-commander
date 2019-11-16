package net.kjp12.commands.utils;

public interface TriPredicate<I1, I2, I3> {
    boolean invoke(I1 i1, I2 i2, I3 i3);
}
