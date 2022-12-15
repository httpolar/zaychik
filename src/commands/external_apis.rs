use crate::prelude::Context;
use crate::structs::saucenao::SearchResponse;
use anyhow::Result;
use poise::serenity_prelude::Attachment;
use reqwest;

const SAUCENAO_SEARCH: &'static str =
    "https://saucenao.com/search.php?output_type=2&numres=10&dedupe=2";

/// Reverse image search via saucenao
#[poise::command(slash_command)]
pub async fn sauce(
    ctx: Context<'_>,
    image: Attachment,
    #[description = "If you want to see low similarity results"] show_low_similarity: Option<bool>,
    #[description = "If you'd like to hide command output"] hidden: Option<bool>,
) -> Result<()> {
    let show_low_similarity = show_low_similarity.unwrap_or(false);

    match hidden {
        Some(true) => ctx.defer_ephemeral().await?,
        _ => ctx.defer().await?,
    }

    let client = &ctx.data().reqwest;
    let api_key = &ctx.data().keys.saucenao;

    let url = reqwest::Url::parse_with_params(
        SAUCENAO_SEARCH,
        &[("api_key", api_key), ("url", &image.url)],
    )?;

    let data: SearchResponse = client.get(url).send().await?.json().await?;

    let mut response = String::new();
    for (entry, idx) in data.results.iter().zip(0..) {
        let is_credible = entry.is_credible().unwrap_or(false);
        if show_low_similarity == false && is_credible == false {
            continue;
        }

        if entry.data.ext_urls.as_ref().is_none() {
            continue;
        }

        let similarity = format!("(`{:.2}`% similarity)", entry.header.similarity_as_f64()?);

        let url = match entry.data.ext_urls.as_ref().unwrap().get(0) {
            Some(v) => {
                let mut result = String::new();
                if !is_credible || idx != 0 {
                    result.push_str("<");
                }

                result.push_str(v);

                if !is_credible || idx != 0 {
                    result.push_str(">");
                }

                result
            }
            _ => "".to_string(),
        };

        let row = format!("#{} {}: {} ", idx + 1, similarity, url);
        response = format!("{response}{}\n", row);
    }

    ctx.send(|r| {
        r.embed(|e| {
            e.image(&image.url)
                .title("Searching for source of the image")
        })
        .embed(|e| {
            e.title("Search results")
                .description(response)
                .footer(|footer| footer.text("Powered by saucenao.com"))
        })
    })
    .await?;

    Ok(())
}
