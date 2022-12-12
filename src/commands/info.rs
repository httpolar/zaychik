use poise::serenity_prelude::{self as serenity, CacheHttp};

use crate::{utils::UserFromUserId, Context, Error};

/// Test if the bot is online
#[poise::command(slash_command, prefix_command)]
pub async fn ping(ctx: Context<'_>) -> Result<(), Error> {
    ctx.send(|reply| reply.ephemeral(true).content("Pong!"))
        .await?;

    Ok(())
}

/// Shows your or someone else's avatar
#[poise::command(slash_command, prefix_command)]
pub async fn avatar(
    ctx: Context<'_>,
    #[description = "Some discord user you want to look up"] user: serenity::UserId,
    #[description = "If you'd like to hide command output"] hidden: Option<bool>,
) -> Result<(), Error> {
    match hidden {
        Some(true) => ctx.defer_ephemeral().await?,
        _ => ctx.defer().await?,
    }

    let user = serenity::User::from_id(ctx.http(), user).await?;
    let avatar_url = user
        .avatar_url()
        .unwrap_or_else(|| user.default_avatar_url());
    let msg = format!("{}'s avatar: {avatar_url}", user.name);

    ctx.say(msg).await?;

    Ok(())
}

/// Shows some base info about Discord users
#[poise::command(slash_command, prefix_command)]
pub async fn userinfo(
    ctx: Context<'_>,
    #[description = "Some discord user you want to look up"] user: serenity::UserId,
    #[description = "If you'd like to hide command output"] hidden: Option<bool>,
) -> Result<(), Error> {
    match hidden {
        Some(true) => ctx.defer_ephemeral().await?,
        _ => ctx.defer().await?,
    }

    let user = serenity::User::from_id(ctx.http(), user).await?;
    let avatar_url = user.face();
    let registered_human_readable = format!("<t:{}:F>", user.created_at().timestamp());

    ctx.send(|reply| {
        reply.embed(|e| {
            e.title(format!("Discord user {}", &user.name));
            e.thumbnail(&avatar_url);
            e.field("Discord Tag", format!("{}", &user.tag()), true);
            e.field("ID", format!("{}", &user.id), true);
            e.field("Avatar", format!("[Avatar URL]({})", &avatar_url), true);
            e.field("Registered At", &registered_human_readable, true);

            if let Some(ref public_flags) = user.public_flags {
                e.field("Public Flags", format!("{}", public_flags.bits()), true);
            }

            if let Some(accent_colour) = user.accent_colour {
                e.field("Accent Colour", format!("#{}", accent_colour.hex()), true);
                e.color(accent_colour);
            }

            e
        })
    })
    .await?;

    Ok(())
}
