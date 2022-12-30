use anyhow::Result;
use poise::serenity_prelude::{MessageId, ChannelId, GuildId, UserId, RoleId, EmojiId};

pub trait AsInt64 {
    fn as_i64(&self) -> Result<i64>;
}

impl AsInt64 for MessageId {
    fn as_i64(&self) -> Result<i64> {
        let n = i64::try_from(self.0)?;
        Ok(n)
    }
}

impl AsInt64 for ChannelId {
    fn as_i64(&self) -> Result<i64> {
        let n = i64::try_from(self.0)?;
        Ok(n)
    }
}

impl AsInt64 for GuildId {
    fn as_i64(&self) -> Result<i64> {
        let n = i64::try_from(self.0)?;
        Ok(n)
    }
}

impl AsInt64 for UserId {
    fn as_i64(&self) -> Result<i64> {
        let n = i64::try_from(self.0)?;
        Ok(n)
    }
}

impl AsInt64 for RoleId {
    fn as_i64(&self) -> Result<i64> {
        let n = i64::try_from(self.0)?;
        Ok(n)
    }
}

impl AsInt64 for EmojiId {
    fn as_i64(&self) -> Result<i64> {
        let n = i64::try_from(self.0)?;
        Ok(n)
    }
}