use crate::{prelude::Context};

pub trait BotEmotes {
    fn emote_success(&self) -> &str;
    fn emote_failure(&self) -> &str;
}

impl<'a> BotEmotes for Context<'a> {
    fn emote_success(&self) -> &str {
        &self.data().bot_emotes.success
    }

    fn emote_failure(&self) -> &str {
        &self.data().bot_emotes.failure
    }
}
