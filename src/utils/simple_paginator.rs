#![allow(unused)]
use crate::prelude::Context;
use anyhow::{bail, Result};
use nanoid::nanoid;
use poise::futures_util::StreamExt;
use poise::serenity_prelude::{
    CacheHttp, Http, InteractionResponseType, MessageBuilder, MessageComponentInteraction,
};
use std::{sync::Arc, time::Duration};
use tokio::sync::Mutex;

pub struct SimplePaginator {
    pub pages: Vec<String>,
    timeout: Duration,
    ephemeral: bool,
    id: String,
}

impl SimplePaginator {
    pub fn builder() -> SimplePaginatorBuilder {
        SimplePaginatorBuilder::new()
    }

    async fn failure(
        &self,
        interaction: &MessageComponentInteraction,
        http: &Http,
        why: impl Into<String>,
    ) -> serenity::Result<()> {
        interaction
            .create_interaction_response(http, |res| {
                res.kind(InteractionResponseType::ChannelMessageWithSource)
                    .interaction_response_data(|d| d.ephemeral(true).content(why.into()))
            })
            .await
    }

    fn format_page(&self, page: impl Into<String>, index: &usize) -> String {
        let headline = format!("Page {} / {}", index + 1, self.pages.len());

        MessageBuilder::new()
            .push_bold_line(headline)
            .push_line("")
            .push(page.into())
            .build()
    }

    async fn send_page(
        &self,
        interaction: &MessageComponentInteraction,
        http: &Http,
        page: impl Into<String>,
        index: &usize,
    ) -> Result<()> {
        interaction
            .create_interaction_response(http, |res| {
                res.kind(InteractionResponseType::UpdateMessage)
                    .interaction_response_data(|d| d.content(self.format_page(page, index)))
            })
            .await?;

        Ok(())
    }

    pub async fn start(&self, ctx: &Context<'_>) -> Result<()> {
        let Some(first_page)  = self.pages.get(0) else {
            bail!("Paginator is empty");
        };

        let current_page = Arc::new(Mutex::<usize>::new(0));

        let previous_id = format!("previous:{}", self.id);
        let next_id = format!("next:{}", self.id);

        let reply_handle = ctx
            .send(|reply| {
                reply.ephemeral(self.ephemeral);
                reply.content(self.format_page(first_page, &0));

                reply.components(|component| {
                    component.create_action_row(|act_row| {
                        act_row.create_button(|btn| btn.custom_id(&previous_id).label("previous"));
                        act_row.create_button(|btn| btn.custom_id(&next_id).label("next"))
                    })
                })
            })
            .await?;

        let msg = reply_handle.message().await?;

        let mut collector = msg
            .await_component_interactions(ctx)
            .timeout(self.timeout)
            .build();

        while let Some(interaction) = collector.next().await {
            let custom_id = interaction.data.custom_id.as_str();
            let mut current_page_lock = current_page.lock().await;

            // handle previous page
            if custom_id == previous_id.as_str() {
                match current_page_lock.checked_sub(1) {
                    None => {
                        self.failure(&interaction, ctx.http(), "Already at the first page.")
                            .await?;
                        continue;
                    }

                    Some(new_page_idx) => {
                        let page = &self.pages[new_page_idx];

                        self.send_page(&interaction, ctx.http(), page, &new_page_idx)
                            .await?;

                        *current_page_lock -= 1;
                    }
                };
            };

            // handle next page
            if custom_id == next_id.as_str() {
                let new_page_idx = current_page_lock.saturating_add(1);

                match self.pages.get(new_page_idx) {
                    None => {
                        self.failure(&interaction, ctx.http(), "Already at the last page,")
                            .await?;
                        continue;
                    }

                    Some(page) => {
                        self.send_page(&interaction, ctx.http(), page, &new_page_idx)
                            .await?;

                        *current_page_lock += 1;
                    }
                };
            };
        }

        Ok(())
    }
}

#[derive(Clone)]
pub struct SimplePaginatorBuilder {
    pages: Vec<String>,
    timeout: Duration,
    ephemeral: bool,
}

impl Default for SimplePaginatorBuilder {
    fn default() -> Self {
        Self {
            pages: vec![],
            timeout: Duration::from_secs(60),
            ephemeral: false,
        }
    }
}

impl SimplePaginatorBuilder {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn timeout(mut self, timeout: Duration) -> Self {
        self.timeout = timeout;
        self
    }

    #[allow(dead_code)]
    pub fn ephemeral(mut self, ephemeral: bool) -> Self {
        self.ephemeral = ephemeral;
        self
    }

    pub fn page(mut self, page: impl Into<String>) -> Self {
        self.pages.push(page.into());
        self
    }

    pub fn build(self) -> SimplePaginator {
        SimplePaginator {
            pages: self.pages,
            timeout: self.timeout,
            id: nanoid!(8),
            ephemeral: self.ephemeral
        }
    }
}

#[test]
fn paginator_three_pages() {
    let paginator = SimplePaginator::builder()
        .page("Hello this is page one")
        .page("Hello this is page two")
        .page("Wow whou could've had thought this is a pgae three!")
        .build();

    assert_eq!(paginator.pages.len(), 3)
}

#[test]
fn paginator_empty() {
    let paginator = SimplePaginator::builder().build();

    assert_eq!(paginator.pages.len(), 0)
}

#[test]
fn paginator_from_a_loop() {
    let pages = vec!["Page 1", "Page 2", "Page 3"];
    let mut builder = SimplePaginator::builder();

    for page in pages {
        builder = builder.page(page);
    }

    let paginator = builder.build();

    assert_eq!(paginator.pages.len(), 3)
}
