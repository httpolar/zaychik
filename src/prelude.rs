use crate::Data;
pub use std::result::Result as StdResult;

pub type Context<'a> = poise::Context<'a, Data, anyhow::Error>;

