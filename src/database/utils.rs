use anyhow::Result;
use sqlx::{Pool, Postgres};

use super::entities::react_roles::ReactRole;

impl ReactRole {
    pub async fn create(
        pool: &Pool<Postgres>,
        guild_id: &i64,
        channel_id: &i64,
        message_id: &i64,
        role_id: &i64,
        emoji_id: &i64,
    ) -> Result<Self> {
        let row: Self = sqlx::query_as(r#"
        INSERT INTO "public"."react_roles" ("guild_id", "channel_id", "message_id", "role_id", "emoji_id", "enabled") 
        VALUES ($1, $2, $3, $4, $5, $6) 
        RETURNING *;"#)
        .bind(guild_id)
        .bind(channel_id)
        .bind(message_id)
        .bind(role_id)
        .bind(emoji_id)
        .bind(true)
        .fetch_one(pool)
        .await?;

        Ok(row)
    }
}
