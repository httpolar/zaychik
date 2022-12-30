use anyhow::Result;
use poise::serenity_prelude::{Http, Reaction};

use crate::database::entities::react_roles::ReactRole;
use crate::traits::as_i64::AsInt64;
use crate::Data;

pub async fn reaction_add(http: &Http, reaction: &Reaction, data: &Data) -> Result<()> {
    let pool = &data.pool;

    let message_id = reaction.message_id.as_i64()?;
    let react_role = sqlx::query_as::<_, ReactRole>(
        r#"SELECT * FROM "public"."react_roles" WHERE "message_id" = $1;"#,
    )
    .bind(message_id)
    .fetch_optional(pool)
    .await?;

    if react_role.is_none() {
        return Ok(());
    }
    let react_role = react_role.unwrap();

    http.add_member_role(
        reaction.guild_id.unwrap().0,
        reaction.user_id.unwrap().0,
        react_role.role_id as u64,
        Some("Reaction Role"),
    )
    .await?;

    Ok(())
}

pub async fn reaction_remove(http: &Http, reaction: &Reaction, data: &Data) -> Result<()> {
    let pool = &data.pool;

    let message_id = reaction.message_id.as_i64()?;
    let react_role = sqlx::query_as::<_, ReactRole>(
        r#"SELECT * FROM "public"."react_roles" WHERE "message_id" = $1;"#,
    )
    .bind(message_id)
    .fetch_optional(pool)
    .await?;

    if react_role.is_none() {
        return Ok(());
    }
    let react_role = react_role.unwrap();

    http.remove_member_role(
        reaction.guild_id.unwrap().0,
        reaction.user_id.unwrap().0,
        react_role.role_id as u64,
        Some("Reaction Role"),
    )
    .await?;

    Ok(())
}
