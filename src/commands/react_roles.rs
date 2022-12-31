use std::time::Duration;

use anyhow::Result;
use poise::serenity_prelude::{CacheHttp, Emoji, Message, ReactionType, Role};

use crate::message_link;
use crate::traits::as_i64::AsInt64;
use crate::utils::paginators::EmbedPaginator;
use crate::{database::entities::react_roles::ReactRole, prelude::Context};

#[poise::command(
    slash_command,
    subcommands("new", "list", "find_by_id", "remove"),
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

    ReactRole::create(
        pool,
        &guild_id,
        &channel_id,
        &message_id,
        &role_id,
        &emoji_id,
    )
    .await?;

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

#[poise::command(slash_command, required_permissions = "MANAGE_ROLES")]
pub async fn list(ctx: Context<'_>) -> Result<()> {
    let pool = &ctx.data().pool;

    let guild_id = ctx.guild_id().unwrap().as_i64()?;
    let react_roles: Vec<ReactRole> =
        sqlx::query_as(r#"SELECT * FROM "public"."react_roles" AS "t" WHERE "t"."guild_id" = $1;"#)
            .bind(guild_id)
            .fetch_all(pool)
            .await?;

    let pages: Vec<String> = react_roles
        .iter()
        .map(|rr| {
            let message_link = message_link!(guild_id, rr.channel_id, rr.message_id);

            let line = format!(
                "**❱ ID:** {}\n**❱ Role:** <@&{}>\n**❱ Message:** [Jump URL]({message_link})",
                rr.id, rr.role_id
            );

            line
        })
        .collect();

    let paginator = EmbedPaginator::builder()
        .timeout(Duration::from_secs(180))
        .title(format!("React roles in {}", ctx.guild().unwrap().name))
        .pages(pages)
        .build();

    paginator.start(&ctx, true).await?;

    Ok(())
}

#[poise::command(
    slash_command,
    rename = "find-by-id",
    required_permissions = "MANAGE_ROLES"
)]
pub async fn find_by_id(ctx: Context<'_>, message: Message) -> Result<()> {
    ctx.defer_ephemeral().await?;

    let pool = &ctx.data().pool;

    let message_id = message.id.as_i64()?;
    let guild_id = ctx.guild_id().unwrap().as_i64()?;

    let react_role: Option<ReactRole> = sqlx::query_as(
        r#"SELECT * FROM "public"."react_roles" AS "t" WHERE "t"."message_id" = $1 AND "t"."guild_id" = $2 LIMIT 1;"#,
    )
    .bind(message_id)
    .bind(guild_id)
    .fetch_optional(pool)
    .await?;

    if react_role.is_none() {
        ctx.say(":x: Couldn't find any role associated with the provided message!")
            .await?;

        return Ok(());
    }

    let react_role = react_role.unwrap();
    let message_link = message_link!(
        react_role.guild_id,
        react_role.channel_id,
        react_role.message_id
    );

    ctx.send(|r| {
        r.embed(|e| {
            e.title("React Role Info").description(format!(
                "**❱ ID:** {}\n**❱ Role:** <@&{}>\n**❱ Message:** [Jump URL]({message_link})",
                react_role.id, react_role.role_id
            ))
        })
    })
    .await?;

    Ok(())
}

#[poise::command(
    slash_command,
    rename = "remove",
    required_permissions = "MANAGE_ROLES"
)]
pub async fn remove(ctx: Context<'_>, message: Message) -> Result<()> {
    ctx.defer_ephemeral().await?;

    let pool = &ctx.data().pool;

    let guild_id = ctx.guild_id().unwrap().as_i64()?;
    let message_id = message.id.as_i64()?;

    let deleted: Option<ReactRole> = sqlx::query_as(
        r#"DELETE
    FROM "public"."react_roles" AS "t"
    WHERE "t"."guild_id" = $1
      AND "t"."message_id" = $2
    RETURNING *;"#,
    )
    .bind(guild_id)
    .bind(message_id)
    .fetch_optional(pool)
    .await?;

    if deleted.is_none() {
        ctx.say(":warning: Couldn't find such reaction role, nothing was changed.")
            .await?;

        return Ok(());
    }

    ctx.send(|r| {
        r.allowed_mentions(|a| a.empty_parse())
            .content(":+1: Successfully removed react role!")
    })
    .await?;

    Ok(())
}
