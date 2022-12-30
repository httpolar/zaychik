use anyhow::{bail, Result};
use poise::serenity_prelude::{ButtonStyle, CacheHttp, Channel, Role};

use crate::constans::{BUTTON_ROLE_ADD_PREFIX, BUTTON_ROLE_RMV_PREFIX};
use crate::database::entities::button_roles::ButtonRole;
use crate::prelude::Context;
use crate::traits::as_i64::AsInt64;

#[poise::command(
    slash_command,
    rename = "button-role",
    subcommands("new"),
    required_permissions = "MANAGE_ROLES"
)]
pub async fn buttonrole(_ctx: Context<'_>, _arg: String) -> Result<()> {
    Ok(())
}

#[poise::command(slash_command, required_permissions = "MANAGE_ROLES")]
pub async fn new(ctx: Context<'_>, channel: Option<Channel>, role: Role) -> Result<()> {
    ctx.defer_ephemeral().await?;

    let channel_id = channel.map(|c| c.id()).unwrap_or_else(|| ctx.channel_id());

    let pool = &ctx.data().pool;
    let mut tx = pool.begin().await?;

    let button_role = sqlx::query_as::<_, ButtonRole>(
        r#"INSERT INTO "public"."button_roles" ("guild_id", "channel_id", "role_id")
    VALUES ($1, $2, $3)
    RETURNING *;"#,
    )
    .bind(ctx.guild_id().unwrap().as_i64()?)
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
                                .label("Obtain")
                        })
                        .create_button(|btn| {
                            btn.custom_id(format!("{BUTTON_ROLE_RMV_PREFIX}:{}", button_role.id))
                                .style(ButtonStyle::Secondary)
                                .label("Remove")
                        })
                })
            })
            .content(format!("Click buttons to obtain or remove {role}"))
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
