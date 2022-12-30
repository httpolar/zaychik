#![allow(incomplete_features)]
#![feature(async_fn_in_trait)]

mod commands;
mod config;
mod constans;
mod database;
mod event_handlers;
mod prelude;
mod structs;
mod tests;
mod traits;
mod utils;

use anyhow::Result;
use commands::get_commands;
use config::{ApiKeys, AppConfig};
use event_handlers::main_handler;
use poise::serenity_prelude as serenity;
use sqlx::{Pool, Postgres};
use utils::database::create_pool;

pub struct Data {
    keys: ApiKeys,
    reqwest: reqwest::Client,
    pool: Pool<Postgres>,
}

#[tokio::main]
async fn main() -> Result<()> {
    let config = AppConfig::figment();
    let pool = create_pool(&config.database.url).await?;

    let framework = poise::Framework::builder()
        .options(poise::FrameworkOptions {
            commands: get_commands().await,
            event_handler: |ctx, event, framework, state| {
                Box::pin(main_handler(ctx, event, framework, state))
            },
            ..Default::default()
        })
        .token(config.token)
        .intents(serenity::GatewayIntents::non_privileged())
        .setup(|ctx, _ready, framework| {
            Box::pin(async move {
                poise::builtins::register_globally(ctx, &framework.options().commands).await?;
                Ok(Data {
                    keys: config.apis,
                    reqwest: reqwest::Client::new(),
                    pool,
                })
            })
        });

    framework.run().await.unwrap();

    Ok(())
}
