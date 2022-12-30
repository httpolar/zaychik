#![allow(unused)]
use std::{sync::Arc, time::Duration};

use anyhow::{Error, Result};
use poise::futures_util::StreamExt;
use poise::serenity_prelude::{
    ButtonStyle, CacheHttp, Colour, CreateComponents, CreateEmbed, Http, InteractionResponseType,
    MessageComponentInteraction, ReactionType,
};
use tokio::sync::Mutex;

use crate::prelude::Context;

const FIRST_ID: &str = "first";
const PREV_ID: &str = "previous";
const STOP_ID: &str = "stop";
const NEXT_ID: &str = "next";
const LAST_ID: &str = "last";

pub struct EmbedPaginator {
    pub title: Option<String>,
    pub footer: Option<String>,
    pub colour: Option<Colour>,
    pub pages: Vec<String>,
    pub timeout: Duration,
}

impl EmbedPaginator {
    pub fn builder() -> EmbedPaginatorBuilder {
        EmbedPaginatorBuilder::default()
    }

    fn max_index(&self) -> Result<usize> {
        self.pages
            .len()
            .checked_sub(1)
            .ok_or(Error::msg("couldnt substract 1 from page length"))
    }

    fn format_page(&self, index: usize) -> Result<CreateEmbed> {
        let mut e = CreateEmbed::default();

        if let Some(ref title) = self.title {
            e.title(title);
        }

        if let Some(colour) = self.colour {
            e.colour(colour);
        }

        let current_page = format!("Page {}/{}", index + 1, self.max_index()? + 1);
        match self.footer.as_ref() {
            Some(s) => {
                let footer = format!("{current_page} • {s}");
                e.footer(|f| f.text(footer));
            }

            None => {
                e.footer(|f| f.text(current_page));
            }
        }

        match self.pages.get(index) {
            Some(s) => {
                e.description(s);
            }
            None => {
                e.description("empty string");
            }
        };

        Ok(e)
    }

    async fn update_message(
        &self,
        interaction: &MessageComponentInteraction,
        http: &Http,
        index: usize,
    ) -> Result<()> {
        let embed = self.format_page(index)?;

        interaction
            .create_interaction_response(http, |r| {
                r.kind(InteractionResponseType::UpdateMessage)
                    .interaction_response_data(|d| d.add_embed(embed))
            })
            .await?;

        Ok(())
    }

    async fn ignore(&self, interaction: &MessageComponentInteraction, http: &Http) -> Result<()> {
        interaction
            .create_interaction_response(http, |r| {
                r.kind(InteractionResponseType::DeferredUpdateMessage)
            })
            .await?;

        Ok(())
    }

    async fn stop(&self, interaction: &MessageComponentInteraction, http: &Http) -> Result<()> {
        interaction
            .create_interaction_response(http, |r| {
                r.kind(InteractionResponseType::UpdateMessage)
                    .interaction_response_data(|d| d.set_components(CreateComponents::default()))
            })
            .await?;

        Ok(())
    }

