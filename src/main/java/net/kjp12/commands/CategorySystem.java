package net.kjp12.commands;

import com.mewna.catnip.entity.guild.Member;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.util.Permission;
import kotlin.jvm.functions.Function3;
import net.kjp12.commands.abstracts.ICommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public final class CategorySystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(CategorySystem.class);
    private final List<Category> CATEGORIES = new LinkedList<>();
    /**
     * Hidden parent category of every command.
     * All commands will inherit this category and its permission checks.
     */
    public final Category SYSTEM_CATEGORY,
    /**
     * Hidden parent category of deprecated commands.
     * All deprecated commands will inherit this category and fail, unless otherwise set.
     */
    DEPRECATED_CATEGORY = new Category("deprecated", (msg, ac, t) -> {
        if (t) throw new CommandException("This command is deprecated!");
        return false;
    }, false);

    public CategorySystem() {
        SYSTEM_CATEGORY = new Category("system", null, true);
    }

    public CategorySystem(CategorySystem parent) {
        SYSTEM_CATEGORY = new Category("system", parent.SYSTEM_CATEGORY, true);
    }

    public final Category buildCategory(String cat) {
        cat = cat.toLowerCase();
        for (var cate : CATEGORIES) if (cate.NAME.equals(cat)) return cate;
        var c = new Category(cat);
        CATEGORIES.add(c);
        return c;
    }

    /**
     * @param cat   Name of a {@link Category} as {@link String}; If it exists, will use it else create a new one.
     * @param check Permission checks in a form of a {@link Function3}.
     *              Respect the input arguments. Arguments...
     *                  {@link Message} The message that contains the command
     *                  {@link ICommand} The command the message wants to trigger
     *                  {@link Boolean} if true should throw an exception else return false on bad permissions.
     *              The returned {@link Boolean} is if the command is allowed to execute or not.
     *
     * @return The built {@link Category} object
     */
    public final Category buildCategory(String cat, Function3<Message, ICommand, Boolean, Boolean> check) {
        return buildCategory(cat).applyPermCheck(check);
    }

    public final void removeCommand(ICommand command) {
        CATEGORIES.forEach(c -> c.removeCommand(command));
    }

    public final List<Category> getCategories(){
        return Collections.unmodifiableList(CATEGORIES);
    }

    /**
     * The main Category class. Inherits TriFunction for delegation purposes, mainly {@link #SYSTEM_CATEGORY}.
     *
     * This Category class is an independent instance from CategorySystem to avoid a permanently left-behind and unneeded CategorySystem instance.
     * As such, it is up to {@link #buildCategory(String)} to ensure it was properly saved within {@link CategorySystem#CATEGORIES}.
     */
    public static final class Category implements Function3<Message, ICommand, Boolean, Boolean> {
        public final String NAME;
        private final LinkedList<ICommand> COMMANDS;
        private boolean isHidden;
        /**
         * Flags if this object has been finalized. If so, disables {@link #addCommand(ICommand)} which makes this category
         * effectively just another {@link Function3}.
         */
        private volatile boolean finalized;
        private Function3<Message, ICommand, Boolean, Boolean> permissionCheck;

        {
            COMMANDS = new LinkedList<>();
        }

        private Category(String name) {
            LOGGER.debug("[" + (NAME = name) + "] Created");
        }

        private Category(String name, Function3<Message, ICommand, Boolean, Boolean> delegate, boolean hidden) {
            this(name);
            permissionCheck = cyclicInheritanceCheck(delegate);
            isHidden = hidden;
        }

        private Function3<Message, ICommand, Boolean, Boolean> cyclicInheritanceCheck(Function3<Message, ICommand, Boolean, Boolean> delegate) {
            if (delegate == this) throw new IllegalArgumentException("Delegate cannot be self (" + NAME + ")");
            if (delegate instanceof Category) { //initial check to avoid unneeded initialization
                var iterated = new LinkedList<Function3<Message, ICommand, Boolean, Boolean>>();
                iterated.add(delegate);
                var test = ((Category) delegate).permissionCheck;
                while (test instanceof Category) {
                    //this is an expensive call ({@link LinkedList#toString()}), so it is best to leave it under an if-throw
                    if (iterated.contains(test))
                        throw new IllegalArgumentException("Cyclic-inheritance detected down chain " + iterated);
                    iterated.add(test);
                    test = ((Category) test).permissionCheck;
                }
            }
            return delegate;
        }

        /**
         * Applies a permission check. Must be applied via {@link CategorySystem#buildCategory(String, Function3)}, which also accepts {@link Category} as its second parameter.
         *
         * @param check The permission check in the form of a {@link Function3} with the following type parameters...
         *              {@link Message}     Argument; Context as Message; Used for Author as {@link Member} to check {@link Permission}
         *              {@link ICommand}    Argument; Command being checked on
         *              {@link Boolean}     Argument; If it should throw else return false on failed check
         *              {@link Boolean}     Return; If the permission check passes.
         * @return Self for convenience.
         */
        private Category applyPermCheck(Function3<Message, ICommand, Boolean, Boolean> check) {
            LOGGER.debug("[" + NAME + "] Applied check " + (permissionCheck = cyclicInheritanceCheck(check)));
            return this;
        }

        /**
         * Sets if this category should be hidden from public view like {@link net.kjp12.commands.defaults.information.HelpCommand}.
         *
         * @param hid Hide if true.
         * @return Self for convenience.
         */
        public final Category setHidden(boolean hid){
            LOGGER.debug("[" + NAME + "] " + ((isHidden = hid) ? "is now" : "no longer") + " hidden");
            return this;
        }

        /**
         * Internal method for use with {@link ICommand} object initialization.
         * This can be manually forced but the command itself won't register that it had gotten a new category instance to check unless told otherwise.
         * The implementation of the command may call this method on its own like in the case of {@link net.kjp12.commands.abstracts.AbstractCommand(net.kjp12.commands.abstracts.ICommandListener)}
         *
         * @param c An {@link ICommand} object.
         * @return this
         */
        public final Category addCommand(ICommand c) {
            notNull(c, "ICommand");
            if (finalized)
                throw new IllegalStateException("Category " + NAME + " has been finalized; won't accept " + c);
            synchronized (COMMANDS) { //prevents concurrent operation & potentially unwanted operations
                for (var a : COMMANDS) {
                    if (a.equals(c)) {
                        LOGGER.warn("[{}] While loading {}, clashed with {}", NAME, a, c);
                        return this;
                    }
                }
                LOGGER.debug("[{}] Added {}", NAME, c);
                COMMANDS.add(c);
            }
            return this;
        }

        /**
         * Removes an {@link ICommand} instance from the category.
         *
         * @param command Command to remove
         */
        public final void removeCommand(ICommand command) {
            if (COMMANDS.remove(command))
                LOGGER.debug("[{}] Removed {}", NAME, command);
        }

        public final List<ICommand> getCommands() {
            return Collections.unmodifiableList(COMMANDS);
        }

        public final int hashCode() {
            int h = NAME.hashCode();
            h = h * 31 + COMMANDS.hashCode();
            h = h * 31 + Boolean.hashCode(isHidden);
            return h * 31 + permissionCheck.hashCode();
        }

        public final String toString(){
            return NAME;
        }

        public final boolean checkPermission(Message msg, @Nullable ICommand ac, boolean t) {
            return permissionCheck == null ? true : permissionCheck.invoke(msg, ac, t);
        }

        public final boolean isHidden(){
            return isHidden;
        }

        /**
         * Delegating method; passes straight to {@link #permissionCheck} in case this is a parent of another category.
         *
         * @return if {@link #permissionCheck} is null, true else whatever {@link Function3#invoke(Object, Object, Object)} returns.
         * */
        @Override
        public final Boolean invoke(Message message, ICommand iCommand, Boolean throwing) {
            return permissionCheck == null ? true : permissionCheck.invoke(message, iCommand, throwing);
        }

        /**
         * Object has been collected by GC and is soon to be destroyed. Assume that the worst happened and destroy everything.
         * This is also called by {@link CategorySystem#finalize()} in order to signal a shutdown.
         * <p>
         * Currently, only cleans {@link #COMMANDS} and lock down from new commands from being added.
         */
        //Is term for shutdown now, so suppression is applicable.
        @SuppressWarnings("deprecation")
        public void finalize() {
            finalized = true;
            synchronized (COMMANDS) {
                COMMANDS.clear();
            }
        }
    }
}
