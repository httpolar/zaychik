use poise::serenity_prelude::{self as serenity, CacheHttp};

use crate::{utils::UserFromUserId, Context, Error};

/// Shows your or someone else's avatar
#[poise::command(slash_command, prefix_command)]
pub async fn avatar(
    ctx: Context<'_>,
    #[description = "Some discord user you want to look up"] user: Option<serenity::UserId>,
    #[description = "If you'd like to hide command output"] hidden: Option<bool>,
) -> Result<(), Error> {
    let hidden = hidden.unwrap_or_else(|| false);
    let user = match user {
        Some(user_id) => serenity::User::from_id(ctx.http(), user_id).await?,
        _ => ctx.author().clone(),
    };

    let avatar_url = user
        .avatar_url()
        .unwrap_or_else(|| user.default_avatar_url());
    let msg = format!("{}'s avatar: {avatar_url}", user.name);

    ctx.send(|reply| reply.ephemeral(hidden).content(msg))
        .await?;

    Ok(())
}
