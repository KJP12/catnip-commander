package net.kjp12.commands;

import com.mewna.catnip.entity.channel.Webhook;
import com.mewna.catnip.entity.guild.Guild;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.internal.operators.single.SingleJust;
import net.kjp12.commands.abstracts.AbstractCommandListener;
import net.kjp12.commands.abstracts.AbstractSubSystemCommand;
import net.kjp12.commands.abstracts.ICommand;
import net.kjp12.commands.abstracts.ICommandListener;
import net.kjp12.commands.defaults.information.CatnipInfoCommand;
import net.kjp12.commands.defaults.information.HelpCommand;
import net.kjp12.commands.defaults.information.PingCommand;
import net.kjp12.commands.defaults.owner.DumpCommand;
import net.kjp12.commands.defaults.owner.EvaluatorCommand;
import net.kjp12.commands.defaults.owner.GetInviteCommand;
import net.kjp12.commands.defaults.owner.ProcessCommand;
import net.kjp12.commands.impl.*;
import net.kjp12.commands.utils.NullWebhook;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;

public class CommandListenerTest {
    final String thePrefix = "mockingBird!";
    final Webhook theWebhook = Mockito.mock(Webhook.class);
    final ICommandListener theListener = new AbstractCommandListener((ExecutorService) null) {
        @Override
        public String getPrefix(Guild guild) {
            return thePrefix;
        }
    };
    final AbstractSubSystemCommand theSubSystem = new SubSysImpl(theListener),
            theProcess = new ProcessCommand(theListener, false);
    final ICommand theDump = new DumpCommand(theListener),
            theHelp = new HelpCommand(theListener),
            theInfo = new CatnipInfoCommand(theListener),
            thePing = new PingCommand(theListener),
            theInvite = new GetInviteCommand(theListener),
            theEvaluator = new EvaluatorCommand(theListener, EvaluatorCommand.SEM.getEngineByName("groovy")),
            theUserPerm = new IUserPermCmdImpl(theListener),
            theBotPerm = new IBotPermCmdImpl(theListener),
            theView = new IViewImpl(theListener),
            theCommand = new ICmdImpl(theListener),
            ssiHelp = new HelpCommand(theSubSystem),
            ssiUserPerm = new IUserPermCmdImpl(theSubSystem),
            ssiBotPerm = new IBotPermCmdImpl(theSubSystem),
            ssiView = new IViewImpl(theSubSystem),
            ssiCommand = new ICmdImpl(theSubSystem),
            pcmHelp = new HelpCommand(theProcess, "Process Commands"),
            pcmKill = new ProcessCommand.ProcessKill(theProcess),
            pcmList = new ProcessCommand.ProcessList(theProcess),
            pcmExecute = new ProcessCommand.ProcessExecute(theProcess);
    final ICommand[] icl = {theDump, theHelp, theInfo, thePing, theInvite, theProcess, theEvaluator, theUserPerm, theBotPerm, theView, theCommand, theSubSystem},
            ssi = {ssiHelp, ssiUserPerm, ssiBotPerm, ssiView, ssiCommand},
            pcm = {pcmHelp, pcmKill, pcmList, pcmExecute};
    final ICommandListener[] ici = {theListener, theSubSystem, theProcess};

    {
        theSubSystem.setDefaultCommand(ssiHelp);
        theProcess.setDefaultCommand(pcmList);
    }

