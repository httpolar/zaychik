use std::str::FromStr;

use anyhow::{Error, Result};
use poise::serenity_prelude::{
    Http, InteractionResponseType, MessageComponentInteraction, ParseValue,
};

use crate::{database::entities::button_roles::ButtonRole, traits::as_i64::AsInt64, Data};

pub fn extract_uuid(custom_id: &str) -> Result<uuid::Uuid> {
    let maybe_uuid = custom_id
        .split(':')
        .nth(1)
        .ok_or(Error::msg("Failed to split custom id of interaction"))?;

    let uuid = uuid::Uuid::from_str(maybe_uuid)?;
    Ok(uuid)
}

async fn inform_actor(
    http: &Http,
    interaction: &MessageComponentInteraction,
    msg: impl Into<String>,
) -> Result<()> {
    interaction
        .create_interaction_response(http, |r| {
            r.kind(InteractionResponseType::ChannelMessageWithSource)
                .interaction_response_data(|d| {
                    d.content(msg.into())
                        .ephemeral(true)
                        .allowed_mentions(|am| am.parse(ParseValue::Users))
                })
        })
        .await?;

    Ok(())
}

pub async fn handle_obtain(
    http: &Http,
    interaction: &MessageComponentInteraction,
    uuid: uuid::Uuid,
    data: &Data,
) -> Result<()> {
    let member = interaction
        .member
        .as_ref()
        .ok_or_else(|| Error::msg("Member is not present on the interaction"))?;

    let guild_id = interaction
        .guild_id
        .ok_or_else(|| Error::msg("Guild is missing on the interaction"))?;

    let pool = &data.pool;

    let button_role = sqlx::query_as::<_, ButtonRole>(
        r#"SELECT * FROM "public"."button_roles" as "t" WHERE "t"."guild_id" = $1 AND "t"."id" = $2 LIMIT 1;"#,
    )
    .bind(guild_id.as_i64()?)
    .bind(uuid)
    .fetch_one(pool)
    .await?;

    let role_id = button_role.role_id()?;

    if member.roles.contains(&role_id) {
        inform_actor(http, interaction, "You already have this role!").await?;
        return Ok(());
    }

    http.add_member_role(
        guild_id.0,
        member.user.id.0,
        role_id.0,
        Some("Button role obtained!"),
    )
    .await?;

    let msg = format!("You have successfully acquired the <@&{role_id}> role!");
    inform_actor(http, interaction, msg).await?;

    Ok(())
}

pub async fn handle_remove(
    http: &Http,
    interaction: &MessageComponentInteraction,
    uuid: uuid::Uuid,
    data: &Data,
) -> Result<()> {
    let member = interaction
        .member
        .as_ref()
        .ok_or_else(|| Error::msg("Member is not present on the interaction"))?;

    let guild_id = interaction
        .guild_id
        .ok_or_else(|| Error::msg("Guild is missing on the interaction"))?;

    let pool = &data.pool;

    let button_role = sqlx::query_as::<_, ButtonRole>(
        r#"SELECT * FROM "public"."button_roles" as "t" WHERE "t"."guild_id" = $1 AND "t"."id" = $2 LIMIT 1;"#,
    )
    .bind(guild_id.as_i64()?)
    .bind(uuid)
    .fetch_one(pool)
    .await?;

    let role_id = button_role.role_id()?;

    if !member.roles.contains(&role_id) {
        inform_actor(http, interaction, "You already don't have this role!").await?;
        return Ok(());
    }

    http.remove_member_role(
        guild_id.0,
        member.user.id.0,
        role_id.0,
        Some("Button role removed!"),
    )
    .await?;

    let msg = format!("You have successfully withdrawn the <@&{role_id}> role!");
    inform_actor(http, interaction, msg).await?;

    Ok(())
}
