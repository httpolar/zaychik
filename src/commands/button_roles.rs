use std::fmt::Display;
use std::time::Duration;

use anyhow::{bail, Error, Result};
use poise::serenity_prelude::{
    ButtonStyle, CacheHttp, Channel, Colour, CreateComponents, Message, Role,
};

use crate::constans::{BUTTON_ROLE_ADD_PREFIX, BUTTON_ROLE_RMV_PREFIX};
use crate::database::entities::button_roles::ButtonRole;
use crate::message_link;
use crate::prelude::Context;
use crate::traits::as_i64::AsInt64;
use crate::traits::bot_emotes::BotEmotes;
use crate::utils::paginators::EmbedPaginator;

fn create_button_role_components<D: Display>(
    uuid: &uuid::Uuid,
    acquire_label: &D,
    withdraw_label: &D,
) -> CreateComponents {
    let mut components = CreateComponents::default();

    components.create_action_row(|act_row| {
        act_row
            .create_button(|btn| {
                btn.custom_id(format!("{BUTTON_ROLE_ADD_PREFIX}:{uuid}"))
                    .style(ButtonStyle::Secondary)
                    .label(acquire_label)
            })
            .create_button(|btn| {
                btn.custom_id(format!("{BUTTON_ROLE_RMV_PREFIX}:{uuid}"))
                    .style(ButtonStyle::Secondary)
                    .label(withdraw_label)
            })
    });

    components
}

#[poise::command(
    slash_command,
    guild_only = true,
    rename = "button-role",
    subcommands("new", "remove", "list"),
    required_permissions = "MANAGE_ROLES"
)]
pub async fn buttonrole(_ctx: Context<'_>, _arg: String) -> Result<()> {
    Ok(())
}

#[allow(clippy::too_many_arguments)]
#[poise::command(
    slash_command,
    guild_only = true,
    required_permissions = "MANAGE_ROLES"
)]
pub async fn new(
    ctx: Context<'_>,
    #[description = "Role that will be given or removed on button clicks"] role: Role,
    #[description = "Channel where the message will be posted"] channel: Option<Channel>,
    #[description = "Contents of the message that will be sent"] message: Option<String>,
    #[description = "Text of the button that gives the role"] acquire_btn_label: Option<String>,
    #[description = "Text of the button that removes the role"] withdraw_btn_label: Option<String>,
    #[description = "If you want to send the buttons message as embed"] as_embed: Option<bool>,
    #[description = "You can set title of the embed"] embed_title: Option<String>,
) -> Result<()> {
    let guild = ctx
        .guild()
        .ok_or_else(|| Error::msg("Guild is not present somehow :thinking:"))?;

    ctx.defer_ephemeral().await?;

    let channel_id = channel.map(|c| c.id()).unwrap_or_else(|| ctx.channel_id());
    let acquire_btn_label = acquire_btn_label.unwrap_or_else(|| "Acquire".to_string());
    let withdraw_btn_label = withdraw_btn_label.unwrap_or_else(|| "Withdraw".to_string());

    let message = message
        .unwrap_or_else(|| format!("Click the buttons to acquire or withdraw the {role} role!"));

    let pool = &ctx.data().pool;
    let mut tx = pool.begin().await?;

    let button_role = sqlx::query_as::<_, ButtonRole>(
        r#"INSERT INTO "public"."button_roles" ("guild_id", "channel_id", "role_id")
        VALUES ($1, $2, $3)
        RETURNING *;"#,
    )
    .bind(guild.id.as_i64()?)
    .bind(channel_id.as_i64()?)
    .bind(role.id.as_i64()?)
    .fetch_one(&mut tx)
    .await?;

    let components =
        create_button_role_components(&button_role.id, &acquire_btn_label, &withdraw_btn_label);

    let reply = channel_id
        .send_message(ctx.http(), |r| {
            if let Some(true) = as_embed {
                r.embed(|e| {
                    if let Some(title) = embed_title {
                        e.title(title);
                    }

                    e.description(message).colour(Colour(0x4F545C))
                });
            } else {
                r.content(message);
            };

            r.set_components(components)
                .allowed_mentions(|am| am.empty_roles().empty_users())
        })
        .await;

    match reply {
        Err(e) => {
            tx.rollback().await?;
            bail!(e);
        }

        Ok(reply_message) => {
            sqlx::query(
                r#"UPDATE "public"."button_roles" SET "message_id" = $1 WHERE "id" = $2 RETURNING *;"#,
            )
            .bind(reply_message.id.as_i64()?)
            .bind(button_role.id)
            .execute(&mut tx)
            .await?;

            tx.commit().await?;

            let desc = format!("{} Button Role successfully created!", ctx.emote_success());

            ctx.send(|r| {
                r.embed(|e| {
                    e.colour(Colour::DARK_GREEN)
                        .title("Success")
                        .description(desc)
                })
            })
            .await?;
        }
    }

    Ok(())
}

