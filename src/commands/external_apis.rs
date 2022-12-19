use crate::prelude::Context;
use crate::structs::saucenao::SearchResponse;
use anyhow::Result;
use poise::serenity_prelude::Attachment;

const SAUCENAO_SEARCH: &str = "https://saucenao.com/search.php?output_type=2&numres=10&dedupe=2";

async fn find_sources(
    client: &reqwest::Client,
    api_key: &String,
    image_url: &String,
) -> Result<SearchResponse> {
    let url = reqwest::Url::parse_with_params(
        SAUCENAO_SEARCH,
        &[("api_key", api_key), ("url", image_url)],
    )?;

    let repsponse = client.get(url).send().await?.json().await?;

    Ok(repsponse)
}

fn generate_result(search_response: &SearchResponse, show_low_similarity: bool) -> Result<String> {
    let mut response = String::new();
    for (entry, idx) in search_response.results.iter().zip(0u8..) {
        let is_credible = entry.is_credible().unwrap_or(false);
        if !show_low_similarity && !is_credible {
            continue;
        }

        if entry.data.ext_urls.is_empty() {
            continue;
        }

        let similarity = format!("(`{:.2}`% similarity)", entry.header.similarity_as_f64()?);
        let url: String = if let Some(s) = entry.data.ext_urls.get(0) {
            s.into()
        } else {
            "".into()
        };

        let row = format!("#{} {}: {} ", idx + 1, similarity, url);
        response = format!("{response}{row}\n");
    }

    Ok(response)
}

#[allow(unused_variables)]
#[poise::command(slash_command, subcommands("file", "url"))]
pub async fn sauce(ctx: Context<'_>, arg: String) -> Result<()> {
    Ok(())
}

/// Reverse image search via saucenao
#[poise::command(slash_command)]
pub async fn file(
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

    let data = find_sources(&ctx.data().reqwest, &ctx.data().keys.saucenao, &image.url).await?;
    let result = generate_result(&data, show_low_similarity)?;

    ctx.send(|r| {
        r.embed(|e| {
            e.image(&image.url)
                .title("Searching for source of the image")
        })
        .embed(|e| e.description(result))
    })
    .await?;

    Ok(())
}

#[poise::command(slash_command)]
pub async fn url(
    ctx: Context<'_>,
    url: String,
    #[description = "If you want to see low similarity results"] show_low_similarity: Option<bool>,
    #[description = "If you'd like to hide command output"] hidden: Option<bool>,
) -> Result<()> {
    let show_low_similarity = show_low_similarity.unwrap_or(false);

    match hidden {
        Some(true) => ctx.defer_ephemeral().await?,
        _ => ctx.defer().await?,
    }

    let data = find_sources(&ctx.data().reqwest, &ctx.data().keys.saucenao, &url).await?;
    let result = generate_result(&data, show_low_similarity)?;

    ctx.send(|r| {
        r.embed(|e| e.image(&url).title("Searching for source of the image"))
            .embed(|e| e.description(result))
    })
    .await?;

    Ok(())
}
