use anyhow::Result;

use crate::{prelude::Context, database::entities::common::Exists};

#[poise::command(
    slash_command,
    subcommands("init"),
    required_permissions = "MANAGE_GUILD"
)]
pub async fn settings(_ctx: Context<'_>, _arg: String) -> Result<()> {
    Ok(())
}

#[poise::command(slash_command, required_permissions = "MANAGE_GUILD")]
pub async fn init(ctx: Context<'_>) -> Result<()> {
    ctx.defer_ephemeral().await?;

    let guild_id = *ctx.guild_id().unwrap().as_u64() as i64;
    let pool = &ctx.data().pool;

    let row = sqlx::query_as::<_, Exists>(
        r#"SELECT EXISTS(SELECT "guild_id" FROM "guilds" WHERE "guild_id" = $1);"#,
    )
    .bind(guild_id)
    .fetch_one(pool)
    .await?;

    if row.exists {
        ctx.say("This server is already present in the database! Things should run properly now.")
            .await?;

        return Ok(());
    }

    sqlx::query(r#"INSERT INTO "public"."guilds" ("guild_id") VALUES ($1);"#)
        .bind(guild_id)
        .fetch_optional(pool)
        .await?;

    ctx.say("Ok, done! Now your server is now stored in the database! :+1:")
        .await?;

    Ok(())
}