#[poise::command(
    slash_command,
    guild_only = true,
    required_permissions = "MANAGE_ROLES"
)]
pub async fn remove(
    ctx: Context<'_>,
    message_link: Option<Message>,
    uuid: Option<String>,
    force: Option<bool>,
) -> Result<()> {
    ctx.defer_ephemeral().await?;

    if uuid.is_none() && message_link.is_none() {
        let desc = format!(
            "{} You must provide at least one of the following arguments: `uuid`, `message_link`",
            ctx.emote_failure()
        );

        ctx.send(|r| {
            r.embed(|e| {
                e.colour(Colour::RED)
                    .title("Bad Command Arguments")
                    .description(desc)
            })
        })
        .await?;

        return Ok(());
    }

    let force = force.unwrap_or(false);
    let guild = ctx.guild().ok_or_else(|| {
        Error::msg("Guild is not present somehow during guild only command execution")
    })?;

    let pool = &ctx.data().pool;

    let maybe_message_id = message_link.map(|ml| ml.id.as_i64().unwrap());
    let maybe_uuid = uuid.map(|u| uuid::Uuid::try_parse(&u).unwrap());

    let to_delete: Option<ButtonRole> = sqlx::query_as(
        r#"SELECT *
        FROM "public"."button_roles" AS "t"
        WHERE "t"."guild_id" = $1
          AND ("t"."message_id" = $2 OR "t"."id" = $3);"#,
    )
    .bind(guild.id.as_i64()?)
    .bind(maybe_message_id)
    .bind(maybe_uuid)
    .fetch_optional(pool)
    .await?;

    if to_delete.is_none() {
        ctx.say(":x: Button role not found").await?;
        return Ok(());
    }

    let to_delete = to_delete.unwrap();

    if !force {
        let button_role_message = ctx
            .http()
            .get_message(to_delete.channel_id()?.0, to_delete.message_id()?.0)
            .await?;

        #[allow(clippy::redundant_pattern_matching)]
        if let Err(_) = button_role_message.delete(ctx.http()).await {
            let link = message_link!(
                to_delete.guild_id,
                to_delete.channel_id,
                button_role_message.id.0
            );

            let desc = format!(":warning: We tried to remove the original message with buttons but couldn't. Please, make sure the bot has correct permissions and delete the [message]({link}) yourself!");

            ctx.send(|r| r.embed(|e| e.colour(Colour::GOLD).title("Warning").description(desc)))
                .await?;
        };
    }

    sqlx::query(
        r#"DELETE
        FROM "public"."button_roles" AS "t"
        WHERE "t"."id" = $1
        RETURNING *;"#,
    )
    .bind(to_delete.id)
    .execute(pool)
    .await?;

    let desc = format!(
        "{} Button Role entry has been removed from the bot's database!",
        ctx.emote_success()
    );

    ctx.send(|r| {
        r.embed(|e| {
            e.colour(Colour::DARK_GREEN)
                .title("Success")
                .description(desc)
        })
    })
    .await?;

    Ok(())
}

#[poise::command(
    slash_command,
    guild_only = true,
    required_permissions = "MANAGE_ROLES"
)]
pub async fn list(ctx: Context<'_>) -> Result<()> {
    let guild = ctx.guild().ok_or_else(|| {
        Error::msg("Guild is not present on guild only command somehow :thinking:")
    })?;

    let pool = &ctx.data().pool;

    let button_roles = sqlx::query_as::<_, ButtonRole>(
        r#"SELECT *
        FROM "public"."button_roles" AS "t"
        WHERE "t"."guild_id" = $1;"#,
    )
    .bind(guild.id.as_i64()?)
    .fetch_all(pool)
    .await?;

    let pages: Vec<String> = button_roles
        .iter()
        .map(|br| {
            let mut page = format!(
                "**❱ UUID:** `{}`\n**❱ Role:** <@&{}>\n**❱ Message:** ",
                br.id, br.role_id
            );

            match br.message_id {
                Some(message_id) => {
                    let link = message_link!(br.guild_id, br.channel_id, message_id);
                    page.push_str(&format!("[Jump URL]({link})"));
                }
                None => {
                    page.push_str("message id is missing for this entry");
                }
            }

            page
        })
        .collect();

    let paginator = EmbedPaginator::builder()
        .colour(Colour::BLURPLE)
        .title("Button Roles")
        .pages(pages)
        .timeout(Duration::from_secs(180))
        .build();

    paginator.start(&ctx, true).await?;

    Ok(())
}