    @TestFactory
    public Iterable<DynamicNode> generateTests() {
        var tests = new ArrayList<DynamicNode>((icl.length + ssi.length + pcm.length) * 9);
        for (var c : icl) {
            helpGenCatAliasTest(tests, "ICL", theListener, c);
            tests.add(DynamicTest.dynamicTest("Expect Fail ICL -> SSI " + c + '(' + c.getClass() + ')', () -> assertThrows(IllegalArgumentException.class, () -> theSubSystem.setDefaultCommand(c))));
            tests.add(DynamicTest.dynamicTest("Expect Fail ICL -> PCM " + c + '(' + c.getClass() + ')', () -> assertThrows(IllegalArgumentException.class, () -> theProcess.setDefaultCommand(c))));
        }
        for (var c : ssi) {
            helpGenCatAliasTest(tests, "SSI", theSubSystem, c);
            tests.add(DynamicTest.dynamicTest("Expect Pass SSI -> SSI " + c + '(' + c.getClass() + ')', () -> assertDoesNotThrow(() -> theSubSystem.setDefaultCommand(c))));
            tests.add(DynamicTest.dynamicTest("Expect Fail SSI -> PCM " + c + '(' + c.getClass() + ')', () -> assertThrows(IllegalArgumentException.class, () -> theProcess.setDefaultCommand(c))));
        }
        for (var c : pcm) {
            helpGenCatAliasTest(tests, "PCM", theProcess, c);
            tests.add(DynamicTest.dynamicTest("Expect Fail PCM -> SSI " + c + '(' + c.getClass() + ')', () -> assertThrows(IllegalArgumentException.class, () -> theSubSystem.setDefaultCommand(c))));
            tests.add(DynamicTest.dynamicTest("Expect Pass PCM -> PCM " + c + '(' + c.getClass() + ')', () -> assertDoesNotThrow(() -> theProcess.setDefaultCommand(c))));
        }
        return tests;
    }

    void helpGenCatAliasTest(Collection<DynamicNode> tests, String f, ICommandListener icl, ICommand c) {
        tests.add(DynamicTest.dynamicTest("Command Listener - " + f + ' ' + c + '(' + c.getClass() + ')', () -> assertEquals(icl, c.getListener())));
        tests.add(DynamicTest.dynamicTest("Normal Alias - " + f + ' ' + c + '(' + c.getClass() + ')', () -> {
            for (var s : c.toAliases()) assertEquals(c, icl.getCommand(s), () -> s + " -> " + c);
        }));
        tests.add(DynamicTest.dynamicTest("Lower Alias - " + f + ' ' + c + '(' + c.getClass() + ')', () -> {
            for (var s : c.toAliases()) assertEquals(c, icl.getCommand(s.toLowerCase()), () -> s + " -> " + c);
        }));
        tests.add(DynamicTest.dynamicTest("Upper Alias - " + f + ' ' + c + '(' + c.getClass() + ')', () -> {
            for (var s : c.toAliases()) assertEquals(c, icl.getCommand(s.toUpperCase()), () -> s + " -> " + c);
        }));
        var cs = icl.getCategorySystem();
        var cl = c.getCategoryList();
        tests.add(DynamicTest.dynamicTest("Category Self-Containment - " + f + ' ' + c + '(' + c.getClass() + ')', () -> {
            for (var cat : cl) assertTrue(cat.getCommands().contains(c), cat::toString);
        }));
        tests.add(DynamicTest.dynamicTest("Category Sanity Check - " + f + ' ' + c + '(' + c.getClass() + ')', () -> {
            for (var cat : c.toCategories()) {
                var cat2 = cs.buildCategory(cat);
                assertNotNull(cat2, () -> "Category " + cat + " doesn't exist or wasn't built?!");
                assertTrue(cl.contains(cat2), () -> cat + " -> " + cat2);
            }
        }));
        tests.add(DynamicTest.dynamicTest("Category Unexpected-Adding Check - " + f + ' ' + c + '(' + c.getClass() + ')', () -> {
            a:
            for (var cat : cs.getCategories()) {
                for (var cat2 : cl) if (cat == cat2) continue a;
                assertFalse(cat.getCommands().contains(c), cat::toString);
            }
        }));
    }

