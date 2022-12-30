use sqlx::FromRow;


#[derive(Debug, FromRow)]
pub struct ReactRole {
    pub id: i64,
    pub guild_id: i64,
    pub channel_id: i64,
    pub message_id: i64,
    pub role_id: i64,
    pub emoji_id: i64,
    pub enabled: bool,
}

#[derive(Debug, FromRow)]
pub struct LogChannel {
    pub id: i64,
    pub guild_id: i64,
    pub channel_id: i64,
    pub mention: bool,
}