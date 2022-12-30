mod reaction_roles;

use anyhow::{Error, Result};
use poise::serenity_prelude as serenity;

use crate::Data;

pub async fn main_handler(
    ctx: &serenity::Context,
    event: &poise::Event<'_>,
    _framework: poise::FrameworkContext<'_, Data, Error>,
    data: &Data,
) -> Result<()> {
    if let poise::Event::Ready { data_about_bot } = event {
        println!("{} is ready!", data_about_bot.user.name)
    }

    if let poise::Event::ReactionAdd { add_reaction } = event {
        self::reaction_roles::reaction_add(&ctx.http, add_reaction, data).await?;
    }

    if let poise::Event::ReactionRemove { removed_reaction } = event {
        self::reaction_roles::reaction_remove(&ctx.http, removed_reaction, data).await?;
    }

    Ok(())
}
