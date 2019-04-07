package net.kjp12.commands.utils;

import com.mewna.catnip.Catnip;
import com.mewna.catnip.entity.message.Embed;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

public class WebhookClient {
    public final String TOKEN, ID;
    private transient Catnip catnip;

    public WebhookClient(Catnip catnip, String id, String token) {
        this.catnip = Objects.requireNonNull(catnip, "catnip");
        TOKEN = token;
        ID = id;
    }

    public CompletionStage<Message> execute(Catnip catnip, MessageOptions msg) {
        return catnip.rest().webhook().executeWebhook(ID, TOKEN, msg);
    }

    public void setCatnip(Catnip catnip) {
        this.catnip = catnip;
    }

    public CompletionStage<Message> send(MessageOptions msg) {
        return catnip.rest().webhook().executeWebhook(ID, TOKEN, msg);
    }

    public CompletionStage<Message> send(Message msg) {
        return catnip.rest().webhook().executeWebhook(ID, TOKEN, new MessageOptions(msg));
    }

    public CompletionStage<Message> send(Embed embed) {
        return catnip.rest().webhook().executeWebhook(ID, TOKEN, new MessageOptions().embed(embed));
    }
}
