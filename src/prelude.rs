use crate::{config::Emotes, Data};
pub use std::result::Result as StdResult;

pub type Context<'a> = poise::Context<'a, Data, anyhow::Error>;

pub trait BotEmotes {
    fn bot_emotes(&self) -> &Emotes;
}

impl<'a> BotEmotes for Context<'a> {
    fn bot_emotes(&self) -> &Emotes {
        &self.data().bot_emotes
    }
}
