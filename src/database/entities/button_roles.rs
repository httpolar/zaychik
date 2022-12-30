use sqlx::FromRow;

#[derive(Debug, FromRow)]
pub struct ButtonRole {
    pub id: uuid::Uuid,
    pub guild_id: i64,
    pub channel_id: i64,
    pub role_id: i64,
}
