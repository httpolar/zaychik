mod button_roles;
mod reaction_roles;

use anyhow::{Error, Result};
use poise::serenity_prelude as serenity;

use crate::constans::{BUTTON_ROLE_ADD_PREFIX, BUTTON_ROLE_RMV_PREFIX};
use crate::event_handlers::button_roles::extract_uuid;
use crate::Data;

pub async fn main_handler(
    ctx: &serenity::Context,
    event: &poise::Event<'_>,
    framework: poise::FrameworkContext<'_, Data, Error>,
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

    if let poise::Event::InteractionCreate { interaction } = event {
        if interaction.application_id().as_u64() != framework.bot_id.as_u64() {
            return Ok(());
        }

        if let Some(component_interaction) = interaction.clone().message_component() {
            let custom_id = &component_interaction.data.custom_id;

            if custom_id.starts_with(BUTTON_ROLE_ADD_PREFIX) {
                let uuid = extract_uuid(custom_id)?;

                self::button_roles::handle_obtain(&ctx.http, &component_interaction, uuid, data)
                    .await?;
            }

            if custom_id.starts_with(BUTTON_ROLE_RMV_PREFIX) {
                let uuid = extract_uuid(custom_id)?;

                self::button_roles::handle_remove(&ctx.http, &component_interaction, uuid, data)
                    .await?;
            }
        }
    }

    Ok(())
}
