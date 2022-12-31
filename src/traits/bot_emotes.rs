use crate::{config::Emotes, prelude::Context};

pub trait BotEmotes {
    fn bot_emotes(&self) -> &Emotes;
}

impl<'a> BotEmotes for Context<'a> {
    fn bot_emotes(&self) -> &Emotes {
        &self.data().bot_emotes
    }
}
