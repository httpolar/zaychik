#![cfg(test)]
use crate::message_link;
use crate::utils::simple_paginator::SimplePaginator;

#[test]
fn message_link_macro() {
    let guild = 1;
    let channel = 2;
    let message = "3";

    let link = message_link!(guild, channel, message);
    let expected = "https://discord.com/channels/1/2/3".to_string();

    assert_eq!(expected, link)
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
