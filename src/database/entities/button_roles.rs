use anyhow::{Error, Result};
use poise::serenity_prelude::{ChannelId, GuildId, MessageId, RoleId};
use sqlx::FromRow;

#[derive(Debug, FromRow)]
pub struct ButtonRole {
    pub id: uuid::Uuid,
    pub guild_id: i64,
    pub channel_id: i64,
    pub message_id: Option<i64>,
    pub role_id: i64,
}

impl ButtonRole {
    #[allow(dead_code)]
    pub fn guild_id(&self) -> Result<GuildId> {
        let as_u64: u64 = self.guild_id.try_into()?;

        Ok(GuildId(as_u64))
    }

    #[allow(dead_code)]
    pub fn channel_id(&self) -> Result<ChannelId> {
        let as_u64: u64 = self.channel_id.try_into()?;

        Ok(ChannelId(as_u64))
    }

    pub fn message_id(&self) -> Result<MessageId> {
        let as_u64: u64 = self
            .message_id
            .ok_or_else(|| Error::msg("Message ID is None"))?
            .try_into()?;

        Ok(MessageId(as_u64))
    }

    pub fn role_id(&self) -> Result<RoleId> {
        let as_u64: u64 = self.role_id.try_into()?;

        Ok(RoleId(as_u64))
    }
}
