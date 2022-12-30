use anyhow::Result;
use sqlx::{postgres::PgPoolOptions, Pool, Postgres};

pub async fn create_pool(protocol: &str) -> Result<Pool<Postgres>> {
    let pool = PgPoolOptions::new()
        .max_connections(10)
        .connect(protocol)
        .await?;

    Ok(pool)
}
