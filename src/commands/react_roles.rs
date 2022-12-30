use anyhow::Result;
use poise::serenity_prelude::{CacheHttp, Emoji, Message, ReactionType, Role};

use crate::{prelude::Context, database::entities::react_roles::ReactRole};

#[poise::command(
    slash_command,
    subcommands("new"),
    required_permissions = "MANAGE_ROLES"
)]
pub async fn reactrole(_ctx: Context<'_>, _arg: String) -> Result<()> {
    Ok(())
}

#[poise::command(slash_command, required_permissions = "MANAGE_ROLES")]
pub async fn new(
    ctx: Context<'_>,
    #[description = "Jump URL to the message or it's ID that will contain a reaction role"]
    message: Message,
    role: Role,
    emoji: Emoji,
) -> Result<()> {
    ctx.defer_ephemeral().await?;

    let guild_id = i64::try_from(*ctx.guild_id().unwrap().as_u64())?;
    let channel_id = i64::try_from(*message.channel_id.as_u64())?;
    let message_id = i64::try_from(*message.id.as_u64())?;
    let role_id = i64::try_from(*role.id.as_u64())?;
    let emoji_id = i64::try_from(*emoji.id.as_u64())?;

    // try addign the reaction
    message
        .react(ctx.http(), ReactionType::from(emoji.clone()))
        .await?;

    let pool = &ctx.data().pool;

    ReactRole::create(pool, &guild_id, &channel_id, &message_id, &role_id, &emoji_id).await?;

    ctx.send(|r| {
        r.embed(|e| {
            e.colour(role.colour)
                .title("Reaction Role Created")
                .description(format!(
                    "You have successfully created {emoji} reaction role [here]({}) for {role}",
                    message.link()
                ))
        })
    })
    .await?;

    Ok(())
}
