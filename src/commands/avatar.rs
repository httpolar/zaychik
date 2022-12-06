use poise::serenity_prelude as serenity;

use crate::{Context, Error};

/// Shows your or someone else's avatar
#[poise::command(slash_command, prefix_command)]
pub async fn avatar(
    ctx: Context<'_>,
    user: Option<serenity::User>,
    hidden: Option<bool>,
) -> Result<(), Error> {
    let hidden = hidden.unwrap_or_else(|| false);
    let user = user.as_ref().unwrap_or_else(|| ctx.author());

    let avatar_url = user
        .avatar_url()
        .unwrap_or_else(|| user.default_avatar_url());
    let msg = format!("{}'s avatar: {avatar_url}", user.name);

    ctx.send(|reply| reply.ephemeral(hidden).content(msg))
        .await?;

    Ok(())
}
