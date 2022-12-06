use crate::{Context, Error};

/// Test if the bot is online
#[poise::command(slash_command, prefix_command)]
pub async fn ping(ctx: Context<'_>) -> Result<(), Error> {
    ctx.send(|reply| reply.ephemeral(true).content("Pong!"))
        .await?;

    Ok(())
}
