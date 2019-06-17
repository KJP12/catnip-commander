package net.kjp12.commands.utils;

import com.mewna.catnip.Catnip;
import com.mewna.catnip.entity.message.Embed;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;
import io.reactivex.Single;

import java.util.Objects;

public class WebhookClient {
    public final String TOKEN, ID;
    private transient Catnip catnip;

    public WebhookClient(Catnip catnip, String id, String token) {
        this.catnip = Objects.requireNonNull(catnip, "catnip");
        TOKEN = token;
        ID = id;
    }

    public Single<Message> execute(Catnip catnip, MessageOptions msg) {
        return catnip.rest().webhook().executeWebhook(ID, TOKEN, msg);
    }

    public void setCatnip(Catnip catnip) {
        this.catnip = catnip;
    }

    public Single<Message> send(MessageOptions msg) {
        return catnip.rest().webhook().executeWebhook(ID, TOKEN, msg);
    }

    public Single<Message> send(Message msg) {
        return catnip.rest().webhook().executeWebhook(ID, TOKEN, new MessageOptions(msg));
    }

    public Single<Message> send(Embed embed) {
        return catnip.rest().webhook().executeWebhook(ID, TOKEN, new MessageOptions().embed(embed));
    }
}
