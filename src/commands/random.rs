use crate::{Context, Error};
use rand::Rng;

/// Generates a random integer
#[poise::command(slash_command, prefix_command)]
pub async fn random(
    ctx: Context<'_>,
    #[description = "Lower bound (inclusive)"] min: Option<i64>,
    #[description = "Upper bound (inclusive)"] max: Option<i64>,
    #[description = "If you'd like to hide command output"] hidden: Option<bool>,
) -> Result<(), Error> {
    let hidden = hidden.unwrap_or(false);

    let min: i64 = min.unwrap_or(1);
    let max: i64 = max.unwrap_or(100);

    if min > max {
        ctx.send(|reply| {
            reply
                .ephemeral(true)
                .content("Minimum number can not be bigger than maximum!")
        })
        .await?;
        return Ok(());
    }

    let the_number: i64 = rand::thread_rng().gen_range(min..=max);
    let response = format!("The number is {the_number}");

    ctx.send(|reply| reply.ephemeral(hidden).content(response))
        .await?;

    Ok(())
}
