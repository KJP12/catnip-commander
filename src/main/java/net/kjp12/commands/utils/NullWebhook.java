package net.kjp12.commands.utils;

import com.mewna.catnip.Catnip;
import com.mewna.catnip.entity.channel.Webhook;
import com.mewna.catnip.entity.message.Message;
import com.mewna.catnip.entity.message.MessageOptions;
import com.mewna.catnip.entity.user.User;
import io.reactivex.Single;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A helper utility to act as a webhook when there isn't a webhook available.
 */
public class NullWebhook implements Webhook {
    public static final NullWebhook theHook = new NullWebhook();

    private NullWebhook() {
    }

    @Nonnull
    public final Single<Message> executeWebhook(@Nonnull final MessageOptions options,
                                                @Nullable final String username, @Nullable final String avatarUrl) {
        return Single.error(new Throwable("Webhook was never set!"));
    }

    @Override
    public long channelIdAsLong() {
        throw new UnsupportedOperationException("Null Webhook Impl");
    }

    @Nonnull
    @Override
    public User user() {
        throw new UnsupportedOperationException("Null Webhook Impl");
    }

    @Nullable
    @Override
    public String name() {
        return null;
    }

    @Nullable
    @Override
    public String avatar() {
        return null;
    }

    @Nonnull
    @Override
    public String token() {
        throw new UnsupportedOperationException("Null Webhook Impl");
    }

    @Override
    public long idAsLong() {
        throw new UnsupportedOperationException("Null Webhook Impl");
    }

    @Override
    public long guildIdAsLong() {
        throw new UnsupportedOperationException("Null Webhook Impl");
    }

    @Override
    public Catnip catnip() {
        throw new UnsupportedOperationException("Null Webhook Impl");
    }
}
