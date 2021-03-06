package com.kantenkugel.discordbot.versioncheck.items;

import com.almightyalpaca.discord.jdabutler.Bot;
import com.almightyalpaca.discord.jdabutler.util.EmbedUtil;
import com.almightyalpaca.discord.jdabutler.util.FormattingUtil;
import com.almightyalpaca.discord.jdabutler.util.MiscUtils;
import com.almightyalpaca.discord.jdabutler.util.gradle.GradleProjectDropboxUtil;
import com.kantenkugel.discordbot.jdocparser.JDoc;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsApi;
import com.kantenkugel.discordbot.jenkinsutil.JenkinsBuild;
import com.kantenkugel.discordbot.versioncheck.*;
import com.kantenkugel.discordbot.versioncheck.changelog.ChangelogProvider;
import com.kantenkugel.discordbot.versioncheck.changelog.JenkinsChangelogProvider;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

public class JDAItem extends VersionedItem implements UpdateHandler
{
    private final ChangelogProvider changelogProvider = new JenkinsChangelogProvider(JenkinsApi.JDA_JENKINS, "https://github.com/DV8FromTheWorld/JDA/");

    private final Supplier<String> versionSupplier = new JenkinsVersionSupplier(JenkinsApi.JDA_JENKINS);

    @Override
    public Supplier<String> getCustomVersionSupplier() {
        return versionSupplier;
    }

    @Override
    public String getName()
    {
        return "JDA";
    }

    @Override
    public RepoType getRepoType()
    {
        return RepoType.JCENTER;
    }

    @Override
    public String getGroupId()
    {
        return "net.dv8tion";
    }

    @Override
    public String getArtifactId()
    {
        return "JDA";
    }

    @Override
    public String getUrl()
    {
        return JenkinsApi.JDA_JENKINS.getLastSuccessfulBuildUrl();
    }

    @Override
    public long getAnnouncementRoleId()
    {
        return 241948671325765632L;
    }

    @Override
    public long getAnnouncementChannelId()
    {
        return 125227483518861312L;
    }

    @Override
    public UpdateHandler getUpdateHandler()
    {
        return this;
    }

    @Override
    public ChangelogProvider getChangelogProvider()
    {
        return changelogProvider;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onUpdate(VersionedItem item, String previousVersion, boolean shouldAnnounce)
    {
        VersionUtils.VersionSplits versionSplits = item.parseVersion();
        if (versionSplits.build != Bot.config.getInt("jda.version.build", -1))
        {
            Bot.LOG.debug("Update found!");

            Bot.config.put("jda.version.build", versionSplits.build);
            Bot.config.put("jda.version.name", item.getVersion());

            Bot.config.save();

            JenkinsBuild jenkinsBuild;

            try
            {
                jenkinsBuild = JenkinsApi.JDA_JENKINS.fetchLastSuccessfulBuild();
            }
            catch(IOException ex)
            {
                Bot.LOG.warn("Could not fetch latest Jenkins build in JDAItem#onUpdate()", ex);
                return;
            }

            if(jenkinsBuild == null)
            {
                Bot.LOG.warn("Could not fetch Jenkins-build for new version (triggered by maven update)");
                return;
            }

            Bot.EXECUTOR.submit(() ->
            {
                JDoc.reFetch();
                GradleProjectDropboxUtil.uploadProject();
            });

            if(!shouldAnnounce)
                return;

            final EmbedBuilder eb = new EmbedBuilder();

            final MessageBuilder mb = new MessageBuilder();

            FormattingUtil.setFooter(eb, jenkinsBuild.culprits, jenkinsBuild.buildTime);

            Role announcementRole = getAnnouncementRole();

            mb.append(announcementRole.getAsMention());

            eb.setAuthor("JDA 3 version " + item.getVersion() + " has been released\n", JenkinsApi.JDA_JENKINS.jenkinsBase + versionSplits.build, EmbedUtil.getJDAIconUrl());

            EmbedUtil.setColor(eb);

            if (jenkinsBuild.changes.size() > 0)
            {

                eb.setTitle(EmbedBuilder.ZERO_WIDTH_SPACE, null);
                ChangelogProvider.Changelog changelog = getChangelogProvider().getChangelog(Integer.toString(jenkinsBuild.buildNum));
                List<String> changeset = changelog.getChangeset();

                int fields;

                if (changeset.size() > 25)
                    fields = 24;
                else
                    fields = Math.min(changeset.size(), 25);

                for (int j = 0; j < fields; j++)
                {
                    final String field = changeset.get(j);
                    eb.addField(j == 0 ? "Commits:" : "", field, false);
                }

                if (changeset.size() > 25)
                    eb.addField("", "max embed length reached", false);

            }

            final MessageEmbed embed = eb.build();

            mb.setEmbed(embed);

            final TextChannel channel = getAnnouncementChannel();

            MiscUtils.announce(channel, announcementRole, mb.build(), true);
        }
    }
}
