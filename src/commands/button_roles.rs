use anyhow::{bail, Error, Result};
use poise::serenity_prelude::{ButtonStyle, CacheHttp, Channel, Role};

use crate::constans::{BUTTON_ROLE_ADD_PREFIX, BUTTON_ROLE_RMV_PREFIX};
use crate::database::entities::button_roles::ButtonRole;
use crate::prelude::Context;
use crate::traits::as_i64::AsInt64;

#[poise::command(
    slash_command,
    guild_only = true,
    rename = "button-role",
    subcommands("new"),
    required_permissions = "MANAGE_ROLES"
)]
pub async fn buttonrole(_ctx: Context<'_>, _arg: String) -> Result<()> {
    Ok(())
}

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

    let reply = channel_id
        .send_message(ctx.http(), |r| {
            r.components(|c| {
                c.create_action_row(|act_row| {
                    act_row
                        .create_button(|btn| {
                            btn.custom_id(format!("{BUTTON_ROLE_ADD_PREFIX}:{}", button_role.id))
                                .style(ButtonStyle::Primary)
                                .label(&acquire_btn_label)
                        })
                        .create_button(|btn| {
                            btn.custom_id(format!("{BUTTON_ROLE_RMV_PREFIX}:{}", button_role.id))
                                .style(ButtonStyle::Secondary)
                                .label(&withdraw_btn_label)
                        })
                })
            })
            .content(message)
            .allowed_mentions(|am| am.empty_roles().empty_users())
        })
        .await;

    if let Err(e) = reply {
        tx.rollback().await?;
        bail!(e);
    }

    tx.commit().await?;

    ctx.say(":+1: Successfully created a button role at designated channel!")
        .await?;

    Ok(())
}
