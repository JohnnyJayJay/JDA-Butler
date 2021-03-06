package com.almightyalpaca.discord.jdabutler.commands.commands;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.commands.Command;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class ShutdownCommand extends Command
{

    private static final String[] ALIASES = new String[] { "reboot" };

    @Override
    public void dispatch(final User sender, final TextChannel channel, final Message message, final String content, final GuildMessageReceivedEvent event)
    {
        if (!Bot.isAdmin(sender))
        {
            this.sendFailed(message);
            return;
        }

        Bot.shutdown();
    }

    @Override
    public String[] getAliases()
    {
        return ShutdownCommand.ALIASES;
    }

    @Override
    public String getHelp()
    {
        return null;
    }

    @Override
    public String getName()
    {
        return "shutdown";
    }
}
