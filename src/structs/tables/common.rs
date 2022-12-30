use sqlx::FromRow;

#[derive(Debug, FromRow)]
pub struct GuildSettings {
    pub guild_id: i64,
}

#[derive(Debug, FromRow)]
pub struct Exists {
    pub exists: bool,
}