    pub async fn start(&self, ctx: &Context<'_>, hidden: bool) -> Result<()> {
        match hidden {
            true => ctx.defer_ephemeral().await?,
            false => ctx.defer().await?,
        }

        if self.pages.is_empty() {
            ctx.say(":thinking: Hm, there's nothing here...").await?;
            return Ok(());
        }

        let current_index = Arc::new(Mutex::<usize>::new(0));
        let first_page = self.format_page(0)?;

        let reply = ctx
            .send(|r| {
                r.embeds.push(first_page);

                r.components(|c| {
                    c.create_action_row(|act_row| {
                        act_row
                            .create_button(|b| {
                                b.custom_id(FIRST_ID)
                                    .style(ButtonStyle::Secondary)
                                    .emoji(ReactionType::Unicode("⏪".to_owned()))
                            })
                            .create_button(|b| {
                                b.custom_id(PREV_ID)
                                    .style(ButtonStyle::Secondary)
                                    .emoji(ReactionType::Unicode("◀️".to_owned()))
                            })
                            .create_button(|b| {
                                b.custom_id(STOP_ID)
                                    .style(ButtonStyle::Secondary)
                                    .emoji(ReactionType::Unicode("⏹️".to_owned()))
                            })
                            .create_button(|b| {
                                b.custom_id(NEXT_ID)
                                    .style(ButtonStyle::Secondary)
                                    .emoji(ReactionType::Unicode("▶️".to_owned()))
                            })
                            .create_button(|b| {
                                b.custom_id(LAST_ID)
                                    .style(ButtonStyle::Secondary)
                                    .emoji(ReactionType::Unicode("⏩".to_owned()))
                            })
                    })
                })
            })
            .await?;

        let first_message = reply.message().await?;
        let mut collector = first_message
            .await_component_interactions(ctx)
            .author_id(ctx.author().id)
            .message_id(first_message.id)
            .timeout(self.timeout)
            .build();

        while let Some(interaction) = collector.next().await {
            let mut index_lock = current_index.lock().await;
            let current_id = interaction.data.custom_id.as_str();

            if current_id == STOP_ID {
                self.stop(&interaction, ctx.http()).await?;
                break;
            }

            // handle first btn
            if current_id == FIRST_ID {
                let next_index = 0;
                if next_index == *index_lock {
                    self.ignore(&interaction, ctx.http()).await?;
                    continue;
                }

                self.update_message(&interaction, ctx.http(), next_index)
                    .await?;

                *index_lock = next_index;
            }

            // handle previous btn
            if current_id == PREV_ID {
                let next_index = index_lock.checked_sub(1);
                if next_index.is_none() || *index_lock == 0 {
                    self.ignore(&interaction, ctx.http()).await?;
                    continue;
                }

                self.update_message(&interaction, ctx.http(), next_index.unwrap())
                    .await?;

                *index_lock = next_index.unwrap();
            }

            // handle next btn
            if current_id == NEXT_ID {
                let next_index = index_lock.wrapping_add(1);
                if next_index > self.max_index()? || *index_lock == self.max_index()? {
                    self.ignore(&interaction, ctx.http()).await?;
                    continue;
                }

                self.update_message(&interaction, ctx.http(), next_index)
                    .await?;

                *index_lock = next_index;
            }

            // handle last btn
            if current_id == LAST_ID {
                let next_index = self.max_index()?;
                if next_index == *index_lock {
                    self.ignore(&interaction, ctx.http()).await?;
                    continue;
                }

                self.update_message(&interaction, ctx.http(), next_index)
                    .await?;

                *index_lock = next_index;
            }
        }

        Ok(())
    }
}

#[derive(Default)]
pub struct EmbedPaginatorBuilder {
    title: Option<String>,
    footer: Option<String>,
    colour: Option<Colour>,
    pages: Vec<String>,
    timeout: Duration,
}

#[allow(dead_code)]
impl EmbedPaginatorBuilder {
    pub fn title(mut self, s: impl Into<String>) -> Self {
        self.title = Some(s.into());
        self
    }

    pub fn footer(mut self, s: impl Into<String>) -> Self {
        self.footer = Some(s.into());
        self
    }

    pub fn colour(mut self, c: impl Into<Colour>) -> Self {
        self.colour = Some(c.into());
        self
    }

    pub fn page(mut self, p: impl Into<String>) -> Self {
        self.pages.push(p.into());
        self
    }

    pub fn pages(mut self, v: Vec<String>) -> Self {
        self.pages = v;
        self
    }

    pub fn timeout(mut self, n: Duration) -> Self {
        self.timeout = n;
        self
    }

    pub fn build(self) -> EmbedPaginator {
        EmbedPaginator {
            title: self.title,
            footer: self.footer,
            colour: self.colour,
            pages: self.pages,
            timeout: self.timeout,
        }
    }
}