    @TestFactory
    public Iterable<DynamicNode> testWebhooks() {
        var tests = new ArrayList<DynamicNode>((ici.length) * 8);
        for (var c : ici) {
            tests.add(DynamicTest.dynamicTest("Expect Default -> NullWebhook " + c + '(' + c.getClass() + ')', () -> assertEquals(NullWebhook.theHook, c.getWebhook())));
            tests.add(DynamicTest.dynamicTest("Expect (Webhook)null -> NullWebhook " + c + '(' + c.getClass() + ')', () -> {
                var tl = (AbstractCommandListener) theListener;
                tl.setWebhook(theWebhook);
                assertEquals(theWebhook, tl.getWebhook(), "theListener#getWebhook() is not setting to theWebhook, invalid");
                tl.setWebhook((Webhook) null);
                assertEquals(NullWebhook.theHook, c.getWebhook(), c.getClass() + "#getWebhook()");
            }));
            tests.add(DynamicTest.dynamicTest("Expect (Single<Webhook>)null -> NullWebhook " + c + '(' + c.getClass() + ')', () -> {
                var tl = (AbstractCommandListener) theListener;
                tl.setWebhook(theWebhook);
                assertEquals(theWebhook, tl.getWebhook(), "theListener#getWebhook() is not setting to theWebhook, invalid");
                tl.setWebhook((Single<Webhook>) null);
                assertEquals(NullWebhook.theHook, c.getWebhook(), c.getClass() + "#getWebhook()");
            }));
            // Note: The following test set is illegal, as Single.just() does not allow null. However, we're mitigating null risk, so testing anyways.
            tests.add(DynamicTest.dynamicTest("Expect Single<Webhook>(null) -> NullWebhook " + c + '(' + c.getClass() + ')', () -> {
                var tl = (AbstractCommandListener) theListener;
                tl.setWebhook(theWebhook);
                assertEquals(theWebhook, tl.getWebhook(), "theListener#getWebhook() is not setting to theWebhook, invalid");
                tl.setWebhook(new SingleJust<>(null));
                assertEquals(NullWebhook.theHook, c.getWebhook(), c.getClass() + "#getWebhook()");
            }));
            tests.add(DynamicTest.dynamicTest("Expect Single.error() -> theWebhook (onError doesn't reset.)" + c + '(' + c.getClass() + ')', () -> {
                var tl = (AbstractCommandListener) theListener;
                tl.setWebhook(theWebhook);
                assertEquals(theWebhook, tl.getWebhook(), "theListener#getWebhook() is not setting to theWebhook, invalid");
                tl.setWebhook(Single.error(new Throwable("Test Throwable")));
                assertEquals(theWebhook, c.getWebhook(), c.getClass() + "#getWebhook()");
            }));
            tests.add(DynamicTest.dynamicTest("Expect Webhook -> Webhook" + c + '(' + c.getClass() + ')', () -> {
                var tl = (AbstractCommandListener) theListener;
                tl.setWebhook(NullWebhook.theHook);
                assertEquals(NullWebhook.theHook, tl.getWebhook(), "theListener#getWebhook() is not setting to NullWebhook, invalid");
                tl.setWebhook(theWebhook);
                assertEquals(theWebhook, c.getWebhook(), c.getClass() + "#getWebhook()");
            }));
            tests.add(DynamicTest.dynamicTest("Expect Single<Webhook> -> NullWebhook " + c + '(' + c.getClass() + ')', () -> {
                var tl = (AbstractCommandListener) theListener;
                tl.setWebhook(NullWebhook.theHook);
                assertEquals(NullWebhook.theHook, tl.getWebhook(), "theListener#getWebhook() is not setting to NullWebhook, invalid");
                tl.setWebhook(Single.just(theWebhook));
                assertEquals(theWebhook, c.getWebhook(), c.getClass() + "#getWebhook()");
            }));
            tests.add(DynamicTest.dynamicTest("Expect Single.error() -> NullWebhook (onError doesn't reset.)" + c + '(' + c.getClass() + ')', () -> {
                var tl = (AbstractCommandListener) theListener;
                tl.setWebhook(NullWebhook.theHook);
                assertEquals(NullWebhook.theHook, tl.getWebhook(), "theListener#getWebhook() is not setting to theWebhook, invalid");
                tl.setWebhook(Single.error(new Throwable("Test Throwable")));
                assertEquals(NullWebhook.theHook, c.getWebhook(), c.getClass() + "#getWebhook()");
            }));
        }
        return tests;
    }
}
