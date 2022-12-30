pub mod database;
pub mod paginators;
pub mod simple_paginator;

use poise::serenity_prelude::{Http, User, UserId};

// This trait might become useless once this gets fixed - https://github.com/serenity-rs/serenity/issues/2302

pub trait UserFromUserId {
    async fn from_id(http: &Http, id: UserId) -> serenity::Result<User>;
}

impl UserFromUserId for User {
    async fn from_id(http: &Http, id: UserId) -> serenity::Result<User> {
        http.get_user(id.0).await
    }
}
